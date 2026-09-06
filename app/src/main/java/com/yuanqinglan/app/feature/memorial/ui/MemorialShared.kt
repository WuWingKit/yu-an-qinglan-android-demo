/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.QingLanGreenSoft
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.feature.memorial.model.MediaKind
import com.yuanqinglan.app.feature.memorial.model.MediaRef
import com.yuanqinglan.app.feature.memorial.model.MemorialLike
import com.yuanqinglan.app.feature.memorial.model.MemorialTrack
import androidx.compose.foundation.Image

/** 轨道相关的对外语义文案（“纪念对象/纪念伙伴”等），避免页面各自硬编码。 */
object MemorialVocab {
    fun ofTrack(track: MemorialTrack): MemorialVocabText = when (track) {
        MemorialTrack.HUMAN -> human
        MemorialTrack.PET -> pet
    }

    fun ofMemorialId(memorialId: String): MemorialVocabText = ofTrack(MemorialTrack.ofId(memorialId))

    private val human = MemorialVocabText(
        subjectLabel = "纪念对象",
        relationLabel = "与TA的关系",
        portraitDescription = HUMAN_PORTRAIT_DESCRIPTION,
        galleryDescription = "家人纪念相册（示意内容）",
    )

    private val pet = MemorialVocabText(
        subjectLabel = "纪念伙伴",
        relationLabel = "与伙伴的关系",
        portraitDescription = PET_PORTRAIT_DESCRIPTION,
        galleryDescription = "伙伴纪念相册（示意内容）",
    )
}

/** 轨道语义文案值对象。 */
data class MemorialVocabText(
    val subjectLabel: String,
    val relationLabel: String,
    val portraitDescription: String,
    val galleryDescription: String,
)

/** 纪念空间摘要卡（首页列表/选择器共用）。 */
@Composable
fun MemorialSpaceCard(
    memorial: MemorialLike,
    vocab: MemorialVocabText,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        color = SurfaceCard,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(AppDimensions.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(QingLanGreenSoft),
            ) {
                Image(
                    painter = painterResource(memorialDrawable(memorial.portrait)),
                    contentDescription = vocab.portraitDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = memorial.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (memorial.relation.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "· ${memorial.relation}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            maxLines = 1,
                        )
                    }
                }
                if (memorial.intro.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = memorial.intro,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = buildString {
                        append("相册 ").append(memorial.gallery.size)
                        append(" · 寄语 ").append(memorial.messages.size)
                        append(" · 故事 ").append(memorial.stories.size)
                        append(" · 日记 ").append(memorial.diary.size)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** 竖排信息小结（名字/关系/简介 + 编辑动作）。 */
@Composable
fun MemorialIntroBlock(
    memorial: MemorialLike,
    vocab: MemorialVocabText,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = memorial.name,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (onEdit != null) {
                Text(
                    text = "编辑",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .clickable(role = Role.Button, onClickLabel = "编辑纪念空间", onClick = onEdit)
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                )
            }
        }
        if (memorial.relation.isNotBlank()) {
            Text(
                text = "${vocab.relationLabel}：${memorial.relation}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (memorial.intro.isNotBlank()) {
            Text(
                text = memorial.intro,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** 区块标题（与核心 SectionHeader 同风格；带图标可选）。 */
@Composable
fun MemorialSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) trailing()
    }
}

/** 页内空/错误状态壳（避免重复布局）。 */
@Composable
fun MemorialEmptyHint(title: String, description: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextSecondary)
        if (description != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}

/** 附加媒体项的源语义名。 */
fun mediaLabel(ref: MediaRef): String = when (ref.kind) {
    MediaKind.DRAWABLE, MediaKind.IMAGE_FILE -> ref.name.ifBlank { "图片" }
    MediaKind.AUDIO_FILE -> ref.name.ifBlank { "录音" }
    MediaKind.VIDEO_FILE -> ref.name.ifBlank { "视频" }
}

/** 轻量图标 + 文本入口块（首页/详情功能入口共用）。 */
@Composable
fun MemorialEntryTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        color = SurfaceCard,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 纪念空间选择对话框：从当前轨空间列表中选择一个后执行动作。 */
@Composable
fun SpacePickerDialog(
    title: String,
    spaces: List<MemorialLike>,
    onPick: (MemorialLike) -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            if (spaces.isEmpty()) {
                Text("当前还没有纪念空间，请先新建。", style = MaterialTheme.typography.bodyMedium)
            } else {
                Column {
                    spaces.forEach { space ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button, onClick = { onPick(space) })
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = space.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            if (space.relation.isNotBlank()) {
                                Text(
                                    text = space.relation,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        },
    )
}
