/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.yuanqinglan.app.core.designsystem.QingLanGreenSoft
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.ui.ConfirmDangerDialog
import com.yuanqinglan.app.feature.memorial.model.MediaRef
import com.yuanqinglan.app.feature.memorial.model.MemorialLike

/**
 * 相册三列网格（多选 / 放大 / 删除二次确认）。
 * 选择态由调用方持有（纯集合逻辑见 [com.yuanqinglan.app.feature.memorial.model.AlbumSelect]）。
 */
@Composable
fun MemorialAlbumPanel(
    photos: List<MediaRef>,
    selection: Set<String>,
    selectionMode: Boolean,
    description: String,
    onToggleSelection: (String) -> Unit,
    onEnterSelection: () -> Unit,
    onExitSelection: () -> Unit,
    onView: (MediaRef) -> Unit,
    onDeleteSelected: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (selectionMode) {
                    "已选择 ${selection.size} 张"
                } else {
                    "相册（${photos.size}）"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (selectionMode) MaterialTheme.colorScheme.primary else TextSecondary,
            )
            Spacer(Modifier.weight(1f))
            if (selectionMode) {
                TextButton(onClick = onExitSelection) { Text("完成") }
            } else {
                TextButton(onClick = onEnterSelection) { Text("管理") }
            }
            if (selectionMode && selection.isNotEmpty()) {
                TextButton(onClick = { confirmDelete = true }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (photos.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("相册还是空的", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(
                    "在“主页/纪念空间”中添加照片后可在此管理",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(items = photos, key = { it.id }) { photo ->
                    AlbumCell(
                        photo = photo,
                        description = description,
                        selected = selection.contains(photo.id),
                        selectionMode = selectionMode,
                        onClick = {
                            if (selectionMode) onToggleSelection(photo.id) else onView(photo)
                        },
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDangerDialog(
            title = "删除选中照片",
            message = "确定删除选中的 ${selection.size} 张照片吗？删除后不可恢复。",
            confirmLabel = "删除",
            onConfirm = {
                confirmDelete = false
                onDeleteSelected(selection)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun AlbumCell(
    photo: MediaRef,
    description: String,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
    ) {
        MediaThumb(
            ref = photo,
            contentDescription = description,
            modifier = Modifier.fillMaxSize(),
        )
        if (selectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = if (selected) "已选中" else "未选中",
                    tint = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

/** 全屏放大查看单张媒体。 */
@Composable
fun FullscreenMediaDialog(
    photo: MediaRef,
    description: String,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xEE000000))
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    MediaThumb(
                        ref = photo,
                        contentDescription = description,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Text(
                    text = photo.name.ifBlank { description },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("关闭", color = Color.White)
                }
            }
        }
    }
}
