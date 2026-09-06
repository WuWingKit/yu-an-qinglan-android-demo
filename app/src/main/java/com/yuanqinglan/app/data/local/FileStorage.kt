/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.data.local

import android.content.Context
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 应用私有目录文件存储：在 `context.filesDir/yuanqinglan/` 下保存/删除图片与音频，
 * 返回可直接复用的 file:// Uri。用于头像、相册、日记附件与树洞附件。
 * 内容只存在于应用私有目录，不申请任何外部存储权限。
 */
class FileStorage(
    private val context: Context,
    private val delayMillis: Long = DEFAULT_SIMULATED_DELAY_MILLIS,
) {

    private val rootDir: File
        get() = File(context.filesDir, "yuanqinglan")

    /**
     * 保存图片字节到私有目录 images/，返回 file:// Uri。
     * [fileExtension] 传入不含点的扩展名（如 webp/jpg/png）。
     */
    suspend fun saveImage(bytes: ByteArray, fileExtension: String = "webp"): Uri {
        requireValidExtension(fileExtension)
        return writeToDisk(bytes, File(rootDir, "images"), "img", fileExtension)
    }

    /**
     * 保存音频字节到私有目录 audio/，返回 file:// Uri。
     * [fileExtension] 传入不含点的扩展名（如 m4a/aac/mp3）。
     */
    suspend fun saveAudio(bytes: ByteArray, fileExtension: String = "m4a"): Uri {
        requireValidExtension(fileExtension)
        return writeToDisk(bytes, File(rootDir, "audio"), "audio", fileExtension)
    }

    /**
     * 保存任意附件字节到指定私有子目录，返回 file:// Uri。
     * 文件名自动加时间戳前缀避免覆盖。
     */
    suspend fun save(bytes: ByteArray, directoryName: String, fileName: String): Uri {
        require(fileName.isNotBlank()) { "fileName 不能为空" }
        require(SAFE_DIRECTORY_REGEX.matches(directoryName)) {
            "directoryName 只允许字母数字、下划线与连字符，实际: $directoryName"
        }
        return writeToDisk(bytes, File(rootDir, directoryName), safeBaseName(fileName), null)
    }

    /** 删除私有目录内的文件（拒绝删除目录外的路径），返回是否删除成功。 */
    suspend fun delete(uri: Uri): Boolean {
        simulateDelay()
        return withContext(Dispatchers.IO) {
            val file = fileOf(uri) ?: return@withContext false
            file.isFile && file.delete()
        }
    }

    /** 判断该 Uri 是否对应应用私有目录内存在的文件。 */
    suspend fun exists(uri: Uri): Boolean {
        simulateDelay()
        return withContext(Dispatchers.IO) {
            val file = fileOf(uri) ?: return@withContext false
            file.isFile
        }
    }

    /** 将 Uri 解析为私有目录内的 File；不在目录内（越界/不可信路径）返回 null。 */
    fun fileOf(uri: Uri): File? {
        if (uri.scheme != "file") return null
        val path = uri.path ?: return null
        val normalizedRoot = rootDir.canonicalFile
        val candidate = File(path).canonicalFile
        return if (candidate.path.startsWith(normalizedRoot.path)) candidate else null
    }

    private suspend fun writeToDisk(
        bytes: ByteArray,
        directory: File,
        baseName: String,
        ext: String?,
    ): Uri {
        simulateDelay()
        return withContext(Dispatchers.IO) {
            if (!directory.exists() && !directory.mkdirs()) {
                throw java.io.IOException("无法创建私有存储目录：${directory.absolutePath}")
            }
            val target = File(
                directory,
                "${System.currentTimeMillis()}_$baseName" + (ext?.let { ".$it" } ?: ""),
            )
            target.writeBytes(bytes)
            Uri.fromFile(target)
        }
    }

    private suspend fun simulateDelay() {
        if (delayMillis > 0L) delay(delayMillis)
    }

    private fun requireValidExtension(fileExtension: String) {
        require(fileExtension.isNotBlank() && !fileExtension.contains('.')) {
            "fileExtension 应为不含点的扩展名，实际: $fileExtension"
        }
    }

    private fun safeBaseName(name: String): String {
        val sanitized = name.replace(Regex("[^\\p{Alnum}\\u4e00-\\u9fa5._-]"), "_")
        return sanitized.ifBlank { "file" }
    }

    companion object {
        const val DEFAULT_SIMULATED_DELAY_MILLIS = 350L

        private val SAFE_DIRECTORY_REGEX = Regex("^[\\p{Alnum}_-]+$")
    }
}
