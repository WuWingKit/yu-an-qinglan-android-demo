/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.data

import android.content.Context
import java.io.File

/**
 * 运行时纪念状态 JSON 快照读写（可选保留：进程重启后若存在快照则恢复，
 * 否则回到内置内容）。快照存放在应用私有目录（与公共 FileStorage 同一根目录
 * `filesDir/yuanqinglan/` 下的 memorial/ 子目录），不进入外部存储。
 */
interface MemorialSnapshotIo {
    /** 读取指定名称的快照文本；不存在或读取失败返回 null。 */
    suspend fun read(name: String): String?

    /** 写入快照（覆盖）。 */
    suspend fun write(name: String, text: String)

    /** 删除快照（用于恢复内置内容）。 */
    suspend fun delete(name: String)
}

/** Android 实现：应用私有目录文件。图片/音频附件仍统一走公共 FileStorage。 */
class PrivateFileSnapshotIo(
    context: Context,
) : MemorialSnapshotIo {

    private val directory: File = File(context.filesDir, SNAPSHOT_ROOT).apply { mkdirs() }

    override suspend fun read(name: String): String? {
        val target = File(directory, name)
        return if (target.isFile) target.readText(Charsets.UTF_8) else null
    }

    override suspend fun write(name: String, text: String) {
        require(name.isNotBlank() && !name.contains('/')) { "快照文件名非法: $name" }
        directory.mkdirs()
        File(directory, name).writeText(text, Charsets.UTF_8)
    }

    override suspend fun delete(name: String) {
        val target = File(directory, name)
        if (target.isFile) target.delete()
    }

    private companion object {
        const val SNAPSHOT_ROOT = "yuanqinglan/memorial"
    }
}
