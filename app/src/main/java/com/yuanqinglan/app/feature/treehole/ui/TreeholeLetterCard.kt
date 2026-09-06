/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.treehole.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.QingLanGreen
import com.yuanqinglan.app.core.designsystem.QingLanGreenDark
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.feature.treehole.model.TreeholeAuthor
import com.yuanqinglan.app.feature.treehole.model.TreeholeAvatarStyle
import com.yuanqinglan.app.feature.treehole.model.TreeholeLetterLike
import java.time.Instant
import java.time.ZoneId

/*
 * 头像占位的本地扩展色（仅本文件使用，不改动设计系统；主/深绿与暖橙复用现有令牌）。
 */
private val AvatarRose = Color(0xFFC27A8B)
private val AvatarAmber = Color(0xFFBF8B3C)
private val AvatarIndigo = Color(0xFF6C7FB2)
private val AvatarTeal = Color(0xFF4F8E86)
private val AvatarBlue = Color(0xFF6B8CB3)

/**
 * 头像 token → 图标（素材来源：androidx.compose.material.icons，Apache-2.0，
 * 随 Compose Material 依赖内置；不引入网络/Coil）。
 */
internal fun TreeholeAvatarStyle.imageVector(): ImageVector = when (this) {
    TreeholeAvatarStyle.LEAF -> Icons.Outlined.Eco
    TreeholeAvatarStyle.FLOWER -> Icons.Outlined.LocalFlorist
    TreeholeAvatarStyle.LIGHT -> Icons.Outlined.Lightbulb
    TreeholeAvatarStyle.STAR -> Icons.Outlined.Star
    TreeholeAvatarStyle.HEART -> Icons.Filled.Favorite
    TreeholeAvatarStyle.MOON -> Icons.Outlined.DarkMode
    TreeholeAvatarStyle.SUN -> Icons.Outlined.WbSunny
    TreeholeAvatarStyle.PAW -> Icons.Outlined.Pets
    TreeholeAvatarStyle.PARK -> Icons.Outlined.Park
    TreeholeAvatarStyle.CLOUD -> Icons.Outlined.Cloud
    TreeholeAvatarStyle.WATERDROP -> Icons.Outlined.WaterDrop
    TreeholeAvatarStyle.SPA -> Icons.Outlined.Spa
    TreeholeAvatarStyle.PERSON -> Icons.Filled.Person
}

/** 头像 token → 主题色（柔和底 + 同色图标；主色复用设计令牌）。 */
internal fun TreeholeAvatarStyle.tintColor(): Color = when (this) {
    TreeholeAvatarStyle.LEAF, TreeholeAvatarStyle.PERSON -> QingLanGreen
    TreeholeAvatarStyle.PAW -> QingLanGreenDark
    TreeholeAvatarStyle.FLOWER, TreeholeAvatarStyle.HEART -> AvatarRose
    TreeholeAvatarStyle.LIGHT, TreeholeAvatarStyle.SUN -> AvatarAmber
    TreeholeAvatarStyle.STAR, TreeholeAvatarStyle.MOON -> AvatarIndigo
    TreeholeAvatarStyle.PARK, TreeholeAvatarStyle.SPA -> AvatarTeal
    TreeholeAvatarStyle.CLOUD, TreeholeAvatarStyle.WATERDROP -> AvatarBlue
}

/** 信件时间：毫秒时间戳 → 「yyyy年M月d日」中文日期（本地时区）。 */
internal fun formatLetterDate(millis: Long): String {
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
}

/**
 * 作者展示头部（可复用）：圆形头像占位、虚构昵称、非实名 ID 与写信时间。
 *
 * 仅承载最小展示字段，不建立任何社交关系；[author] 缺失（旧数据/本人信件）时
 * 回退产品默认展示，头像缺失/未知 token 时回退产品默认头像。
 */
@Composable
internal fun TreeholeAuthorHeader(
    author: TreeholeAuthor?,
    dateMillis: Long,
    modifier: Modifier = Modifier,
) {
    val safeAuthor = author ?: TreeholeAuthor()
    val style = TreeholeAvatarStyle.fromToken(safeAuthor.avatar)
    val tint = style.tintColor()
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BoxAvatar(style = style, tint = tint)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = safeAuthor.displayNickname,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "ID ${safeAuthor.displayAnonId}",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatLetterDate(dateMillis),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            maxLines = 1,
        )
    }
}

/** 圆形头像占位（Material Icons 图标 + 柔和同色底）。 */
@Composable
private fun BoxAvatar(style: TreeholeAvatarStyle, tint: Color) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = style.imageVector(),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * 拾信阅读卡：作者头部、分类、信纸、标题、正文与附件，适合连续阅读。
 *
 * 内容池只读展示用卡片；作者展示字段（虚构昵称/非实名 ID/头像/时间）由
 * App 2.0 内容扩充（Issue #22）叠加在 [TreeholeAuthorHeader] 内。
 */
@Composable
internal fun ReadLetterCard(letter: TreeholeLetterLike, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        color = SurfaceCard,
    ) {
        Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
            TreeholeAuthorHeader(author = letter.author, dateMillis = letter.createdAtMillis)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TreeholeCategoryTag(text = letter.category)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "信纸 · ${letter.paper.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = letter.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            TreeholeLetterBodyText(body = letter.body)
            TreeholeLetterAttachmentBlock(letter = letter)
        }
    }
}
