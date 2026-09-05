/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.model

/**
 * 相册多选状态纯逻辑（不可变集合；供相册管理页与单元测试共用）。
 */
object AlbumSelect {

    /** 点击一项：已选中则取消，未选中则加入。 */
    fun toggle(current: Set<String>, mediaRefId: String): Set<String> =
        if (current.contains(mediaRefId)) current - mediaRefId else current + mediaRefId

    /** 清空选择。 */
    fun clear(current: Set<String>): Set<String> = emptySet()

    /** 仅保留仍存在于相册中的 ID（防止删除后选择态残留）。 */
    fun prune(current: Set<String>, existingIds: Set<String>): Set<String> =
        current intersect existingIds
}
