/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.yuanqinglan.app.R
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.QingLanGreenSoft
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.designsystem.Warning
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.ConfirmDangerDialog
import com.yuanqinglan.app.core.ui.NoticeBanner
import com.yuanqinglan.app.core.ui.NoticeTone
import com.yuanqinglan.app.core.ui.PrimaryButton
import com.yuanqinglan.app.core.ui.SecondaryButton
import com.yuanqinglan.app.data.local.AppContainer
import com.yuanqinglan.app.feature.memorial.data.AiFlowGate
import com.yuanqinglan.app.feature.memorial.data.MemorialRepository
import com.yuanqinglan.app.feature.memorial.data.MemorialServiceLocator
import com.yuanqinglan.app.feature.memorial.model.MediaKind
import com.yuanqinglan.app.feature.memorial.model.MediaRef
import com.yuanqinglan.app.feature.memorial.model.MemorialIds
import com.yuanqinglan.app.navigation.AppRoute
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** AI 素材生成阶段文案（本地顺序流程）。 */
private val GENERATION_PHASES = listOf("整理素材", "降噪修复", "增强细节", "完成")

/**
 * AI 素材工作台 ViewModel：素材列表、生成进度、结果与销毁全部为本地状态。
 * 素材/结果文件落在应用私有目录；销毁会清空列表并删除对应私有文件。
 */
class AiUploadViewModel(
    private val repository: MemorialRepository,
    private val appContext: Context,
    val memorialId: String,
) : ViewModel() {

    private val _spaceName = MutableStateFlow<String?>(null)
    val spaceName: StateFlow<String?> = _spaceName.asStateFlow()

    private val _consented = MutableStateFlow(AiFlowGate.consented.value)
    val consented: StateFlow<Boolean> = _consented.asStateFlow()

    private val _materials = MutableStateFlow<List<MediaRef>>(emptyList())
    val materials: StateFlow<List<MediaRef>> = _materials.asStateFlow()

    private val _generating = MutableStateFlow(false)
    val generating: StateFlow<Boolean> = _generating.asStateFlow()

    private val _phaseText = MutableStateFlow("")
    val phaseText: StateFlow<String> = _phaseText.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _resultReady = MutableStateFlow(false)
    val resultReady: StateFlow<Boolean> = _resultReady.asStateFlow()

    private val _savedCount = MutableStateFlow(0)
    val savedCount: StateFlow<Int> = _savedCount.asStateFlow()

    private val _destroyed = MutableStateFlow(false)
    val destroyed: StateFlow<Boolean> = _destroyed.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    /** 已保存结果文件的私有 Uri（供一键销毁）。 */
    private val savedResultUris = mutableListOf<String>()

    private var generationJob: Job? = null

    init {
        viewModelScope.launch {
            AiFlowGate.consented.collect { _consented.value = it }
        }
        viewModelScope.launch {
            runCatching { repository.space(memorialId) }
                .getOrNull()
                ?.let { _spaceName.value = it.name }
        }
    }

    fun clearNotice() {
        _notice.value = null
    }

    /** 素材加入本地列表（bytes 已由调用方写入私有目录）。 */
    fun addMaterial(ref: MediaRef) {
        _materials.update { it + ref }
        _destroyed.value = false
        _notice.value = null
    }

    /** 移除单个素材并删除其私有文件。 */
    fun removeMaterial(refId: String) {
        viewModelScope.launch {
            val target = _materials.value.firstOrNull { it.id == refId } ?: return@launch
            _materials.update { list -> list.filterNot { it.id == refId } }
            runCatching { AppContainer.fileStorage.delete(Uri.parse(target.value)) }
        }
    }

    /** 开始修复生成：分阶段进度动画后产出固定预置结果（可重复）。 */
    fun startGeneration() {
        if (_generating.value) return
        _resultReady.value = false
        _notice.value = null
        generationJob = viewModelScope.launch {
            _generating.value = true
            val total = GENERATION_PHASES.size
            GENERATION_PHASES.forEachIndexed { index, phase ->
                _phaseText.value = phase
                _progress.value = index.toFloat() / total.toFloat()
                delay(800L)
            }
            _phaseText.value = ""
            _progress.value = 1f
            _generating.value = false
            _resultReady.value = true
        }
    }

    /**
     * 保存当前结果到本机私有目录：把预置结果按轻微暖色调烘焙成 PNG 存入
     * ai_result 目录（每次保存生成新副本，可重复）。
     */
    fun saveResultToLocal() {
        if (!_resultReady.value || _generating.value) return
        viewModelScope.launch {
            val resultBitmap = withContext(Dispatchers.IO) {
                val source = BitmapFactory.decodeResource(
                    appContext.resources,
                    R.drawable.ai_restore_sample_faded,
                ) ?: return@withContext null
                val baked = source.copy(Bitmap.Config.ARGB_8888, true)
                Canvas(baked).drawColor(WARM_OVERLAY_ARGB, PorterDuff.Mode.SRC_OVER)
                source.recycle()
                baked
            } ?: run {
                _notice.value = "结果文件生成失败，请重试"
                return@launch
            }
            val bytes = withContext(Dispatchers.IO) {
                ByteArrayOutputStream().use { stream ->
                    resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    stream.toByteArray()
                }
            }
            val saved = AppContainer.fileStorage.save(bytes, "ai_result", "restored.png")
            savedResultUris += saved.toString()
            _savedCount.update { it + 1 }
            _notice.value = "修复结果已保存到本机。"
        }
    }

    /** 一键永久销毁：删除全部素材与已保存结果的私有文件并清空本地状态。 */
    fun destroyAll() {
        viewModelScope.launch {
            _materials.value.forEach { ref ->
                runCatching { AppContainer.fileStorage.delete(Uri.parse(ref.value)) }
            }
            savedResultUris.forEach { uri ->
                runCatching { AppContainer.fileStorage.delete(Uri.parse(uri)) }
            }
            savedResultUris.clear()
            generationJob?.cancel()
            _materials.value = emptyList()
            _savedCount.value = 0
            _resultReady.value = false
            _generating.value = false
            _phaseText.value = ""
            _progress.value = 0f
            _notice.value = null
            _destroyed.value = true
        }
    }

    /** 销毁成功态后重新开始使用工作台。 */
    fun restartAfterDestroy() {
        _destroyed.value = false
        _notice.value = null
    }

    override fun onCleared() {
        generationJob?.cancel()
        super.onCleared()
    }

    private companion object {
        /** 轻微暖色调叠加（与结果预览渲染保持一致）。 */
        const val WARM_OVERLAY_ARGB = 0x33D69A5B
    }
}

@Composable
fun AiUploadScreen(
    memorialId: String,
    navController: NavHostController,
) {
    val context = LocalContext.current
    val repository = remember(context) { MemorialServiceLocator.repository(context) }
    val appContext = remember(context) { context.applicationContext }
    val viewModel: AiUploadViewModel = viewModel(
        factory = remember(repository, appContext, memorialId) {
            MemorialViewModelFactory { AiUploadViewModel(repository, appContext, memorialId) }
        },
    )
    AiUploadContent(viewModel = viewModel, navController = navController)
}

@Composable
private fun AiUploadContent(
    viewModel: AiUploadViewModel,
    navController: NavHostController,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val spaceName by viewModel.spaceName.collectAsStateWithLifecycle()
    val consented by viewModel.consented.collectAsStateWithLifecycle()
    val materials by viewModel.materials.collectAsStateWithLifecycle()
    val generating by viewModel.generating.collectAsStateWithLifecycle()
    val phaseText by viewModel.phaseText.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val resultReady by viewModel.resultReady.collectAsStateWithLifecycle()
    val savedCount by viewModel.savedCount.collectAsStateWithLifecycle()
    val destroyed by viewModel.destroyed.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()

    var confirmDestroy by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var creationMode by rememberSaveable(viewModel.memorialId) {
        mutableStateOf(
            if (AiMemoryVideoRules.isAvailable(viewModel.memorialId)) {
                AiCreationMode.MEMORY_VIDEO
            } else {
                AiCreationMode.PHOTO_RESTORE
            },
        )
    }

    fun importUri(uri: Uri?, imageOnly: Boolean) {
        if (uri == null) return
        scope.launch {
            importing = true
            val bytes = readUriBytes(context, uri)
            if (bytes == null || bytes.isEmpty()) {
                importing = false
                return@launch
            }
            val name = uri.lastPathSegment ?: "素材"
            val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull()
            val kind = kindOf(name, mime, imageOnly)
            val saved = AppContainer.fileStorage.save(bytes, "ai_upload", name)
            viewModel.addMaterial(
                MediaRef(
                    id = MemorialIds.next("ai"),
                    kind = kind,
                    value = saved.toString(),
                    name = name,
                    sizeBytes = bytes.size.toLong(),
                ),
            )
            importing = false
        }
    }

    val albumPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        importUri(it, imageOnly = true)
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        importUri(it, imageOnly = false)
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        importUri(it, imageOnly = false)
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        importUri(it, imageOnly = false)
    }

    AppScaffold(
        title = spaceName ?: "AI 追忆",
        onBack = { navController.popBackStack() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 目标空间与本地说明
            Text(
                text = "目标空间：${spaceName ?: "加载中…"}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Text(
                text = "素材与生成结果仅保存在本机，可重复生成，也可随时一键永久销毁。",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )

            notice?.let { message ->
                NoticeBanner(text = message, tone = NoticeTone.INFO)
            }

            // ---- 伦理授权门 ----
            if (!consented) {
                NoticeBanner(
                    text = "请先阅读并同意 AI 追忆伦理与授权说明",
                    tone = NoticeTone.WARNING,
                )
                SecondaryButton(
                    text = "去阅读",
                    onClick = {
                        AiFlowGate.prepare(viewModel.memorialId)
                        navController.navigate(AppRoute.AI_ETHICS.route)
                    },
                )
            } else {
                NoticeBanner(
                    text = "本会话已确认伦理与授权；素材与生成全程在本机进行。",
                    tone = NoticeTone.INFO,
                )
            }

            AiCreationModeSelector(
                selected = creationMode,
                onSelected = { creationMode = it },
            )

            if (creationMode == AiCreationMode.MEMORY_VIDEO) {
                AiMemoryVideoWorkflow(
                    memorialId = viewModel.memorialId,
                    consented = consented,
                )
                return@Column
            }

            if (destroyed) {
                DestroyedSuccessPanel(onRestart = viewModel::restartAfterDestroy)
                return@Column
            }

            // ---- 素材来源 ----
            MemorialSectionTitle(text = "添加素材")
            Text(
                text = "选择要修复的私人影像素材（照片 / 文件 / 录音 / 视频），选定后仅保存在本机。",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SourceCard(
                    icon = Icons.Outlined.PhotoLibrary,
                    label = "相册",
                    enabled = !importing,
                    modifier = Modifier.weight(1f),
                ) { albumPicker.launch("image/*") }
                SourceCard(
                    icon = Icons.Outlined.FolderOpen,
                    label = "文件",
                    enabled = !importing,
                    modifier = Modifier.weight(1f),
                ) { filePicker.launch("*/*") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SourceCard(
                    icon = Icons.Outlined.Mic,
                    label = "录音",
                    enabled = !importing,
                    modifier = Modifier.weight(1f),
                ) { audioPicker.launch("audio/*") }
                SourceCard(
                    icon = Icons.Outlined.Videocam,
                    label = "视频",
                    enabled = !importing,
                    modifier = Modifier.weight(1f),
                ) { videoPicker.launch("video/*") }
            }

            // ---- 素材列表 ----
            if (materials.isEmpty()) {
                MemorialEmptyHint(
                    title = "还没有添加素材",
                    description = "从上方选择照片、文件、录音或视频作为修复素材",
                )
            } else {
                materials.forEach { ref ->
                    MaterialRow(
                        ref = ref,
                        onRemove = { viewModel.removeMaterial(ref.id) },
                    )
                }
            }

            // ---- 修复前预览（固定本地样本，仅用于流程展示） ----
            val firstImage = materials.firstOrNull {
                it.kind == MediaKind.IMAGE_FILE || it.kind == MediaKind.DRAWABLE
            }
            if (firstImage != null) {
                MemorialSectionTitle(text = "修复前预览")
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppDimensions.CardRadius),
                    color = SurfaceCard,
                ) {
                    Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
                        Image(
                            painter = painterResource(memorialDrawable(AiRestoreSampleToken)),
                            contentDescription = "修复前素材预览",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                        )
                        Text(
                            text = "修复前预览：${firstImage.name}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            // ---- 生成区 ----
            MemorialSectionTitle(text = "生成修复")
            if (generating) {
                Column {
                    Text(
                        text = phaseText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "处理仅在本地进行，请稍候…",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            } else {
                PrimaryButton(
                    text = "开始修复生成",
                    enabled = consented && !importing,
                    onClick = viewModel::startGeneration,
                )
                if (!consented) {
                    Text(
                        text = "请先阅读并同意伦理与授权说明后，才可使用生成功能。",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
            }

            // ---- 结果视图 ----
            if (resultReady) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppDimensions.CardRadius),
                    color = SurfaceCard,
                ) {
                    Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(memorialDrawable(AiRestoreSampleToken)),
                                contentDescription = "修复结果预览",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                            )
                            // 轻微暖色调叠加，模拟修复完成后的整体观感。
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .background(Color(0x33D69A5B)),
                            )
                        }
                        Text(
                            text = "修复结果预览",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        if (savedCount > 0) {
                            Text(
                                text = "本会话已保存 $savedCount 份结果到本机",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        PrimaryButton(
                            text = "保存结果到本机",
                            onClick = viewModel::saveResultToLocal,
                        )
                        Spacer(Modifier.height(8.dp))
                        SecondaryButton(
                            text = "重新生成",
                            onClick = viewModel::startGeneration,
                        )
                        Spacer(Modifier.height(4.dp))
                        DangerActionButton(
                            text = "永久销毁（高优先级）",
                            onClick = { confirmDestroy = true },
                        )
                    }
                }
            } else if (materials.isNotEmpty() && !generating) {
                Text(
                    text = "点击「开始修复生成」处理已选素材（结果同样仅保存在本机）。",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
        }
    }

    if (confirmDestroy) {
        ConfirmDangerDialog(
            title = "永久销毁全部 AI 素材与结果",
            message = "将删除全部素材与已保存的生成结果对应的本机文件，销毁后不可恢复。确定继续吗？",
            confirmLabel = "永久销毁",
            onConfirm = {
                confirmDestroy = false
                viewModel.destroyAll()
            },
            onDismiss = { confirmDestroy = false },
        )
    }
}

/** 素材来源选择卡（图标 + 名称）。 */
@Composable
private fun SourceCard(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        color = SurfaceCard,
        onClick = onClick,
        enabled = enabled,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )
        }
    }
}

/** 单个素材行：类型 + 名称 + 大小，可移除。 */
@Composable
private fun MaterialRow(
    ref: MediaRef,
    onRemove: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CompactRadius),
        color = QingLanGreenSoft,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ref.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${formatBytesText(ref.sizeBytes)} · ${typeLabelOf(ref.name, null, false)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(AppDimensions.MinimumTouchTarget),
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "移除该素材",
                    tint = TextSecondary,
                )
            }
        }
    }
}

/** 危险操作文字按钮（警示色，高优先级入口）。 */
@Composable
private fun DangerActionButton(
    text: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = Warning),
    ) {
        Icon(
            imageVector = Icons.Outlined.DeleteOutline,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(text)
    }
}

/** 永久销毁后的成功态面板。 */
@Composable
private fun DestroyedSuccessPanel(onRestart: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        color = SurfaceCard,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 26.dp, horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = null,
                tint = Warning,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "已永久销毁",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Text(
                text = "本机上的 AI 追忆素材与生成结果已全部清除，不可恢复。",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(14.dp))
            SecondaryButton(text = "继续添加素材", onClick = onRestart)
        }
    }
}

/** 文件大小可读文本。 */
private fun formatBytesText(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}

/** 素材展示类型（按 MIME，必要时按扩展名兜底）。 */
private fun typeLabelOf(name: String, mime: String?, imageOnly: Boolean): String {
    val m = mime.orEmpty().lowercase()
    when {
        m.startsWith("image/") || imageOnly -> return "图片"
        m.startsWith("audio/") -> return "音频"
        m.startsWith("video/") -> return "视频"
    }
    return when (name.lowercase().substringAfterLast('.', "")) {
        "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif" -> "图片"
        "mp3", "m4a", "aac", "wav", "amr", "ogg", "flac" -> "音频"
        "mp4", "mov", "3gp", "mkv", "webm", "avi" -> "视频"
        else -> "文件"
    }
}

/** 素材 MediaKind 归类（音频/视频与展示类型一致，其余按图片文件处理）。 */
private fun kindOf(name: String, mime: String?, imageOnly: Boolean): MediaKind {
    val label = typeLabelOf(name, mime, imageOnly)
    return when (label) {
        "音频" -> MediaKind.AUDIO_FILE
        "视频" -> MediaKind.VIDEO_FILE
        else -> MediaKind.IMAGE_FILE
    }
}
