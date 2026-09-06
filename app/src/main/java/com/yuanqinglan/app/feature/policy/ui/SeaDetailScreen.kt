/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.policy.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.QingLanGreenDark
import com.yuanqinglan.app.core.designsystem.QingLanGreenSoft
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.EmptyState
import com.yuanqinglan.app.core.ui.ErrorRetry
import com.yuanqinglan.app.core.ui.LoadingState
import com.yuanqinglan.app.core.ui.NoticeBanner
import com.yuanqinglan.app.core.ui.NoticeTone
import com.yuanqinglan.app.data.local.AppContainer
import com.yuanqinglan.app.feature.policy.SeaDetailViewModel
import com.yuanqinglan.app.feature.policy.data.AssetPolicyCatalogSource
import com.yuanqinglan.app.feature.policy.model.SeaGuide

/**
 * 公益海葬指引页：项目说明、一般流程、报名方式与注意事项。
 * 页面仅介绍公益海葬项目本身，不提供、不引导任何内河撒江类服务入口。
 */
@Composable
fun SeaDetailRoute(onBack: () -> Unit) {
    val vm: SeaDetailViewModel = viewModel {
        SeaDetailViewModel(source = AssetPolicyCatalogSource(AppContainer.demoAssets))
    }
    val guideState by vm.guideState.collectAsStateWithLifecycle()

    AppScaffold(title = "公益海葬指引", onBack = onBack) {
        when (val state = guideState) {
            DemoState.Loading -> LoadingState()
            is DemoState.Error -> Box(modifier = Modifier.fillMaxSize()) {
                ErrorRetry(message = state.message, onRetry = vm::reload)
            }
            DemoState.Empty -> EmptyState(
                title = "暂无内容",
                description = "指引暂时不可用，请返回重试。",
                actionLabel = "返回",
                onAction = onBack,
            )
            is DemoState.Success -> SeaGuideContent(guide = state.value)
        }
    }
}

@Composable
private fun SeaGuideContent(guide: SeaGuide) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = 8.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = guide.title,
                style = MaterialTheme.typography.headlineMedium,
                color = QingLanGreenDark,
            )
        }
        guide.introParagraphs.forEach { paragraph ->
            item {
                Text(
                    text = paragraph,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
        if (guide.flowSteps.isNotEmpty()) {
            item {
                Text(
                    text = guide.flowTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )
            }
            itemsIndexed(guide.flowSteps) { index, step ->
                Card(
                    shape = RoundedCornerShape(AppDimensions.CardRadius),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(AppDimensions.CardPadding),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = QingLanGreenSoft,
                            modifier = Modifier.size(28.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = (index + 1).toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = QingLanGreenDark,
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = step.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = step.detail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(AppDimensions.CardRadius),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
                    Text(
                        text = guide.applyTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = guide.applyParagraph,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(10.dp))
                    NoticeBanner(
                        text = "本页仅提供指引信息，不在线收集报名信息；登记一律以组织单位官方渠道为准。",
                        tone = NoticeTone.INFO,
                    )
                }
            }
        }
        if (guide.notices.isNotEmpty()) {
            item {
                Text(
                    text = "注意事项",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )
            }
            guide.notices.forEach { notice ->
                item {
                    NoticeBanner(
                        text = notice,
                        tone = NoticeTone.WARNING,
                    )
                }
            }
        }
        if (guide.complianceNote.isNotBlank()) {
            item {
                NoticeBanner(
                    text = guide.complianceNote,
                    tone = NoticeTone.COMPLIANCE,
                )
            }
        }
    }
}
