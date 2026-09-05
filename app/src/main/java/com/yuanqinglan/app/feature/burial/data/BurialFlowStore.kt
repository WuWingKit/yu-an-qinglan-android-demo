/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial.data

import com.yuanqinglan.app.core.model.AudienceTrack
import com.yuanqinglan.app.feature.burial.model.BurialMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 当前安葬浏览上下文（轨道 + 模式）。
 *
 * 契约冻结 `plan` 路由无参，套餐页无法从导航参数获得上下文，
 * 因此由详情页在跳转“查看套餐”前写入本处（导航仍只传稳定路由）；
 * 直接进入套餐页或进程重建时回退默认值（人类 / 树葬），保证不崩溃、不串轨。
 */
data class BurialPlanContext(
    val track: AudienceTrack = AudienceTrack.HUMAN,
    val mode: BurialMode = BurialMode.TREE,
)

object BurialFlowStore {
    private val _context = MutableStateFlow(BurialPlanContext())
    val context: StateFlow<BurialPlanContext> = _context.asStateFlow()

    fun setPlanContext(track: AudienceTrack, mode: BurialMode) {
        _context.value = BurialPlanContext(track, mode)
    }
}
