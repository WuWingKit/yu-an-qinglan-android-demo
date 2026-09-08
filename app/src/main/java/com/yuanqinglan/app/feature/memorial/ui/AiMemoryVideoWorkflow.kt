/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.ui

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yuanqinglan.app.R
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.QingLanGreenSoft
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.ui.ConfirmDangerDialog
import com.yuanqinglan.app.core.ui.NoticeBanner
import com.yuanqinglan.app.core.ui.NoticeTone
import com.yuanqinglan.app.core.ui.PrimaryButton
import com.yuanqinglan.app.core.ui.SecondaryButton
import com.yuanqinglan.app.data.local.AppContainer
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AiCreationMode(val label: String) {
    PHOTO_RESTORE("影像修复"),
    MEMORY_VIDEO("照片成片"),
}

internal enum class AiMemorySubjectKind { HUMAN, PET }

internal data class AiMemoryGenerationStage(
    val title: String,
    val detail: String,
    val targetProgress: Float,
    val durationMillis: Long,
)

internal data class AiMemoryVideoPreset(
    val memorialId: String,
    val subjectKind: AiMemorySubjectKind,
    val subjectLabel: String,
    val sourceDescription: String,
    val fileStem: String,
    @param:DrawableRes val sourceDrawable: Int,
    @param:RawRes val videoRaw: Int,
    val videoDurationMillis: Int,
    val aspectRatio: Float,
    val frameLabel: String,
)

internal object AiMemoryVideoRules {
    private val humanStages = listOf(
        AiMemoryGenerationStage("校验照片清晰度", "检查面部完整度、曝光与可用区域", 0.12f, 520L),
        AiMemoryGenerationStage("定位面部与视线", "锁定五官位置并保持原始神态", 0.31f, 680L),
        AiMemoryGenerationStage("构建呼吸与微表情", "生成克制的眨眼、呼吸与表情变化", 0.58f, 920L),
        AiMemoryGenerationStage("修复边缘与光影", "稳定发丝、服饰轮廓和背景光线", 0.81f, 760L),
        AiMemoryGenerationStage("稳定时序并合成", "检查帧间一致性并导出追忆影像", 0.96f, 640L),
    )
    private val petStages = listOf(
        AiMemoryGenerationStage("校验照片清晰度", "检查宠物主体、毛发边缘与可用区域", 0.12f, 520L),
        AiMemoryGenerationStage("识别宠物轮廓", "定位眼睛、口鼻和身体姿态", 0.31f, 680L),
        AiMemoryGenerationStage("构建自然动作", "生成轻柔眨眼、呼吸与神态变化", 0.58f, 920L),
        AiMemoryGenerationStage("稳定毛发与背景", "减少边缘闪动并保持环境连续", 0.81f, 760L),
        AiMemoryGenerationStage("稳定时序并合成", "检查帧间一致性并导出追忆影像", 0.96f, 640L),
    )

    val presets: Map<String, AiMemoryVideoPreset> = listOf(
        AiMemoryVideoPreset(
            memorialId = "hm-001",
            subjectKind = AiMemorySubjectKind.HUMAN,
            subjectLabel = "外公",
            sourceDescription = "用于生成追忆影像的外公照片",
            fileStem = "grandfather",
            sourceDrawable = R.drawable.memorial_grandfather_portrait,
            videoRaw = R.raw.ai_memory_grandfather_generated,
            videoDurationMillis = 28_000,
            aspectRatio = 816f / 1104f,
            frameLabel = "竖向画幅",
        ),
        AiMemoryVideoPreset(
            memorialId = "hm-002",
            subjectKind = AiMemorySubjectKind.HUMAN,
            subjectLabel = "母亲",
            sourceDescription = "用于生成追忆影像的母亲正面照片",
            fileStem = "mother",
            sourceDrawable = R.drawable.ai_memory_mother_source,
            videoRaw = R.raw.ai_memory_mother_generated,
            videoDurationMillis = 16_817,
            aspectRatio = 1f,
            frameLabel = "方形画幅",
        ),
        AiMemoryVideoPreset(
            memorialId = "hm-003",
            subjectKind = AiMemorySubjectKind.HUMAN,
            subjectLabel = "姑姑",
            sourceDescription = "用于生成追忆影像的中年女性正面照片",
            fileStem = "woman",
            sourceDrawable = R.drawable.memorial_woman_portrait,
            videoRaw = R.raw.ai_memory_woman_generated,
            videoDurationMillis = 11_560,
            aspectRatio = 1f,
            frameLabel = "方形画幅",
        ),
        AiMemoryVideoPreset(
            memorialId = "pm-002",
            subjectKind = AiMemorySubjectKind.PET,
            subjectLabel = "狗狗",
            sourceDescription = "用于生成追忆影像的狗狗正面照片",
            fileStem = "dog",
            sourceDrawable = R.drawable.memorial_dog_portrait,
            videoRaw = R.raw.ai_memory_dog_generated,
            videoDurationMillis = 15_069,
            aspectRatio = 1f,
            frameLabel = "方形画幅",
        ),
    ).associateBy(AiMemoryVideoPreset::memorialId)

    val availableMemorialIds: Set<String> = presets.keys

    fun preset(memorialId: String): AiMemoryVideoPreset? = presets[memorialId]

    fun isAvailable(memorialId: String): Boolean = memorialId in availableMemorialIds

    fun stagesFor(subjectKind: AiMemorySubjectKind): List<AiMemoryGenerationStage> =
        if (subjectKind == AiMemorySubjectKind.PET) petStages else humanStages
}

private enum class MotionStyle(
    val subjectKind: AiMemorySubjectKind,
    val label: String,
    val description: String,
) {
    GENTLE_GAZE(AiMemorySubjectKind.HUMAN, "温和注视", "轻柔眨眼与自然呼吸"),
    SOFT_SMILE(AiMemorySubjectKind.HUMAN, "自然微笑", "保留神态并增加微笑变化"),
    QUIET_COMPANION(AiMemorySubjectKind.PET, "安静陪伴", "轻柔呼吸与自然眨眼"),
    PLAYFUL_LOOK(AiMemorySubjectKind.PET, "轻快回应", "保留姿态并增加抬头回应"),
}

private data class AiMemoryVideoState(
    val generating: Boolean = false,
    val phase: String = "",
    val phaseDetail: String = "",
    val stageIndex: Int = 0,
    val totalStages: Int = 0,
    val progress: Float = 0f,
    val resultReady: Boolean = false,
    val resultUri: String? = null,
    val savedUri: String? = null,
    val notice: String? = null,
)

private class AiMemoryVideoViewModel(
    private val appContext: Context,
    private val memorialId: String,
) : ViewModel() {
    private val preset = AiMemoryVideoRules.preset(memorialId)
    private val _state = MutableStateFlow(AiMemoryVideoState())
    val state: StateFlow<AiMemoryVideoState> = _state.asStateFlow()
    private var generationJob: Job? = null
    private val savedResultUris = mutableListOf<String>()

    init {
        viewModelScope.launch { clearPrivateVideoFiles(SESSION_DIRECTORY) }
    }

    fun startGeneration() {
        val selectedPreset = preset ?: return
        if (_state.value.generating) return
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            _state.value.resultUri?.let { oldResult ->
                runCatching { AppContainer.fileStorage.delete(Uri.parse(oldResult)) }
            }
            val stages = AiMemoryVideoRules.stagesFor(selectedPreset.subjectKind)
            _state.value = AiMemoryVideoState(generating = true, totalStages = stages.size)
            var stageStartProgress = 0f
            stages.forEachIndexed { index, stage ->
                val steps = (stage.durationMillis / PROGRESS_TICK_MILLIS).toInt().coerceAtLeast(1)
                repeat(steps) { stepIndex ->
                    val fraction = (stepIndex + 1f) / steps
                    val eased = 1f - (1f - fraction) * (1f - fraction) * (1f - fraction)
                    _state.value = _state.value.copy(
                        phase = stage.title,
                        phaseDetail = stage.detail,
                        stageIndex = index + 1,
                        progress = stageStartProgress + (stage.targetProgress - stageStartProgress) * eased,
                    )
                    delay(PROGRESS_TICK_MILLIS)
                }
                stageStartProgress = stage.targetProgress
            }
            runCatching {
                val bytes = loadBundledVideo()
                AppContainer.fileStorage.save(bytes, SESSION_DIRECTORY, outputFileName())
            }.onSuccess { result ->
                _state.value = _state.value.copy(
                    generating = false,
                    phase = "",
                    phaseDetail = "",
                    progress = 1f,
                    resultReady = true,
                    resultUri = result.toString(),
                )
            }.onFailure {
                _state.value = AiMemoryVideoState(notice = "影像生成失败，请重试。")
            }
        }
    }

    fun saveResult() {
        if (!_state.value.resultReady || _state.value.savedUri != null) return
        viewModelScope.launch {
            val bytes = loadBundledVideo()
            val saved = AppContainer.fileStorage.save(
                bytes = bytes,
                directoryName = RESULT_DIRECTORY,
                fileName = outputFileName(),
            )
            savedResultUris += saved.toString()
            _state.value = _state.value.copy(
                savedUri = saved.toString(),
                notice = "追忆影像已保存到本机私有空间。",
            )
        }
    }

    fun destroyResult() {
        viewModelScope.launch {
            _state.value.resultUri?.let { uri ->
                runCatching { AppContainer.fileStorage.delete(Uri.parse(uri)) }
            }
            _state.value.savedUri?.let { uri ->
                runCatching { AppContainer.fileStorage.delete(Uri.parse(uri)) }
            }
            savedResultUris.forEach { uri ->
                runCatching { AppContainer.fileStorage.delete(Uri.parse(uri)) }
            }
            clearPrivateVideoFiles(SESSION_DIRECTORY)
            clearPrivateVideoFiles(RESULT_DIRECTORY)
            savedResultUris.clear()
            generationJob?.cancel()
            _state.value = AiMemoryVideoState(notice = "本次生成影像与保存副本已永久销毁。")
        }
    }

    private suspend fun loadBundledVideo(): ByteArray = withContext(Dispatchers.IO) {
        appContext.resources.openRawResource(requireNotNull(preset).videoRaw).use { it.readBytes() }
    }

    private fun outputFileName(): String = "${requireNotNull(preset).fileStem}_memory-video.mp4"

    private suspend fun clearPrivateVideoFiles(directoryName: String) = withContext(Dispatchers.IO) {
        val directory = File(appContext.filesDir, "yuanqinglan/$directoryName")
        directory.listFiles()
            ?.filter { it.isFile && it.name.endsWith("_memory-video.mp4") }
            ?.forEach { it.delete() }
    }

    override fun onCleared() {
        generationJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val SESSION_DIRECTORY = "ai_session"
        const val RESULT_DIRECTORY = "ai_result"
        const val PROGRESS_TICK_MILLIS = 80L
    }
}

@Composable
fun AiCreationModeSelector(
    selected: AiCreationMode,
    onSelected: (AiCreationMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MemorialSectionTitle(text = "创作方式")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AiCreationMode.entries.forEach { mode ->
                FilterChip(
                    selected = selected == mode,
                    onClick = { onSelected(mode) },
                    label = { Text(mode.label) },
                    leadingIcon = if (mode == AiCreationMode.MEMORY_VIDEO) {
                        { Icon(Icons.Outlined.MovieCreation, contentDescription = null) }
                    } else {
                        { Icon(Icons.Outlined.AutoAwesome, contentDescription = null) }
                    },
                )
            }
        }
    }
}

@Composable
fun AiMemoryVideoWorkflow(
    memorialId: String,
    consented: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val viewModel: AiMemoryVideoViewModel = viewModel(
        key = "ai-memory-video-$memorialId",
        factory = remember(appContext, memorialId) {
            MemorialViewModelFactory { AiMemoryVideoViewModel(appContext, memorialId) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val preset = AiMemoryVideoRules.preset(memorialId)
    val motionStyles = remember(preset?.subjectKind) {
        MotionStyle.entries.filter { it.subjectKind == preset?.subjectKind }
    }
    var motionStyle by rememberSaveable(memorialId) {
        mutableStateOf(motionStyles.firstOrNull() ?: MotionStyle.GENTLE_GAZE)
    }
    var confirmDestroy by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "用一张获得授权的照片，生成一段自然、克制的动态追忆影像。",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )

        if (preset == null) {
            NoticeBanner(
                text = "当前纪念空间尚未添加可用于成片的授权正面照片。请先在纪念相册中补充清晰照片。",
                tone = NoticeTone.WARNING,
            )
            return@Column
        }

        MemorialSectionTitle(text = "照片素材")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimensions.CardRadius),
            color = SurfaceCard,
        ) {
            Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(preset.aspectRatio)
                        .clip(RoundedCornerShape(AppDimensions.CompactRadius)),
                ) {
                    Image(
                        painter = painterResource(preset.sourceDrawable),
                        contentDescription = preset.sourceDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                    if (state.generating) {
                        AiProcessingSweepOverlay(Modifier.matchParentSize())
                    }
                }
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    Column {
                        Text("素材检查通过", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(
                            "1 张清晰${preset.subjectLabel}照片 · 已确认使用授权",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }

        MemorialSectionTitle(text = "动态方式")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            motionStyles.forEach { style ->
                FilterChip(
                    selected = motionStyle == style,
                    onClick = { motionStyle = style },
                    label = { Text(style.label) },
                )
            }
        }
        Text(
            text = motionStyle.description +
                " · 约 ${((preset.videoDurationMillis + 500) / 1_000)} 秒 · ${preset.frameLabel}",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )

        if (state.generating) {
            GenerationProgress(
                phase = state.phase,
                phaseDetail = state.phaseDetail,
                stageIndex = state.stageIndex,
                totalStages = state.totalStages,
                progress = state.progress,
            )
        } else if (!state.resultReady) {
            PrimaryButton(
                text = "生成追忆影像",
                enabled = consented,
                onClick = viewModel::startGeneration,
            )
            if (!consented) {
                Text(
                    text = "请先完成上方伦理与授权确认。",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
        }

        state.notice?.let { NoticeBanner(text = it, tone = NoticeTone.INFO) }

        state.resultUri?.let { resultUri ->
            AnimatedVisibility(
                visible = state.resultReady,
                enter = fadeIn(tween(420)),
                exit = fadeOut(tween(180)),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MemorialSectionTitle(text = "生成结果")
                    AiGeneratedVideoPlayer(
                        videoUri = Uri.parse(resultUri),
                        posterDrawable = preset.sourceDrawable,
                        posterDescription = preset.sourceDescription,
                        expectedDurationMillis = preset.videoDurationMillis,
                        aspectRatio = preset.aspectRatio,
                    )
                    NoticeBanner(
                        text = "AI 合成影像 · 由已获授权的${preset.subjectLabel}照片生成，仅用于私人追忆，并非真实拍摄记录。",
                        tone = NoticeTone.COMPLIANCE,
                    )
                    if (state.savedUri == null) {
                        PrimaryButton(text = "保存影像到本机", onClick = viewModel::saveResult)
                    }
                    SecondaryButton(text = "重新生成", onClick = viewModel::startGeneration)
                    SecondaryButton(text = "永久销毁生成影像", onClick = { confirmDestroy = true })
                }
            }
        }
    }

    if (confirmDestroy) {
        ConfirmDangerDialog(
            title = "永久销毁生成影像",
            message = "生成影像及其本机保存副本将被删除，销毁后不可恢复。",
            confirmLabel = "永久销毁",
            onConfirm = {
                confirmDestroy = false
                viewModel.destroyResult()
            },
            onDismiss = { confirmDestroy = false },
        )
    }
}

@Composable
private fun AiProcessingSweepOverlay(modifier: Modifier = Modifier) {
    var previewHeightPx by remember { mutableIntStateOf(0) }
    val transition = rememberInfiniteTransition(label = "ai-processing")
    val scanFraction by transition.animateFloat(
        initialValue = -0.18f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_650, easing = LinearEasing),
        ),
        label = "scan-position",
    )
    val accent = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .onSizeChanged { previewHeightPx = it.height }
            .background(accent.copy(alpha = 0.06f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .graphicsLayer { translationY = scanFraction * previewHeightPx }
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            accent.copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0.42f),
                            accent.copy(alpha = 0.18f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            shape = RoundedCornerShape(6.dp),
            color = Color.Black.copy(alpha = 0.58f),
        ) {
            Text(
                text = "AI 分析中",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun GenerationProgress(
    phase: String,
    phaseDetail: String,
    stageIndex: Int,
    totalStages: Int,
    progress: Float,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 180),
        label = "generation-progress",
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "AI 生成进度 ${(animatedProgress * 100).toInt()}%，$phase"
            },
        shape = RoundedCornerShape(AppDimensions.CompactRadius),
        color = QingLanGreenSoft,
    ) {
        Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "正在生成追忆影像",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                    Text(
                        text = phase.ifBlank { "准备素材" },
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                    )
                }
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth(),
            )
            if (totalStages > 0) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    repeat(totalStages) { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .background(
                                    color = if (index < stageIndex) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    },
                                    shape = CircleShape,
                                ),
                        )
                    }
                }
            }
            Text(
                text = phaseDetail.ifBlank { "正在准备本机处理环境" },
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "第 ${stageIndex.coerceAtLeast(1)} / ${totalStages.coerceAtLeast(1)} 步 · 本机合成处理中",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(alpha = 0.82f),
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun AiGeneratedVideoPlayer(
    videoUri: Uri,
    @DrawableRes posterDrawable: Int,
    posterDescription: String,
    expectedDurationMillis: Int,
    aspectRatio: Float,
    modifier: Modifier = Modifier,
) {
    var videoView by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var started by rememberSaveable { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var muted by rememberSaveable { mutableStateOf(false) }
    var position by remember { mutableIntStateOf(0) }
    var duration by remember(videoUri) { mutableIntStateOf(expectedDurationMillis) }

    LaunchedEffect(playing) {
        while (playing) {
            val player = videoView
            if (player != null) {
                position = player.currentPosition.coerceAtLeast(0)
                duration = player.duration.takeIf { it > 0 } ?: duration
            }
            delay(250L)
        }
    }
    LaunchedEffect(playing, controlsVisible) {
        if (playing && controlsVisible) {
            delay(1_200L)
            controlsVisible = false
        }
    }
    LaunchedEffect(muted, mediaPlayer) {
        val volume = if (muted) 0f else 1f
        mediaPlayer?.setVolume(volume, volume)
    }
    DisposableEffect(Unit) {
        onDispose {
            videoView?.stopPlayback()
            mediaPlayer = null
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        color = Color(0xFF111411),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .semantics { contentDescription = "AI 合成追忆影像播放器" },
                contentAlignment = Alignment.Center,
            ) {
                AndroidView(
                    factory = { viewContext ->
                        VideoView(viewContext).apply {
                            setVideoURI(videoUri)
                            setOnPreparedListener { player ->
                                mediaPlayer = player
                                player.isLooping = false
                                val volume = if (muted) 0f else 1f
                                player.setVolume(volume, volume)
                                duration = player.duration.takeIf { it > 0 } ?: duration
                            }
                            setOnCompletionListener {
                                playing = false
                                controlsVisible = true
                                position = duration
                            }
                            videoView = this
                        }
                    },
                    modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio),
                )

                if (!started) {
                    Image(
                        painter = painterResource(posterDrawable),
                        contentDescription = posterDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.18f)),
                    )
                }

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "显示播放控制",
                        ) { controlsVisible = true },
                )

                if (controlsVisible || !playing) {
                    Surface(
                        modifier = Modifier
                            .size(64.dp)
                            .clickable(role = Role.Button) {
                            val player = videoView ?: return@clickable
                            if (playing) {
                                player.pause()
                                playing = false
                            } else {
                                if (position >= duration - 300) {
                                    player.seekTo(0)
                                    position = 0
                                }
                                started = true
                                player.start()
                                playing = true
                            }
                            },
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.62f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                contentDescription = if (playing) "暂停影像" else "播放影像",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp),
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${formatVideoTime(position)} / ${formatVideoTime(duration)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.82f),
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { muted = !muted }) {
                    Icon(
                        imageVector = if (muted) {
                            Icons.AutoMirrored.Outlined.VolumeOff
                        } else {
                            Icons.AutoMirrored.Outlined.VolumeUp
                        },
                        contentDescription = if (muted) "开启声音" else "关闭声音",
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

private fun formatVideoTime(millis: Int): String {
    val totalSeconds = (millis.coerceAtLeast(0) / 1000)
    return String.format(Locale.CHINA, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}
