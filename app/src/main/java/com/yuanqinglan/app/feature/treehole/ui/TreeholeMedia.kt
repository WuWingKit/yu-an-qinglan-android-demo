/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.treehole.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaRecorder
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 树洞附件本地 IO 小工具：读取选择结果字节、解析文件名/扩展名、采样解码
 * 私有目录图片，以及麦克风录音控制器。全部为树洞自实现，不依赖其他 feature。
 * 附件只进入应用私有目录，不申请外部存储权限。
 */

/** 从系统选择器返回的 Uri 读取全部字节（图片/音频共用），失败返回 null。 */
suspend fun readContentBytes(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull()
}

/** 查询系统选择器 Uri 的展示名；查询失败时退回 Uri 最后一段，仍不可用返回 null。 */
fun queryDisplayName(context: Context, uri: Uri): String? {
    val fromQuery = runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) cursor.getString(index) else null
        }
    }.getOrNull()
    val fromQueryTrimmed = fromQuery?.trim()?.takeIf { it.isNotEmpty() }
    if (fromQueryTrimmed != null) return fromQueryTrimmed
    val segment = uri.lastPathSegment?.trim()?.takeIf { it.isNotEmpty() }
    return segment?.let { it.substringAfterLast('/') }?.takeIf { it.isNotEmpty() }
}

/** 从文件名提取扩展名（纯字母数字 1..8 位）；不含规则扩展名时回退 [defaultExtension]。 */
fun extensionOf(name: String?, defaultExtension: String): String {
    val raw = name?.substringAfterLast('.', "")?.trim().orEmpty()
    return if (raw.matches(Regex("^[A-Za-z0-9]{1,8}$"))) raw.lowercase(Locale.CHINA) else defaultExtension
}

/** 字节数 → 可读文案（B/KB/MB）。 */
fun formatByteSize(sizeBytes: Long): String {
    val kb = 1024L
    val mb = kb * 1024L
    return when {
        sizeBytes >= mb -> String.format(Locale.CHINA, "%.1f MB", sizeBytes.toDouble() / mb)
        sizeBytes >= kb -> String.format(Locale.CHINA, "%.0f KB", sizeBytes.toDouble() / kb)
        else -> "$sizeBytes B"
    }
}

/**
 * 采样解码私有目录 file:// 图片（大图先读尺寸再按 1400px 目标采样，防 OOM）。
 * 解码失败/非 file 协议返回 null，由调用方给出"暂时无法查看"提示。
 */
suspend fun decodeSampleBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    if (uri.scheme != "file") return@withContext null
    val path = uri.path ?: return@withContext null
    val file = File(path)
    if (!file.isFile) return@withContext null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
    val sample = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxDimension = 1400)
    val decode = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    runCatching { BitmapFactory.decodeFile(path, decode) }.getOrNull()
}

private fun sampleSizeFor(width: Int, height: Int, maxDimension: Int): Int {
    var sample = 1
    var current = maxOf(width, height)
    while (current > maxDimension) {
        sample *= 2
        current /= 2
    }
    return sample
}

/**
 * 本地录音控制器：输出到应用缓存目录，停止成功后才交给调用方读取并转存
 * 私有目录（录音失败/取消时清理缓存文件，不残留）。
 */
class TreeholeAudioRecorder(context: Context) {

    private val appContext = context.applicationContext
    private var recorder: MediaRecorder? = null
    private var output: File? = null

    val isRecording: Boolean
        get() = recorder != null

    /** 开始录音（需已获得 RECORD_AUDIO 权限）；硬件/格式异常返回 false。 */
    @Suppress("DEPRECATION")
    fun start(): Boolean {
        if (recorder != null) return false
        val target = File(appContext.cacheDir, "treehole_rec_${System.currentTimeMillis()}.m4a")
        return try {
            val instance = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(target.absolutePath)
                prepare()
            }
            instance.start()
            recorder = instance
            output = target
            true
        } catch (e: Exception) {
            runCatching { recorder?.release() }
            recorder = null
            runCatching { target.delete() }
            false
        }
    }

    /** 停止并返回录音文件；录音过短/失败时清理并返回 null。 */
    @Suppress("DEPRECATION")
    fun stop(): File? {
        val instance = recorder ?: return null
        recorder = null
        val file = output
        output = null
        return try {
            instance.stop()
            val result = file?.takeIf { it.isFile && it.length() > 0L }
            if (result == null) file?.delete()
            result
        } catch (e: Exception) {
            runCatching { instance.reset() }
            file?.delete()
            null
        } finally {
            runCatching { instance.release() }
        }
    }

    /** 放弃录音：停止并清理缓存文件。 */
    @Suppress("DEPRECATION")
    fun cancel() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        output?.delete()
        output = null
    }
}
