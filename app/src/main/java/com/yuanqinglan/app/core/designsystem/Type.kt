/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

val YuanQingLanTypography = Typography(
    headlineMedium = TextStyle(
        fontSize = 22.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleLarge = TextStyle(
        fontSize = 17.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
)

private fun TextStyle.scaleFont(factor: Float): TextStyle {
    fun scaleTextUnit(value: TextUnit): TextUnit =
        if (value == TextUnit.Unspecified) value else value * factor
    return copy(
        fontSize = scaleTextUnit(fontSize),
        lineHeight = scaleTextUnit(lineHeight),
        letterSpacing = scaleTextUnit(letterSpacing),
    )
}

/**
 * 老年模式排版：在 [YuanQingLanTypography] 基础上按 [ElderFontScale] 等比放大
 * 全部字号与行高（未指定的槽位保持系统默认值）。令牌结构与普通模式完全一致。
 */
val ElderYuanQingLanTypography: Typography = run {
    val base = YuanQingLanTypography
    fun scale(style: TextStyle): TextStyle = style.scaleFont(ElderFontScale)
    Typography(
        displayLarge = scale(base.displayLarge),
        displayMedium = scale(base.displayMedium),
        displaySmall = scale(base.displaySmall),
        headlineLarge = scale(base.headlineLarge),
        headlineMedium = scale(base.headlineMedium),
        headlineSmall = scale(base.headlineSmall),
        titleLarge = scale(base.titleLarge),
        titleMedium = scale(base.titleMedium),
        titleSmall = scale(base.titleSmall),
        bodyLarge = scale(base.bodyLarge),
        bodyMedium = scale(base.bodyMedium),
        bodySmall = scale(base.bodySmall),
        labelLarge = scale(base.labelLarge),
        labelMedium = scale(base.labelMedium),
        labelSmall = scale(base.labelSmall),
    )
}
