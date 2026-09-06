/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.profile.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yuanqinglan.app.R
import com.yuanqinglan.app.core.designsystem.OutlineWarm
import com.yuanqinglan.app.core.designsystem.TextSecondary

private val authorizationPages = intArrayOf(
    R.drawable.authorization_li_yunfeng_zh,
    R.drawable.authorization_li_yunfeng_en,
)

/** 个人中心页底的低强调版权与当前有效授权说明。 */
@Composable
fun AuthorizationCopyrightFooter(modifier: Modifier = Modifier) {
    var showAuthorization by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalDivider(color = OutlineWarm.copy(alpha = 0.65f))
        Spacer(Modifier.size(14.dp))
        Text(
            text = "Copyright © 2026 西南大学24级学行科创班胡荣杰（WuWingKit）",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary.copy(alpha = 0.78f),
            textAlign = TextAlign.Center,
        )
        Text(
            text = "保留所有权利 · 依据 GitHub 仓库专有 License",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 3.dp),
        )

        Surface(
            onClick = { showAuthorization = true },
            modifier = Modifier
                .padding(top = 12.dp)
                .width(104.dp)
                .aspectRatio(210f / 297f)
                .border(0.5.dp, OutlineWarm, RoundedCornerShape(4.dp)),
            shape = RoundedCornerShape(4.dp),
            color = Color.White,
        ) {
            Image(
                painter = painterResource(R.drawable.authorization_li_yunfeng_zh),
                contentDescription = "打开李芸凤软件使用授权书",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Text(
            text = "已授权给西南大学经济管理学院李芸凤",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "仅限 2026年重庆市大学生新文科实践创新大赛非商业用途",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary.copy(alpha = 0.82f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
        )
        TextButton(onClick = { showAuthorization = true }) {
            Icon(
                imageVector = Icons.Outlined.OpenInFull,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("查看软件使用授权书")
        }
    }

    if (showAuthorization) {
        AuthorizationViewerDialog(onDismiss = { showAuthorization = false })
    }
}

@Composable
private fun AuthorizationViewerDialog(onDismiss: () -> Unit) {
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

@Composable
private fun ZoomableAuthorizationPage(
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
