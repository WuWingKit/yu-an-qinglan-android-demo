/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.LawnSoft
import com.yuanqinglan.app.core.designsystem.QingLanGreen
import com.yuanqinglan.app.core.designsystem.QingLanGreenSoft
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.ToolBlueSoft

/** 提示/合规横幅的语气：普通提示、政策合规、警示。 */
enum class NoticeTone {
    INFO,
    COMPLIANCE,
    WARNING,
}

private data class NoticePalette(
    val container: Color,
    val bar: Color,
    val icon: ImageVector,
)

private fun NoticeTone.palette(): NoticePalette = when (this) {
    NoticeTone.INFO -> NoticePalette(
        container = QingLanGreenSoft,
        bar = QingLanGreen,
        icon = Icons.Outlined.Info,
    )
    NoticeTone.COMPLIANCE -> NoticePalette(
        container = ToolBlueSoft,
        bar = Color(0xFF4E7790),
        icon = Icons.Outlined.VerifiedUser,
    )
    NoticeTone.WARNING -> NoticePalette(
        container = LawnSoft,
        bar = Color(0xFFA67C52),
        icon = Icons.Outlined.WarningAmber,
    )
}

/**
 * 合规/提示条：左侧色条 + 状态图标 + 说明文字，圆角 10dp。
 * 用于政策提示、伦理说明与温和警示；文案由调用方提供（不得含"演示"等字样）。
 */
@Composable
fun NoticeBanner(
    text: String,
    tone: NoticeTone = NoticeTone.INFO,
    modifier: Modifier = Modifier,
) {
    val palette = tone.palette()
    val shape = RoundedCornerShape(AppDimensions.CompactRadius)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = palette.container,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .background(palette.bar, RoundedCornerShape(2.dp)),
            )
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = palette.icon,
                contentDescription = null,
                tint = palette.bar,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
