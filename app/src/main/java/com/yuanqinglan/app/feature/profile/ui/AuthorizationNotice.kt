/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.profile.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Copyright
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yuanqinglan.app.R
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.designsystem.currentTouchTargetSize

/**
 * 已签名软件使用授权书页面（中/英两页，1.1.0 打包的 drawable 资源，不重新打包）。
 * 图片仅随 APK 内置、在本机内存中展示：不写日志、不参与分析事件、不落外部存储。
 */
internal val authorizationPages = intArrayOf(
    R.drawable.authorization_li_yunfeng_zh,
    R.drawable.authorization_li_yunfeng_en,
)

/**
 * “我的”页底部单行低强调入口：版权及授权情况。
 * 整行可点击热区不小于 [currentTouchTargetSize]（普通 48dp / 老年 52dp）。
 * 版权长文、被授权人信息与证书缩略图已移入详情页，首页不再重复展示。
 */
@Composable
fun CopyrightAuthorizationRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val touchTarget = currentTouchTargetSize()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = touchTarget)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Copyright,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "版权及授权情况",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary.copy(alpha = 0.72f),
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * 全屏证书查看器：中/英两页已签名授权书，支持切页、缩放（1x–4x）与平移。
 * 顶部语言标签、底部页码与上一页/下一页操作均可被 TalkBack 朗读。
 */
@Composable
internal fun AuthorizationViewerDialog(onDismiss: () -> Unit) {
    var pageIndex by rememberSaveable { mutableIntStateOf(0) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF121614),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "关闭授权书",
                            tint = Color.White,
                        )
                    }
                    Text(
                        text = "软件使用授权书",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (pageIndex == 0) "中文" else "English",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.padding(end = 12.dp),
                    )
                }

                ZoomableAuthorizationPage(
                    resId = authorizationPages[pageIndex],
                    description = if (pageIndex == 0) "软件使用授权书中文版" else "Software use authorization in English",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF0A0C0B)),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { pageIndex = 0 },
                        enabled = pageIndex > 0,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "上一页",
                            tint = if (pageIndex > 0) Color.White else Color.White.copy(alpha = 0.28f),
                        )
                    }
                    Text(
                        text = "授权书 ${pageIndex + 1} / ${authorizationPages.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 18.dp),
                    )
                    IconButton(
                        onClick = { pageIndex = authorizationPages.lastIndex },
                        enabled = pageIndex < authorizationPages.lastIndex,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = "下一页",
                            tint = if (pageIndex < authorizationPages.lastIndex) Color.White else Color.White.copy(alpha = 0.28f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 授权书单页：内置 drawable 适配显示，支持单指缩放（1x–4x）与平移。
 * 图片为本机内存内展示，不写日志、不参与分析、不落外部存储。
 */
@Composable
internal fun ZoomableAuthorizationPage(
    @DrawableRes resId: Int,
    description: String,
    modifier: Modifier = Modifier,
) {
    var scale by remember(resId) { mutableFloatStateOf(1f) }
    var offset by remember(resId) { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 4f)
        scale = nextScale
        offset = if (nextScale == 1f) Offset.Zero else offset + panChange
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(resId),
            contentDescription = description,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                )
                .transformable(transformState)
                .semantics { role = Role.Image },
        )
    }
}
