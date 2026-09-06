/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.currentTouchTargetSize
import com.yuanqinglan.app.core.model.AudienceTrack

private data class TrackPresentation(
    val track: AudienceTrack,
    val label: String,
    val icon: ImageVector,
)

private val AudienceTracks = listOf(
    TrackPresentation(AudienceTrack.HUMAN, "人类", Icons.Outlined.Person),
    TrackPresentation(AudienceTrack.PET, "宠物", Icons.Outlined.Pets),
)

/**
 * 人类/宠物双段切换（人宠服务与内容池隔离的总入口）。
 * 选中段为白色卡片、主色图标；整段可点，热区不小于 48dp（老年模式 52dp）。
 */
@Composable
fun AudienceSegment(
    selected: AudienceTrack,
    onSelect: (AudienceTrack) -> Unit,
    modifier: Modifier = Modifier,
) {
    val touchTarget = currentTouchTargetSize()
    val outlineColor = MaterialTheme.colorScheme.outline
    val shape = RoundedCornerShape(AppDimensions.CompactRadius)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, outlineColor), shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AudienceTracks.forEach { presentation ->
            val isSelected = presentation.track == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = touchTarget)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) SurfaceCard else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                        onClickLabel = "切换到${presentation.label}",
                        onClick = { onSelect(presentation.track) },
                    )
                    // 使用显式接收者，避免与外层参数 selected 同名遮蔽 semantics 属性。
                    .semantics { this.selected = isSelected },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = presentation.icon,
                        contentDescription = null,
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = presentation.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}
