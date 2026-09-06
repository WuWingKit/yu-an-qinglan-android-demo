/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp

/**
 * 老年模式（适老模式）全局开关因子。
 * 开启后字号约放大 [ElderFontScale] 倍；点击热区提升到 [AppDimensions.ElderMinimumTouchTarget]。
 */
const val ElderFontScale = 1.25f

/** 全局老年模式状态：默认关闭。在 App 根部通过 [ProvideElderMode] 提供。 */
val LocalElderMode: ProvidableCompositionLocal<Boolean> =
    staticCompositionLocalOf { false }

/**
 * 提供老年模式开关值，供主题（字号/对比度）与各组件（触达尺寸）读取。
 * 普通模式显式传入 false 时同样会覆盖默认值。
 */
@Composable
fun ProvideElderMode(enabled: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalElderMode provides enabled, content = content)
}

/**
 * 当前生效的最小点击热区：老年模式 52dp，普通模式 48dp。
 * 组件应在需要保证触达尺寸的地方调用本函数，而不是写死缩放。
 */
@Composable
fun currentTouchTargetSize(): Dp {
    return if (LocalElderMode.current) {
        AppDimensions.ElderMinimumTouchTarget
    } else {
        AppDimensions.MinimumTouchTarget
    }
}
