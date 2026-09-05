/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.home.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.ConfirmDangerDialog
import com.yuanqinglan.app.core.ui.EmptyState
import com.yuanqinglan.app.core.ui.ErrorRetry
import com.yuanqinglan.app.core.ui.InfoRow
import com.yuanqinglan.app.core.ui.LoadingState
import com.yuanqinglan.app.core.ui.NoticeBanner
import com.yuanqinglan.app.core.ui.NoticeTone
import com.yuanqinglan.app.core.ui.PrimaryButton
import com.yuanqinglan.app.core.ui.SecondaryButton
import com.yuanqinglan.app.data.local.AppContainer
import com.yuanqinglan.app.feature.home.ActivitiesViewModel
import com.yuanqinglan.app.feature.home.data.AssetHomeCatalogSource
import com.yuanqinglan.app.feature.home.model.ActivityEvent
import com.yuanqinglan.app.feature.home.model.ActivityStatus

/** 公益活动页：列表 ⇄ 详情（页内切换），详情支持本地报名/取消报名（可重复、取消需确认）。 */
@Composable
fun ActivitiesRoute(onBack: () -> Unit) {
    val vm: ActivitiesViewModel = viewModel {
        ActivitiesViewModel(source = AssetHomeCatalogSource(AppContainer.demoAssets))
    }
    val listState by vm.listState.collectAsStateWithLifecycle()
    val detailId by vm.detailId.collectAsStateWithLifecycle()
    val signedIds by vm.signedIds.collectAsStateWithLifecycle()
    val pending by vm.pendingSignup.collectAsStateWithLifecycle()

    val detailEvent: ActivityEvent? = (listState as? DemoState.Success)?.value?.firstOrNull { it.id == detailId }

    AppScaffold(
        title = if (detailEvent != null) "活动详情" else "公益活动",
        onBack = {
            if (detailEvent != null) vm.closeDetail() else onBack()
        },
    ) {
        when (val state = listState) {
            DemoState.Loading -> LoadingState()
            is DemoState.Error -> Box(modifier = Modifier.fillMaxSize()) {
                ErrorRetry(message = state.message, onRetry = vm::reload)
            }
            DemoState.Empty -> EmptyState(
                title = "暂无活动安排",
                description = "新的集体纪念与公益活动会在这里发布。",
            )
            is DemoState.Success -> {
                val event = detailEvent
                if (event != null) {
                    ActivityDetailPane(
                        event = event,
                        signed = event.id in signedIds,
                        onRequestSignup = { vm.requestSignup(event.id) },
                        onBackToList = vm::closeDetail,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            top = 6.dp,
                            bottom = 24.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            NoticeBanner(
                                text = "报名为本地意向登记，仅保存在本机，不向主办方或机构提交；线下参与以主办方确认与现场签到为准。",
                                tone = NoticeTone.INFO,
                            )
                        }
                        items(state.value, key = { it.id }) { event ->
                            ActivityListCard(event = event, onClick = { vm.openDetail(event.id) })
                        }
                    }
                }
            }
        }
    }

    // 取消报名二次确认。
    pending?.let { request ->
        if (!request.wantSign) {
            ConfirmDangerDialog(
                title = "取消报名",
                message = "确定取消本次活动的报名登记吗？登记仅保存在本机，取消后可随时重新报名。",
                confirmLabel = "确认取消",
                onConfirm = vm::confirmPending,
                onDismiss = vm::dismissPending,
            )
        }
    }
}

@Composable
private fun ActivityListCard(event: ActivityEvent, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = "查看${event.title}", onClick = onClick),
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                StatusChip(status = event.status)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = event.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${event.time} · ${event.location}",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ActivityHero(event.imageKey)
        }
    }
}

/** 活动卡头图：图片为氛围背景，未收录图片键时留空不占位。 */
@Composable
private fun ActivityHero(imageKey: String?) {
    val res = HomeVisuals.activityImage(imageKey)
    if (res != null) {
        Spacer(Modifier.height(10.dp))
        Image(
            painter = painterResource(res),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(AppDimensions.CompactRadius)),
        )
    }
}

@Composable
private fun ActivityDetailPane(
    event: ActivityEvent,
    signed: Boolean,
    onRequestSignup: () -> Unit,
    onBackToList: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = 6.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = event.title,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(status = event.status)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = event.type,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(AppDimensions.CardRadius),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            ) {
                Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
                    InfoRow(label = "时间", value = event.time)
                    InfoRow(label = "地点", value = event.location)
                    InfoRow(label = "主办方", value = event.organizer)
                    InfoRow(label = "状态", value = event.status.label)
                }
            }
        }
        item {
            Text(
                text = event.summary,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
            )
        }
        items(event.detail) { paragraph ->
            Text(
                text = paragraph,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
        item {
            when (event.status) {
                ActivityStatus.ENDED -> NoticeBanner(
                    text = "本场活动已结束，报名入口已关闭；可关注后续场次。",
                    tone = NoticeTone.WARNING,
                )
                ActivityStatus.UPCOMING, ActivityStatus.SIGNING -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (signed) {
                            NoticeBanner(
                                text = "已在本机登记报名，可点击下方按钮取消后重新报名。",
                                tone = NoticeTone.INFO,
                            )
                            SecondaryButton(text = "取消报名登记", onClick = onRequestSignup)
                        } else {
                            PrimaryButton(text = "报名 / 预约", onClick = onRequestSignup)
                        }
                    }
                }
            }
        }
        item {
            NoticeBanner(
                text = "报名登记仅保存在本机，用于意向记录；名额、地点与时间调整以主办方最终通知为准。",
                tone = NoticeTone.COMPLIANCE,
            )
        }
        item {
            TextButtonBack(onClick = onBackToList)
        }
    }
}

@Composable
private fun TextButtonBack(onClick: () -> Unit) {
    Text(
        text = "返回活动列表",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clickable(role = Role.Button, onClickLabel = "返回活动列表", onClick = onClick)
            .padding(vertical = 6.dp),
    )
}
