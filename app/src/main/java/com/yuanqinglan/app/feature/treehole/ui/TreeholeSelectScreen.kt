/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.treehole.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.yuanqinglan.app.core.designsystem.AppBackground
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.QingLanGreenSoft
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.ui.ReferenceNote
import com.yuanqinglan.app.data.local.AppContainer
import com.yuanqinglan.app.feature.treehole.model.TreeholePoolType
import com.yuanqinglan.app.navigation.AppRoute

/**
 * 树洞入口页（一级 Tab 根页，不带标题栏壳）：心灵树洞大标题 + 人间/生灵
 * 两张入口卡。点击入口先检查本会话"游客确认"：未确认弹本地对话框说明，
 * 确认后允许进入对应内容池；两个池的确认互不影响。
 *
 * 受 [AppContainer.settings] 的心灵树洞总开关控制：关闭时展示"已关闭"状态，
 * 并提示前往「我的」重新开启；开启时正常展示双池入口。
 */
@Composable
fun TreeholeSelectScreen(navController: NavHostController) {
    val treeholeEnabled by AppContainer.settings.treeholeEnabled
        .collectAsStateWithLifecycle(initialValue = true)

    // 游客确认仅记录本会话，两池分别独立。
    var humanAcknowledged by rememberSaveable { mutableStateOf(false) }
    var petAcknowledged by rememberSaveable { mutableStateOf(false) }
    var gatePool by remember { mutableStateOf<TreeholePoolType?>(null) }

    fun acknowledgedFor(type: TreeholePoolType): Boolean = when (type) {
        TreeholePoolType.HUMAN_POOL -> humanAcknowledged
        TreeholePoolType.PET_POOL -> petAcknowledged
    }

    fun openPool(type: TreeholePoolType) {
        val route = when (type) {
            TreeholePoolType.HUMAN_POOL -> AppRoute.TREEHOLE_HUMAN.route
            TreeholePoolType.PET_POOL -> AppRoute.TREEHOLE_PET.route
        }
        navController.navigate(route)
    }

    fun requestEnter(type: TreeholePoolType) {
        if (acknowledgedFor(type)) {
            openPool(type)
        } else {
            gatePool = type
        }
    }

    fun goToSettings() {
        navController.navigate(AppRoute.PROFILE.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = AppDimensions.PageHorizontal),
    ) {
        Spacer(Modifier.height(18.dp))
        Text(
            text = "心灵树洞",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "人间与生灵两个独立内容池，用匿名信件安放说不出口的话",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(20.dp))

        if (treeholeEnabled) {
            TreeholeEntryCard(
                icon = Icons.Outlined.Person,
                title = TreeholePoolType.HUMAN_POOL.poolLabel,
                subtitle = "给家人、朋友或陌生人一句心里话",
                onClick = { requestEnter(TreeholePoolType.HUMAN_POOL) },
            )
            Spacer(Modifier.height(14.dp))
            TreeholeEntryCard(
                icon = Icons.Outlined.Pets,
                title = TreeholePoolType.PET_POOL.poolLabel,
                subtitle = "给离开或远方的伙伴写信",
                onClick = { requestEnter(TreeholePoolType.PET_POOL) },
            )
        } else {
            TreeholeDisabledCard(onGoSettings = ::goToSettings)
        }

        Spacer(Modifier.weight(1f))
        ReferenceNote(
            text = if (treeholeEnabled) {
                "本应用不连接外部社区；信件、回应与举报均为本地状态，不对外发布。"
            } else {
                "心灵树洞已关闭，可在「我的」中重新开启。"
            },
        )
        Spacer(Modifier.height(16.dp))
    }

    val gateType = gatePool
    if (gateType != null) {
        val confirmAndEnter = {
            when (gateType) {
                TreeholePoolType.HUMAN_POOL -> humanAcknowledged = true
                TreeholePoolType.PET_POOL -> petAcknowledged = true
            }
            gatePool = null
            openPool(gateType)
        }
        TreeholeGuestGateDialog(
            onConfirm = confirmAndEnter,
            onDismiss = { gatePool = null },
        )
    }
}

/** 心灵树洞总开关关闭时的占位卡片（含前往设置的入口）。 */
@Composable
private fun TreeholeDisabledCard(onGoSettings: () -> Unit) {
    val shape = RoundedCornerShape(AppDimensions.CardRadius)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceCard)
            .padding(AppDimensions.CardPadding),
    ) {
        Text(
            text = "心灵树洞当前已关闭",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "如需使用树洞，可前往「我的」中的树洞设置重新开启。",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onGoSettings,
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimensions.MinimumTouchTarget),
        ) {
            Text("前往设置开启")
        }
    }
}

@Composable
private fun TreeholeEntryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(AppDimensions.CardRadius)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceCard)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = "进入$title",
                onClick = onClick,
            )
            .padding(AppDimensions.CardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(QingLanGreenSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** 游客暂不可发布确认框（本地身份说明，非账号系统）。 */
@Composable
private fun TreeholeGuestGateDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "游客暂不可使用树洞发布",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Text(
                text = "树洞面向注册用户开放发布。匿名拾信与阅读不受影响。继续即代表你已了解本机使用说明。",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(
                    text = "我已了解，继续",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "取消",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}
