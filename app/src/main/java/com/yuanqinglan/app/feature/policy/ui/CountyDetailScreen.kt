/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.policy.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.yuanqinglan.app.core.ui.PrimaryButton
import com.yuanqinglan.app.data.local.AppContainer
import com.yuanqinglan.app.feature.policy.CountyDetailViewModel
import com.yuanqinglan.app.feature.policy.data.AssetPolicyCatalogSource
import com.yuanqinglan.app.feature.policy.logic.CountyIndex

/**
 * 区县详情页：名称（标题经 [CountyIndex.cleanTitle] 清除残留 `>` 等脏字符）、
 * 简介、政策摘要与办理提示；提供"进入预审"按钮，携带区县参数跳转 presult。
 */
@Composable
fun CountyDetailRoute(countyId: String, onBack: () -> Unit, onStartPrecheck: (String) -> Unit) {
    val vm: CountyDetailViewModel = viewModel(key = "county-$countyId") {
        CountyDetailViewModel(
            source = AssetPolicyCatalogSource(AppContainer.demoAssets),
            countyId = countyId,
        )
    }
    val countyState by vm.countyState.collectAsStateWithLifecycle()

    AppScaffold(title = "区县详情", onBack = onBack) {
        when (val state = countyState) {
            DemoState.Loading -> LoadingState()
            is DemoState.Error -> Box(modifier = Modifier.fillMaxSize()) {
                ErrorRetry(message = state.message, onRetry = { vm.load(countyId) })
            }
            DemoState.Empty -> EmptyState(
                title = "暂无内容",
                description = "未找到该区县信息，请返回重试。",
                actionLabel = "返回",
                onAction = onBack,
            )
            is DemoState.Success -> {
                val county = state.value
                val cleanName = CountyIndex.cleanTitle(county.name)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = 8.dp,
                        bottom = 28.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Column {
                            Text(
                                text = cleanName,
                                style = MaterialTheme.typography.headlineMedium,
                                color = QingLanGreenDark,
                            )
                            Spacer(Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(AppDimensions.CompactRadius),
                                color = QingLanGreenSoft,
                            ) {
                                Text(
                                    text = county.zone,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = QingLanGreenDark,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                    item {
                        InfoSection(title = "区县简介", body = county.brief)
                    }
                    item {
                        InfoSection(title = "政策摘要", body = county.policySummary)
                    }
                    item {
                        InfoSection(title = "办理提示", body = county.processTips)
                    }
                    item {
                        NoticeBanner(
                            text = POLICY_COMPLIANCE_SENTENCE,
                            tone = NoticeTone.COMPLIANCE,
                        )
                    }
                    item {
                        PrimaryButton(
                            text = "进入政策预审",
                            onClick = { onStartPrecheck(county.id) },
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    item {
                        Text(
                            text = "预审为本地参考测算，不代替任何政务办理。",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoSection(title: String, body: String) {
    Card(
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}
