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

@Composable
fun YuanQingLanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = YuanQingLanColors,
        typography = YuanQingLanTypography,
        content = content,
    )
}
