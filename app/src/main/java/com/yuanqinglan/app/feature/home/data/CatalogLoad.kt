/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.home.data

import com.yuanqinglan.app.core.model.DemoState
import kotlinx.coroutines.CancellationException

/** 首页内容加载辅助：把 suspend 列表加载转为可展示的 [DemoState]。 */
internal suspend fun <T> loadListState(
    block: suspend () -> List<T>,
    failureMessage: String = "内容加载失败，请稍后重试。",
): DemoState<List<T>> = try {
    val list = block()
    if (list.isEmpty()) DemoState.Empty else DemoState.Success(list)
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    DemoState.Error(failureMessage)
}
