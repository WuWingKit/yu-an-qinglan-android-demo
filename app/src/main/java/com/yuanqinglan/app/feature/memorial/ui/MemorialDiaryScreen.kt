/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.ui

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.ConfirmDangerDialog
import com.yuanqinglan.app.core.ui.EmptyState
import com.yuanqinglan.app.core.ui.ErrorRetry
import com.yuanqinglan.app.core.ui.FormTextField
import com.yuanqinglan.app.core.ui.LoadingState
import com.yuanqinglan.app.core.ui.NoticeBanner
import com.yuanqinglan.app.core.ui.NoticeTone
import com.yuanqinglan.app.core.ui.PrimaryButton
import com.yuanqinglan.app.core.ui.SecondaryButton
import com.yuanqinglan.app.data.local.AppContainer
import com.yuanqinglan.app.feature.memorial.data.MemorialRepository
import com.yuanqinglan.app.feature.memorial.data.MemorialServiceLocator
import com.yuanqinglan.app.feature.memorial.model.MediaKind
import com.yuanqinglan.app.feature.memorial.model.MediaRef
import com.yuanqinglan.app.feature.memorial.model.MemorialDiaryEntry
import com.yuanqinglan.app.feature.memorial.model.MemorialIds
import com.yuanqinglan.app.feature.memorial.model.MemorialLike
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_DIARY_TITLE_LENGTH = 40
private const val MAX_DIARY_BODY_LENGTH = 2000
private const val MAX_DIARY_IMAGES = 2

private fun diaryTitleError(value: String): String? = when {
    value.isBlank() -> "请填写日记标题"
    value.length > MAX_DIARY_TITLE_LENGTH -> "标题不能超过 $MAX_DIARY_TITLE_LENGTH 个字"
    else -> null
}

private fun diaryBodyError(value: String): String? =
    if (value.length > MAX_DIARY_BODY_LENGTH) "正文不能超过 $MAX_DIARY_BODY_LENGTH 个字" else null

/** 尽力而为地删除私有目录附件（越界路径由 FileStorage 拒绝，不抛出）。 */
private suspend fun deleteMediaRefFile(ref: MediaRef) {
    if (ref.kind == MediaKind.DRAWABLE) return
    runCatching {
        val uri = Uri.parse(ref.value)
        if (uri.scheme == "file") {
            AppContainer.fileStorage.delete(uri)
        }
    }
}

private fun audioExtensionOf(name: String?): String {
    val raw = name?.substringAfterLast('.', "")?.lowercase() ?: ""
    return if (raw.isNotEmpty() && raw.all { it.isLetterOrDigit() }) raw else "m4a"
}

/** 思念日记 ViewModel：按纪念空间 ID 观察对应轨空间。 */
class MemorialDiaryViewModel(
    private val repository: MemorialRepository,
    val memorialId: String,
) : ViewModel() {

    private val _state = MutableStateFlow<DemoState<MemorialLike>>(DemoState.Loading)
    val state: StateFlow<DemoState<MemorialLike>> = _state.asStateFlow()

    private var collectJob: kotlinx.coroutines.Job? = null

    init {
        startCollect()
    }

    private fun startCollect() {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            repository.observeSpace(memorialId).collect { _state.value = it }
        }
    }

    fun refresh() {
        startCollect()
    }

    override fun onCleared() {
        collectJob?.cancel()
        super.onCleared()
    }
}

/**
 * 思念日记：列表（倒序）+ 新建/编辑对话框 + 二次确认删除。
 * 路由：memorial-diary/{memorialId}
 */
@Composable
fun MemorialDiaryScreen(
    memorialId: String,
    navController: NavHostController,
) {
    val context = LocalContext.current
    val repository = remember(context) { MemorialServiceLocator.repository(context) }
    val viewModel: MemorialDiaryViewModel = viewModel(
        factory = remember(repository, memorialId) {
            MemorialViewModelFactory { MemorialDiaryViewModel(repository, memorialId) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val current = state) {
        DemoState.Loading -> AppScaffold(title = "思念日记", onBack = { navController.popBackStack() }) {
            LoadingState()
        }
        is DemoState.Error -> AppScaffold(title = "思念日记", onBack = { navController.popBackStack() }) {
            ErrorRetry(message = current.message, onRetry = { viewModel.refresh() })
        }
        DemoState.Empty -> AppScaffold(title = "思念日记", onBack = { navController.popBackStack() }) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
            ) {
                EmptyState(
                    title = "还没有日记条目",
                    description = "没有找到可展示的纪念空间，可能已被删除。",
                )
            }
        }
        is DemoState.Success -> DiaryListContent(
            repository = repository,
            memorialId = memorialId,
            space = current.value,
            onBack = { navController.popBackStack() },
        )
    }
}

@Composable
private fun DiaryListContent(
    repository: MemorialRepository,
    memorialId: String,
    space: MemorialLike,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var creating by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<MemorialDiaryEntry?>(null) }
    var actionNotice by remember { mutableStateOf<String?>(null) }
    val actionScope = rememberCoroutineScope()

    val entries = space.sortedDiaryDesc()
    val editingEntry = editingId?.let { space.diaryById(it) }
    val editorVisible = creating || editingEntry != null

    AppScaffold(
        title = "${space.name} · 思念日记",
        onBack = onBack,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            actionNotice?.let {
                NoticeBanner(text = it, tone = NoticeTone.WARNING)
                Spacer(Modifier.height(10.dp))
            }
            if (entries.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                ) {
                    EmptyState(
                        title = "还没有日记条目",
                        description = "把想说的话写下来，留给家人，也留给思念的人。",
                        actionLabel = "写第一篇日记",
                        onAction = { creating = true },
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        PrimaryButton(text = "写日记", onClick = { creating = true })
                    }
                    items(entries, key = { it.id }) { entry ->
                        DiaryEntryCard(
                            entry = entry,
                            onClick = { editingId = entry.id },
                        )
                    }
                }
            }
        }
    }

    if (editorVisible) {
        DiaryEntryEditorDialog(
            existing = editingEntry,
            onDismiss = {
                creating = false
                editingId = null
            },
            onRequestDelete = { entry ->
                creating = false
                editingId = null
                pendingDelete = entry
            },
            onSaveEntry = { entry, isNew ->
                if (isNew) repository.addDiaryEntry(memorialId, entry)
                else repository.updateDiaryEntry(memorialId, entry)
            },
        )
    }

    pendingDelete?.let { entry ->
        ConfirmDangerDialog(
            title = "删除日记条目",
            message = "确定删除「${entry.title.ifBlank { "未命名" }}」吗？图片与音频附件会一并移除，且无法恢复。",
            confirmLabel = "删除",
            onConfirm = {
                val target = entry
                pendingDelete = null
                actionNotice = null
                actionScope.launch {
                    target.images.forEach { deleteMediaRefFile(it) }
                    target.audio?.let { deleteMediaRefFile(it) }
                    val ok = repository.removeDiaryEntry(memorialId, target.id)
                    if (!ok) actionNotice = "删除失败，请稍后重试。"
                }
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun DiaryEntryCard(
    entry: MemorialDiaryEntry,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        color = SurfaceCard,
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
            Text(
                text = entry.title.ifBlank { "未命名日记" },
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatDateTimeText(entry.createdAtMillis),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (entry.body.isNotBlank()) {
                Text(
                    text = entry.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            if (entry.hasAttachment()) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (entry.images.isNotEmpty()) {
                        DiaryBadge(text = "图片 ${entry.images.size}")
                    }
                    if (entry.audio != null) {
                        DiaryBadge(text = "音频")
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaryBadge(text: String) {
    Box(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.primaryContainer,
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
    }
}

/**
 * 新建/编辑日记对话框：标题 + 正文 + 最多两张图片 + 单个音频（文件选择或录音）。
 * 附件文件先落私有目录，删除/替换时尽力清理对应文件。
 */
@Composable
private fun DiaryEntryEditorDialog(
    existing: MemorialDiaryEntry?,
    onDismiss: () -> Unit,
    onRequestDelete: (MemorialDiaryEntry) -> Unit,
    onSaveEntry: suspend (MemorialDiaryEntry, Boolean) -> Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember(existing?.id) { mutableStateOf(existing?.title ?: "") }
    var body by remember(existing?.id) { mutableStateOf(existing?.body ?: "") }
    var images by remember(existing?.id) {
        mutableStateOf(existing?.images ?: emptyList())
    }
    var audio by remember(existing?.id) { mutableStateOf(existing?.audio) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var pickError by remember { mutableStateOf<String?>(null) }

    val originalImages = existing?.images.orEmpty()
    val originalAudio = existing?.audio

    val titleError = diaryTitleError(title)
    val bodyError = diaryBodyError(body)
    val canSave = !saving && titleError == null && bodyError == null

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null || saving) return@rememberLauncherForActivityResult
        if (images.size >= MAX_DIARY_IMAGES) return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = readUriBytes(context, uri)
            if (bytes == null || bytes.isEmpty()) {
                pickError = "读取图片失败，请重新选择。"
                return@launch
            }
            pickError = null
            val saved = AppContainer.fileStorage.saveImage(bytes)
            val ref = MediaRef(
                id = MemorialIds.next("dp"),
                kind = MediaKind.IMAGE_FILE,
                value = saved.toString(),
                name = uri.lastPathSegment ?: "图片",
                sizeBytes = bytes.size.toLong(),
            )
            if (images.size >= MAX_DIARY_IMAGES) {
                deleteMediaRefFile(ref)
            } else {
                images = images + ref
            }
        }
    }

    val audioFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null || saving) return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = readUriBytes(context, uri)
            if (bytes == null || bytes.isEmpty()) {
                pickError = "读取音频失败，请重新选择。"
                return@launch
            }
            pickError = null
            val saved = AppContainer.fileStorage.saveAudio(
                bytes,
                audioExtensionOf(uri.lastPathSegment),
            )
            audio?.let { deleteMediaRefFile(it) }
            audio = MediaRef(
                id = MemorialIds.next("da"),
                kind = MediaKind.AUDIO_FILE,
                value = saved.toString(),
                name = uri.lastPathSegment ?: "音频",
                sizeBytes = bytes.size.toLong(),
            )
        }
    }

    // ---------------- 录音 ----------------
    val recorder = remember(existing?.id) { AudioRecorderController(context) }
    DisposableEffect(recorder) {
        onDispose { recorder.cancel() }
    }
    var recording by remember { mutableStateOf(false) }
    var recordNotice by remember { mutableStateOf<String?>(null) }
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            recordNotice = null
            if (recorder.start() != null) {
                recording = true
            } else {
                recordNotice = "无法开始录音，可改用从文件选择音频。"
            }
        } else {
            recordNotice = "无法录音：未获得麦克风权限，可改用从文件选择音频"
        }
    }

    fun startRecording() {
        recordNotice = null
        recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    fun stopRecording() {
        scope.launch {
            recording = false
            val file = recorder.stop()
            if (file == null) {
                recordNotice = "录音失败，请重试或改用从文件选择音频。"
                return@launch
            }
            val bytes = withContext(Dispatchers.IO) {
                runCatching { file.readBytes() }.getOrDefault(ByteArray(0))
            }
            runCatching { file.delete() }
            if (bytes.isEmpty()) {
                recordNotice = "录音内容为空，请重新录制。"
                return@launch
            }
            val saved = AppContainer.fileStorage.saveAudio(bytes, "m4a")
            audio?.let { deleteMediaRefFile(it) }
            audio = MediaRef(
                id = MemorialIds.next("da"),
                kind = MediaKind.AUDIO_FILE,
                value = saved.toString(),
                name = "录音",
                sizeBytes = bytes.size.toLong(),
            )
            recordNotice = null
        }
    }

    fun cancelEdit() {
        if (saving) return
        scope.launch {
            images.filter { ref -> originalImages.none { it.id == ref.id } }
                .forEach { deleteMediaRefFile(it) }
            audio?.let { current ->
                if (originalAudio?.id != current.id) deleteMediaRefFile(current)
            }
            onDismiss()
        }
    }

    fun save() {
        val trimmedTitle = title.trim()
        if (titleError != null || bodyError != null || saving) return
        saving = true
        saveError = null
        scope.launch {
            val now = System.currentTimeMillis()
            val entry = if (existing == null) {
                MemorialDiaryEntry(
                    id = MemorialIds.next("d"),
                    title = trimmedTitle,
                    body = body.trim(),
                    createdAtMillis = now,
                    updatedAtMillis = now,
                    images = images,
                    audio = audio,
                )
            } else {
                existing.copy(
                    title = trimmedTitle,
                    body = body.trim(),
                    updatedAtMillis = now,
                    images = images,
                    audio = audio,
                )
            }
            val ok = onSaveEntry(entry, existing == null)
            saving = false
            if (ok) {
                onDismiss()
            } else {
                saveError = "保存失败，请稍后重试。"
            }
        }
    }

    Dialog(onDismissRequest = { cancelEdit() }) {
        Surface(
            shape = RoundedCornerShape(AppDimensions.CardRadius),
            color = SurfaceCard,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .padding(16.dp),
            ) {
                Text(
                    text = if (existing == null) "写日记" else "编辑日记",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .verticalScroll(rememberScrollState())
                        .padding(end = 2.dp),
                ) {
                    FormTextField(
                        label = "标题",
                        value = title,
                        onValueChange = {
                            if (!saving) {
                                title = it
                                saveError = null
                            }
                        },
                        isError = titleError != null && title.isNotBlank(),
                        supportingText = titleError ?: "${title.length}/$MAX_DIARY_TITLE_LENGTH",
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = body,
                        onValueChange = {
                            if (!saving) {
                                body = it
                                saveError = null
                            }
                        },
                        label = { Text("正文") },
                        supportingText = {
                            Text(
                                text = bodyError ?: "可选，${body.length}/$MAX_DIARY_BODY_LENGTH",
                                color = if (bodyError != null) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        isError = bodyError != null,
                        minLines = 5,
                        maxLines = 9,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppDimensions.CompactRadius),
                    )
                    Spacer(Modifier.height(14.dp))

                    if (pickError != null || recordNotice != null) {
                        NoticeBanner(
                            text = pickError ?: recordNotice ?: "",
                            tone = NoticeTone.WARNING,
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    Text(
                        text = "图片附件（最多 2 张）",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(6.dp))
                    if (images.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            images.forEach { ref ->
                                RemovableImageThumb(
                                    ref = ref,
                                    onRemove = {
                                        scope.launch {
                                            images = images.filterNot { it.id == ref.id }
                                            deleteMediaRefFile(ref)
                                        }
                                    },
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    if (images.size < MAX_DIARY_IMAGES) {
                        SecondaryButton(
                            text = if (images.isEmpty()) "从相册选择图片" else "再选一张图片",
                            onClick = {
                                imagePicker.launch("image/*")
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "音频附件（可选，一个）",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(6.dp))
                    val currentAudio = audio
                    when {
                        recording -> {
                            PrimaryButton(
                                text = "录音中… 点击停止并保存",
                                onClick = { stopRecording() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        currentAudio != null -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AudioPlayRow(
                                    uri = currentAudio.value,
                                    label = currentAudio.name.ifBlank { "录音" },
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0x22000000), CircleShape)
                                        .clickable {
                                            scope.launch {
                                                val removed = currentAudio
                                                audio = null
                                                deleteMediaRefFile(removed)
                                            }
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "移除音频",
                                        tint = Color(0xCC000000),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SecondaryButton(
                                    text = "从文件更换",
                                    onClick = { audioFilePicker.launch("audio/*") },
                                    modifier = Modifier.weight(1f),
                                )
                                SecondaryButton(
                                    text = "重新录音",
                                    onClick = { startRecording() },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        else -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SecondaryButton(
                                    text = "从文件选择音频",
                                    onClick = { audioFilePicker.launch("audio/*") },
                                    modifier = Modifier.weight(1f),
                                )
                                SecondaryButton(
                                    text = "录音",
                                    onClick = { startRecording() },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }

                if (saveError != null) {
                    Spacer(Modifier.height(8.dp))
                    NoticeBanner(text = saveError ?: "", tone = NoticeTone.WARNING)
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (existing != null) {
                        TextButton(onClick = { onRequestDelete(existing) }) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.weight(1f))
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    TextButton(onClick = { cancelEdit() }) {
                        Text("取消")
                    }
                    Spacer(Modifier.width(6.dp))
                    Button(
                        onClick = { save() },
                        enabled = canSave,
                    ) {
                        Text(if (saving) "保存中…" else "保存")
                    }
                }
            }
        }
    }
}

@Composable
private fun RemovableImageThumb(
    ref: MediaRef,
    onRemove: () -> Unit,
) {
    Box(modifier = Modifier.size(84.dp)) {
        MediaThumb(
            ref = ref,
            contentDescription = "日记图片附件",
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(AppDimensions.CompactRadius)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(24.dp)
                .background(Color(0x88000000), CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "移除图片",
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
