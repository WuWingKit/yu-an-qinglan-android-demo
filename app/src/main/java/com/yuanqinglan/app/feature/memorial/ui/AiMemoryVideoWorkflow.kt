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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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

internal object AiMemoryVideoRules {
    const val FEATURED_MEMORIAL_ID = "hm-002"
    const val VIDEO_DURATION_MILLIS = 16_817
    val phases = listOf("分析面部特征", "构建自然动作", "稳定画面细节", "合成追忆影像")

    fun isAvailable(memorialId: String): Boolean = memorialId == FEATURED_MEMORIAL_ID
}

private enum class MotionStyle(val label: String, val description: String) {
    GENTLE_GAZE("温和注视", "轻柔眨眼与自然呼吸"),
    SOFT_SMILE("自然微笑", "保留神态并增加微笑变化"),
}

private data class AiMemoryVideoState(
    val generating: Boolean = false,
    val phase: String = "",
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
    private val _state = MutableStateFlow(AiMemoryVideoState())
    val state: StateFlow<AiMemoryVideoState> = _state.asStateFlow()
    private var generationJob: Job? = null
    private val savedResultUris = mutableListOf<String>()

    init {
        viewModelScope.launch { clearPrivateVideoFiles(SESSION_DIRECTORY) }
    }

    fun startGeneration() {
        if (_state.value.generating || !AiMemoryVideoRules.isAvailable(memorialId)) return
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            _state.value.resultUri?.let { oldResult ->
                runCatching { AppContainer.fileStorage.delete(Uri.parse(oldResult)) }
            }
            _state.value = AiMemoryVideoState(generating = true)
            val phases = AiMemoryVideoRules.phases
            phases.forEachIndexed { index, phase ->
                _state.value = _state.value.copy(
                    phase = phase,
                    progress = index.toFloat() / phases.size,
                )
                delay(850L)
            }
            runCatching {
                val bytes = loadBundledVideo()
                AppContainer.fileStorage.save(bytes, SESSION_DIRECTORY, "memory-video.mp4")
            }.onSuccess { result ->
                _state.value = _state.value.copy(
                    generating = false,
                    phase = "",
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
                fileName = "memory-video.mp4",
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
        appContext.resources.openRawResource(R.raw.ai_memory_mother_generated).use { it.readBytes() }
    }

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
    val available = AiMemoryVideoRules.isAvailable(memorialId)
    var motionStyle by rememberSaveable { mutableStateOf(MotionStyle.GENTLE_GAZE) }
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

        if (!available) {
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
                Image(
                    painter = painterResource(R.drawable.ai_memory_mother_source),
                    contentDescription = "用于生成追忆影像的母亲正面照片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                )
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
                            "1 张清晰正面照 · 已确认使用授权",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }

        MemorialSectionTitle(text = "动态方式")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MotionStyle.entries.forEach { style ->
                FilterChip(
                    selected = motionStyle == style,
                    onClick = { motionStyle = style },
                    label = { Text(style.label) },
                )
            }
        }
        Text(
            text = motionStyle.description + " · 约 17 秒 · 方形画幅",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )

        if (state.generating) {
            GenerationProgress(phase = state.phase, progress = state.progress)
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

        if (state.resultReady) {
            MemorialSectionTitle(text = "生成结果")
            AiGeneratedVideoPlayer(videoUri = Uri.parse(requireNotNull(state.resultUri)))
            NoticeBanner(
                text = "AI 合成影像 · 由授权照片生成，仅用于私人追忆，并非真实拍摄记录。",
                tone = NoticeTone.COMPLIANCE,
            )
            if (state.savedUri == null) {
                PrimaryButton(text = "保存影像到本机", onClick = viewModel::saveResult)
            }
            SecondaryButton(text = "重新生成", onClick = viewModel::startGeneration)
            SecondaryButton(text = "永久销毁生成影像", onClick = { confirmDestroy = true })
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
private fun GenerationProgress(phase: String, progress: Float) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                Text(
                    text = phase,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "正在保持人物神态与画面稳定，请勿离开当前页面。",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun AiGeneratedVideoPlayer(
    videoUri: Uri,
    modifier: Modifier = Modifier,
) {
    var videoView by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var started by rememberSaveable { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var muted by rememberSaveable { mutableStateOf(false) }
    var position by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(AiMemoryVideoRules.VIDEO_DURATION_MILLIS) }

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
                    .aspectRatio(1f)
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
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                )

                if (!started) {
                    Image(
                        painter = painterResource(R.drawable.ai_memory_mother_source),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
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
