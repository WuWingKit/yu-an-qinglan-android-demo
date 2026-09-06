/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.core.model

sealed interface DemoState<out T> {
    data object Loading : DemoState<Nothing>
    data object Empty : DemoState<Nothing>
    data class Success<T>(val value: T) : DemoState<T>
    data class Error(val message: String) : DemoState<Nothing>
}
