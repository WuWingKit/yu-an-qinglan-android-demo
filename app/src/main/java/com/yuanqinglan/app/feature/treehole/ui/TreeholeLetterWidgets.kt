/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.treehole.ui

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.QingLanGreenSoft
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.feature.treehole.model.TreeholeAttachment
import com.yuanqinglan.app.feature.treehole.model.TreeholeAttachmentKind
import com.yuanqinglan.app.feature.treehole.model.TreeholeLetterLike

/**
 * 信件展示共用部件（拾信与我的信件复用）：分类标签、可展开正文、
 * 图片缩略（可点开查看）、音频播放行、状态徽标。
 */

/** 分类小标签（浅绿底胶囊）。 */
@Composable
internal fun TreeholeCategoryTag(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AppDimensions.CompactRadius))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** 审核中徽标（本地状态，非社交计数）。 */
@Composable
internal fun TreeholeReviewingBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AppDimensions.CompactRadius))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = "审核中",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

/** 正文（默认折叠 4 行，超过时提供展开/收起）。 */
@Composable
internal fun TreeholeLetterBodyText(
    body: String,
    modifier: Modifier = Modifier,
    collapsedMaxLines: Int = 4,
) {
    var expanded by remember(body) { mutableStateOf(false) }
    val collapsible = body.length > 60
    Column(modifier = modifier) {
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            maxLines = if (expanded || !collapsible) Int.MAX_VALUE else collapsedMaxLines,
            overflow = TextOverflow.Ellipsis,
        )
        if (collapsible) {
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Text(
                    text = if (expanded) "收起" else "展开全文",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** 信件附件展示区（图片缩略 + 音频播放行，按实际存在渲染）。 */
@Composable
internal fun TreeholeLetterAttachmentBlock(letter: TreeholeLetterLike, modifier: Modifier = Modifier) {
    val hasImage = letter.image != null
    val hasAudio = letter.audio != null
    if (!hasImage && !hasAudio) return
    Column(modifier = modifier) {
        letter.image?.let { image ->
            TreeholeImageThumb(attachment = image)
            Spacer(Modifier.height(8.dp))
        }
        letter.audio?.let { audio ->
            TreeholeAudioPlayRow(attachment = audio)
        }
    }
}

private enum class AudioPlayState {
    IDLE,
    PREPARING,
    PLAYING,
    PAUSED,
    ERROR,
}

/** 音频播放行：本地 MediaPlayer 播放/暂停，出错时明确提示不可用。 */
@Composable
internal fun TreeholeAudioPlayRow(
    attachment: TreeholeAttachment,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var state by remember(attachment.id) { mutableStateOf(AudioPlayState.IDLE) }
    var errorNote by remember(attachment.id) { mutableStateOf<String?>(null) }
    val player = remember(attachment.id) { MediaPlayer() }

    DisposableEffect(attachment.id) {
        onDispose { runCatching { player.release() } }
    }

    player.setOnPreparedListener {
        state = AudioPlayState.PLAYING
        runCatching { it.start() }
    }
    player.setOnCompletionListener { state = AudioPlayState.IDLE }
    player.setOnErrorListener { _, _, _ ->
        state = AudioPlayState.ERROR
        errorNote = "该音频暂时无法播放"
        true
    }

    val togglePlayback: () -> Unit = {
        when (state) {
            AudioPlayState.IDLE, AudioPlayState.ERROR -> {
                errorNote = null
                try {
                    player.reset()
                    player.setDataSource(context, Uri.parse(attachment.uri))
                    state = AudioPlayState.PREPARING
                    player.prepareAsync()
                } catch (e: Exception) {
                    state = AudioPlayState.ERROR
                    errorNote = "该音频暂时无法播放"
                }
            }
            AudioPlayState.PREPARING -> Unit
            AudioPlayState.PLAYING -> {
                runCatching { player.pause() }
                state = AudioPlayState.PAUSED
            }
            AudioPlayState.PAUSED -> {
                runCatching { player.start() }
                state = AudioPlayState.PLAYING
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CompactRadius),
        color = SurfaceCard,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(AppDimensions.CompactRadius))
                .clickable(onClick = togglePlayback)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(QingLanGreenSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (state == AudioPlayState.PLAYING) {
                        Icons.Outlined.Pause
                    } else {
                        Icons.Filled.PlayArrow
                    },
                    contentDescription = if (state == AudioPlayState.PLAYING) "暂停" else "播放",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.name.ifBlank { "音频" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = errorNote ?: formatByteSize(attachment.sizeBytes),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 图片附件：缩略展示；点击弹出查看（解码失败给出明确提示，不强制解码）。 */
@Composable
internal fun TreeholeImageThumb(
    attachment: TreeholeAttachment,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var viewing by remember(attachment.id) { mutableStateOf(false) }
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = attachment.id) {
        value = try {
            decodeSampleBitmap(context, Uri.parse(attachment.uri))?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(AppDimensions.CompactRadius))
            .background(SurfaceCard)
            .clickable { viewing = true },
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = "信件图片附件",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "图片",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = attachment.name.ifBlank { "" },
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    if (viewing) {
        TreeholeImageViewerDialog(
            attachmentName = attachment.name.ifBlank { "图片" },
            bitmap = bitmap,
            onDismiss = { viewing = false },
        )
    }
}

@Composable
private fun TreeholeImageViewerDialog(
    attachmentName: String,
    bitmap: ImageBitmap?,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(AppDimensions.CardRadius),
            color = SurfaceCard,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = attachmentName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(12.dp))
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!,
                        contentDescription = attachmentName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "图片暂时无法查看",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        text = "关闭",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** 附件类型辅助判断（写草稿列表使用）。 */
internal fun TreeholeAttachment.isImage(): Boolean = kind == TreeholeAttachmentKind.IMAGE

internal fun TreeholeAttachment.isAudio(): Boolean = kind == TreeholeAttachmentKind.AUDIO
