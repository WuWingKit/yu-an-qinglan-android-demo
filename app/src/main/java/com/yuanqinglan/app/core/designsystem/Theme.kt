/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val YuanQingLanColors = lightColorScheme(
    primary = QingLanGreen,
    onPrimary = Color.White,
    primaryContainer = QingLanGreenSoft,
    onPrimaryContainer = QingLanGreenDark,
    secondary = QingLanGreenDark,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceSoft,
    onSurfaceVariant = TextSecondary,
    outline = OutlineWarm,
    error = Warning,
)

/** 老年模式配色：仅加深次文本与描边，其余令牌与普通模式一致（保持令牌结构稳定）。 */
private val YuanQingLanElderColors = lightColorScheme(
    primary = QingLanGreen,
    onPrimary = Color.White,
    primaryContainer = QingLanGreenSoft,
    onPrimaryContainer = QingLanGreenDark,
    secondary = QingLanGreenDark,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceSoft,
    onSurfaceVariant = TextSecondaryElder,
    outline = OutlineWarmElder,
    error = Warning,
)

/**
 * 渝安青澜主题。老年模式开启时（[LocalElderMode] 为 true，
 * 由 [ProvideElderMode] 在本主题之前提供）自动切换放大字号与高对比配色。
 */
@Composable
fun YuanQingLanTheme(content: @Composable () -> Unit) {
    val elderMode = LocalElderMode.current
    MaterialTheme(
        colorScheme = if (elderMode) YuanQingLanElderColors else YuanQingLanColors,
        typography = if (elderMode) ElderYuanQingLanTypography else YuanQingLanTypography,
        content = content,
    )
}
