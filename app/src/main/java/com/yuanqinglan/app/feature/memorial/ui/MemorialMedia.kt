/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yuanqinglan.app.R
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.QingLanGreenSoft
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.feature.memorial.model.MediaKind
import com.yuanqinglan.app.feature.memorial.model.MediaRef
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * drawable 资源名 → R.drawable 映射。未知 token 回退人类肖像（UI 兜底，
 * 数据侧仍各自持有正确 token，绝不因此串轨）。
 */
@DrawableRes
fun memorialDrawable(token: String): Int = when (token) {
    HumanPortraitToken -> R.drawable.memorial_human_portrait
    MotherPortraitToken -> R.drawable.memorial_mother_portrait
    PetPortraitToken -> R.drawable.memorial_pet_portrait
    GalleryHumanTeaToken -> R.drawable.memorial_gallery_family_tea
    GalleryPetParkToken -> R.drawable.memorial_gallery_pet_park
    AiRestoreSampleToken -> R.drawable.ai_restore_sample_faded
    else -> R.drawable.memorial_human_portrait
}

const val HumanPortraitToken = "memorial_human_portrait"
const val MotherPortraitToken = "memorial_mother_portrait"
const val PetPortraitToken = "memorial_pet_portrait"
const val GalleryHumanTeaToken = "memorial_gallery_family_tea"
const val GalleryPetParkToken = "memorial_gallery_pet_park"
const val AiRestoreSampleToken = "ai_restore_sample_faded"

/** 头像/肖像圆形示意语义（不带“演示”字样）。 */
const val HUMAN_PORTRAIT_DESCRIPTION = "纪念人物示意肖像"
const val PET_PORTRAIT_DESCRIPTION = "纪念宠物示意肖像"

/** 私有目录 file:// Uri 解码（带采样防大图 OOM），IO 线程执行。 */
suspend fun decodeFileBitmap(uri: String): ImageBitmap? = withContext(Dispatchers.IO) {
    val parsed = Uri.parse(uri)
    if (parsed.scheme != "file") return@withContext null
    val path = parsed.path ?: return@withContext null
    val file = File(path)
    if (!file.isFile) return@withContext null
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(path, options)
    val sample = calculateSampleSize(options.outWidth, options.outHeight)
    val decode = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val bitmap = BitmapFactory.decodeFile(path, decode) ?: return@withContext null
    bitmap.asImageBitmap()
}

private fun calculateSampleSize(width: Int, height: Int): Int {
    var sample = 1
    var maxDimension = maxOf(width, height)
    while (maxDimension > 1600) {
        sample *= 2
        maxDimension /= 2
    }
    return sample
}

/** 图片/音视频附件统一可视化：图片真渲染、音视频用图标卡。 */
@Composable
fun MediaThumb(
    ref: MediaRef,
    modifier: Modifier = Modifier,
    contentDescription: String?,
) {
    when (ref.kind) {
        MediaKind.DRAWABLE, MediaKind.IMAGE_FILE -> {
            if (ref.isDrawable) {
                Image(
                    painter = painterResource(memorialDrawable(ref.value)),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = modifier,
                )
            } else {
                val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = ref.value) {
                    value = decodeFileBitmap(ref.value)
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!,
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Crop,
                        modifier = modifier,
                    )
                } else {
                    MediaTypePlaceholder(
                        text = "图片不可用",
                        modifier = modifier,
                    )
                }
            }
        }
        MediaKind.AUDIO_FILE -> MediaTypePlaceholder(text = "音频", modifier = modifier)
        MediaKind.VIDEO_FILE -> MediaTypePlaceholder(text = "视频", modifier = modifier)
    }
}

@Composable
private fun MediaTypePlaceholder(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(QingLanGreenSoft),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.VideoFile,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(text = text, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
    }
}

/** 私有音频播放/暂停行（播放本地音频，不依赖外部播放器）。 */
@Composable
fun AudioPlayRow(
    uri: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var playing by remember { mutableStateOf(false) }
    val player = remember(uri) {
        runCatching {
            MediaPlayer().apply {
                setDataSource(context, Uri.parse(uri))
                setOnCompletionListener { playing = false }
                prepare()
            }
        }.getOrNull()
    }
    DisposableEffect(player) {
        onDispose {
            runCatching { if (player?.isPlaying == true) player?.stop() }
            player?.release()
        }
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(AppDimensions.CompactRadius),
        color = SurfaceCard,
        onClick = {
            if (player == null) return@Surface
            if (playing) {
                runCatching { player.pause() }
                playing = false
            } else {
                runCatching { player.start() }
                playing = true
            }
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (playing) Icons.Outlined.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (player == null) "音频不可用" else if (playing) "暂停" else "播放",
                tint = TextPrimary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (player == null) "音频不可用（文件缺失）" else label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                maxLines = 1,
            )
        }
    }
}

/** 简单音频录音控制器（先申请麦克风权限，失败返回 null 并给调用方反馈）。 */
class AudioRecorderController(context: Context) {

    private val appContext = context.applicationContext
    private var recorder: MediaRecorder? = null
    private var output: File? = null

    val isRecording: Boolean
        get() = recorder != null

    /** 开始录音；不可用（权限缺失/硬件异常/未声明权限）返回 null。 */
    fun start(): File? {
        if (recorder != null) return null
        return try {
            val file = File(appContext.cacheDir, "rec_${System.currentTimeMillis()}.m4a")
            val instance = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
            }
            instance.prepare()
            instance.start()
            recorder = instance
            output = file
            file
        } catch (e: Exception) {
            runCatching { recorder?.release() }
            recorder = null
            null
        }
    }

    /** 停止并返回音频文件；未在录音返回 null。 */
    fun stop(): File? {
        val instance = recorder ?: return null
        recorder = null
        return try {
            instance.stop()
            output
        } catch (e: Exception) {
            null
        } finally {
            runCatching { instance.release() }
            output = null
        }
    }

    fun cancel() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        output?.delete()
        output = null
    }
}

/** 从 Uri 读取全部字节（图片/音频选择结果）。 */
suspend fun readUriBytes(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull()
}

/** 毫秒 → "yyyy年M月d日"。 */
fun formatDateText(millis: Long): String =
    SimpleDateFormat("yyyy年M月d日", Locale.CHINA).format(Date(millis))

/** 毫秒 → "yyyy年M月d日 HH:mm"。 */
fun formatDateTimeText(millis: Long): String =
    SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA).format(Date(millis))
