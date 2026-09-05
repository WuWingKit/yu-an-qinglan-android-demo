/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.policy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.LawnSoft
import com.yuanqinglan.app.core.designsystem.QingLanGreen
import com.yuanqinglan.app.core.designsystem.QingLanGreenDark
import com.yuanqinglan.app.core.designsystem.QingLanGreenSoft
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.designsystem.ToolBlueSoft
import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.EmptyState
import com.yuanqinglan.app.core.ui.ErrorRetry
import com.yuanqinglan.app.core.ui.FormTextField
import com.yuanqinglan.app.core.ui.LoadingState
import com.yuanqinglan.app.core.ui.NoticeBanner
import com.yuanqinglan.app.core.ui.NoticeTone
import com.yuanqinglan.app.data.local.AppContainer
import com.yuanqinglan.app.feature.policy.PolicyPageMode
import com.yuanqinglan.app.feature.policy.PolicyViewModel
import com.yuanqinglan.app.feature.policy.data.AssetPolicyCatalogSource
import com.yuanqinglan.app.feature.policy.model.County
import com.yuanqinglan.app.feature.policy.model.PolicyArticle
import com.yuanqinglan.app.feature.policy.model.PolicyLevel

/** 政策链路统一合规句。 */
internal const val POLICY_COMPLIANCE_SENTENCE =
    "相关信息仅供参考，具体政策、费用与办理结果以主管机构和服务机构最终公布为准。"

/**
 * 政策补贴页：政策列表 + 顶部"区县查询"入口（页内切换到 38 区县列表，
 * 支持搜索），另有公益海葬指引入口。
 */
@Composable
fun PolicyRoute(
    onBack: () -> Unit,
    onOpenCounty: (String) -> Unit,
    onOpenPrecheck: (String) -> Unit,
    onOpenSeaDetail: () -> Unit,
) {
    val vm: PolicyViewModel = viewModel {
        PolicyViewModel(source = AssetPolicyCatalogSource(AppContainer.demoAssets))
    }
    val policiesState by vm.policiesState.collectAsStateWithLifecycle()
    val countiesState by vm.countiesState.collectAsStateWithLifecycle()
    val mode by vm.mode.collectAsStateWithLifecycle()
    val query by vm.countyQuery.collectAsStateWithLifecycle()

    AppScaffold(
        title = if (mode == PolicyPageMode.POLICIES) "政策补贴" else "区县查询",
        onBack = {
            if (mode == PolicyPageMode.COUNTIES) vm.backToPolicies() else onBack()
        },
    ) {
        if (mode == PolicyPageMode.POLICIES) {
            PolicyListPane(
                state = policiesState,
                onRetry = vm::reloadPolicies,
                onOpenCounties = vm::openCounties,
                onOpenSeaDetail = onOpenSeaDetail,
            )
        } else {
            CountiesPane(
                state = countiesState,
                query = query,
                filtered = vm.filteredCounties(),
                onQueryChange = vm::updateCountyQuery,
                onOpenCounty = onOpenCounty,
                onRetry = vm::reloadCounties,
            )
        }
    }
}

@Composable
private fun PolicyListPane(
    state: DemoState<List<PolicyArticle>>,
    onRetry: () -> Unit,
    onOpenCounties: () -> Unit,
    onOpenSeaDetail: () -> Unit,
) {
    when (state) {
        DemoState.Loading -> LoadingState()
        is DemoState.Error -> Box(modifier = Modifier.fillMaxSize()) {
            ErrorRetry(message = state.message, onRetry = onRetry)
        }
        DemoState.Empty -> EmptyState(
            title = "政策信息整理中",
            description = "补贴与办理提示会在这里持续更新。",
        )
        is DemoState.Success -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 6.dp,
                bottom = 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                NoticeBanner(
                    text = "本页政策内容为信息参考，办理请以主管机构当年公布为准。",
                    tone = NoticeTone.COMPLIANCE,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    EntryTile(
                        title = "区县查询",
                        subtitle = "38 个区县 · 补贴与办理摘要",
                        icon = Icons.Outlined.Place,
                        container = ToolBlueSoft,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenCounties,
                    )
                    EntryTile(
                        title = "公益海葬指引",
                        subtitle = "流程 · 报名方式 · 注意事项",
                        icon = Icons.Outlined.Waves,
                        container = LawnSoft,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenSeaDetail,
                    )
                }
            }
            item {
                Text(
                    text = "政策条目",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )
            }
            items(state.value, key = { it.id }) { article ->
                PolicyArticleCard(article = article)
            }
            item {
                Spacer(Modifier.height(4.dp))
                NoticeBanner(text = POLICY_COMPLIANCE_SENTENCE, tone = NoticeTone.COMPLIANCE)
            }
        }
    }
}

@Composable
private fun EntryTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    container: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable(role = Role.Button, onClickLabel = "进入$title", onClick = onClick),
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppDimensions.CardPadding),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(container, RoundedCornerShape(AppDimensions.CompactRadius)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = QingLanGreenDark,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PolicyArticleCard(article: PolicyArticle) {
    Card(
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LevelChip(level = article.level)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = article.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun LevelChip(level: PolicyLevel) {
    val container = when (level) {
        PolicyLevel.NATIONAL -> LawnSoft
        PolicyLevel.CITY -> QingLanGreenSoft
        PolicyLevel.TIP -> ToolBlueSoft
        PolicyLevel.NOTICE -> ToolBlueSoft
    }
    Surface(shape = RoundedCornerShape(AppDimensions.CompactRadius), color = container) {
        Text(
            text = level.label,
            style = MaterialTheme.typography.labelMedium,
            color = QingLanGreenDark,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun CountiesPane(
    state: DemoState<List<County>>,
    query: String,
    filtered: List<County>,
    onQueryChange: (String) -> Unit,
    onOpenCounty: (String) -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        DemoState.Loading -> LoadingState()
        is DemoState.Error -> Box(modifier = Modifier.fillMaxSize()) {
            ErrorRetry(message = state.message, onRetry = onRetry)
        }
        DemoState.Empty -> EmptyState(
            title = "区县信息整理中",
            description = "38 个区县的补贴与办理摘要会在这里更新。",
        )
        is DemoState.Success -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 6.dp,
                bottom = 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FormTextField(
                    label = "搜索区县（名称或片区）",
                    value = query,
                    onValueChange = onQueryChange,
                    supportingText = "共 ${filtered.size} 个匹配结果",
                )
            }
            if (filtered.isEmpty()) {
                item {
                    EmptyState(
                        title = "未找到相关区县",
                        description = "换个关键词试试，例如：渝中、万州、渝东北。",
                        actionLabel = "清除搜索",
                        onAction = { onQueryChange("") },
                    )
                }
            } else {
                items(filtered, key = { it.id }) { county ->
                    CountyRow(county = county, onClick = { onOpenCounty(county.id) })
                }
            }
        }
    }
}

@Composable
private fun CountyRow(county: County, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = "查看${county.name}", onClick = onClick),
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
    ) {
        Row(
            modifier = Modifier.padding(AppDimensions.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Apartment,
                contentDescription = null,
                tint = QingLanGreen,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = county.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(AppDimensions.CompactRadius),
                        color = QingLanGreenSoft,
                    ) {
                        Text(
                            text = county.zone,
                            style = MaterialTheme.typography.labelMedium,
                            color = QingLanGreenDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = county.policySummary,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
