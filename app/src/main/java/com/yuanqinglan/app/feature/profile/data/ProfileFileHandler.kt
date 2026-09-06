/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.profile.data

import android.content.Context
import android.net.Uri
import com.yuanqinglan.app.data.local.FileStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 头像/素材文件的私有拷贝与删除抽象（便于 VM 单元测试注入假实现）。 */
interface ProfileMediaImporter {
    /** 拷贝图片到私有 images/，返回 file:// uri；失败返回 null。 */
    suspend fun importImageToPrivate(sourceUri: String): String?

    /** 拷贝音频到私有 audio/，返回 file:// uri；失败返回 null。 */
    suspend fun importAudioToPrivate(sourceUri: String): String?

    /** 删除私有目录文件（越界/不存在返回 false）。 */
    suspend fun deletePrivateFile(uriString: String?): Boolean
}

/**
 * [ProfileMediaImporter] 的默认实现：把系统相册/文件选择返回的 content Uri
 * 拷贝到应用私有目录（经公共 [FileStorage]），仅在本机保留、可随时删除。
 * 不申请存储权限、不对外传输。
 */
class ProfileFileHandler(
    context: Context,
    private val fileStorage: FileStorage = FileStorage(context.applicationContext),
) : ProfileMediaImporter {

    private val resolver = context.applicationContext.contentResolver

    override suspend fun importImageToPrivate(sourceUri: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(sourceUri)
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@runCatching null
                val ext = resolver.getType(uri)?.let { mimeToExt(it) } ?: "jpg"
                fileStorage.saveImage(bytes, ext).toString()
            }.getOrNull()
        }

    override suspend fun importAudioToPrivate(sourceUri: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(sourceUri)
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@runCatching null
                fileStorage.saveAudio(bytes, AUDIO_EXT).toString()
            }.getOrNull()
        }

    override suspend fun deletePrivateFile(uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        return runCatching { fileStorage.delete(Uri.parse(uriString)) }.getOrDefault(false)
    }

    private fun mimeToExt(mime: String): String = when {
        mime.contains("png") -> "png"
        mime.contains("webp") -> "webp"
        mime.contains("gif") -> "gif"
        else -> "jpg"
    }

    companion object {
        const val AUDIO_EXT = "m4a"
    }
}
