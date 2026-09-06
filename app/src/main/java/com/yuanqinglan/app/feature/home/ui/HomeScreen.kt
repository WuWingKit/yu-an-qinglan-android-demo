/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yuanqinglan.app.core.designsystem.AppBackground
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.QingLanGreen
import com.yuanqinglan.app.core.designsystem.QingLanGreenDark
import com.yuanqinglan.app.core.designsystem.QingLanGreenSoft
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.SurfaceSoft
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.core.ui.EmptyState
import com.yuanqinglan.app.core.ui.ErrorRetry
import com.yuanqinglan.app.core.ui.LoadingState
import com.yuanqinglan.app.core.ui.NoticeBanner
import com.yuanqinglan.app.core.ui.NoticeTone
import com.yuanqinglan.app.core.ui.ReferenceNote
import com.yuanqinglan.app.core.ui.SectionHeader
import com.yuanqinglan.app.data.local.AppContainer
import com.yuanqinglan.app.feature.home.HomeFeedViewModel
import com.yuanqinglan.app.feature.home.data.AssetHomeCatalogSource
import com.yuanqinglan.app.feature.home.model.ActivityEvent
import com.yuanqinglan.app.feature.home.model.ActivityStatus
import com.yuanqinglan.app.feature.home.model.LifeEdCourse
import com.yuanqinglan.app.feature.home.model.NewsArticle

/** 首页合规句（克制口径，不含"演示"等字样）。 */
internal const val HOME_COMPLIANCE_SENTENCE =
    "相关信息仅供参考，具体政策、费用与办理结果以主管机构和服务机构最终公布为准。"

/** 首页 Tab 根页面：品牌标题 + 老年模式开关、轮播、常用服务、生命教育、近期活动、资讯、政策入口。 */
@Composable
fun HomeRoute(
    onOpenBurial: () -> Unit,
    onOpenTree: () -> Unit,
    onOpenFlower: () -> Unit,
    onOpenLawn: () -> Unit,
    onOpenPet: () -> Unit,
    onOpenPolicy: () -> Unit,
    onOpenMemorial: () -> Unit,
    onOpenActivities: () -> Unit,
    onOpenLifeEd: () -> Unit,
    onOpenMatch: () -> Unit,
    onOpenNewsDetail: (String) -> Unit,
) {
    val vm: HomeFeedViewModel = viewModel {
        HomeFeedViewModel(
            source = AssetHomeCatalogSource(AppContainer.demoAssets),
            settings = AppContainer.settings,
        )
    }
    val elderMode by vm.elderMode.collectAsStateWithLifecycle()
    val newsState by vm.newsState.collectAsStateWithLifecycle()
    val lifeEdState by vm.lifeEdState.collectAsStateWithLifecycle()
    val activitiesState by vm.activitiesState.collectAsStateWithLifecycle()
    val featuredNews by vm.featuredNews.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        contentPadding = PaddingValues(
            start = AppDimensions.PageHorizontal,
            top = 10.dp,
            end = AppDimensions.PageHorizontal,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SectionSpacing),
    ) {
        item {
            HomeHeader(
                elderMode = elderMode,
                onElderModeChange = vm::setElderMode,
            )
        }
        item {
            HomeCarousel(
                pages = carouselPages(
                    onOpenCityNews = { onOpenNewsDetail("news-04") },
                    onOpenTree = onOpenTree,
                    onOpenFlower = onOpenFlower,
                    onOpenLawn = onOpenLawn,
                    onOpenPet = onOpenPet,
                ),
            )
        }
        item {
            SectionHeader(title = "常用服务")
        }
        item {
            ServiceGrid(
                onBurial = onOpenBurial,
                onPolicy = onOpenPolicy,
                onMemorial = onOpenMemorial,
                onActivities = onOpenActivities,
            )
        }
        item {
            SectionHeader(title = "生命教育", actionLabel = "查看全部", onAction = onOpenLifeEd)
        }
        item {
            LifeEdPreviewSection(
                state = lifeEdState,
                onOpenAll = onOpenLifeEd,
                onRetry = vm::reloadAll,
            )
        }
        item {
            SectionHeader(title = "近期活动")
        }
        item {
            ActivitiesPreviewSection(
                state = activitiesState,
                onOpenActivity = onOpenActivities,
                onRetry = vm::reloadAll,
            )
        }
        item {
            SectionHeader(title = "资讯", actionLabel = "换一换", onAction = vm::refreshNews)
        }
        item {
            NewsPreviewSection(
                state = newsState,
                featured = featuredNews,
                onOpen = onOpenNewsDetail,
                onRetry = vm::reloadAll,
                onRefresh = vm::refreshNews,
            )
        }
        item {
            PolicyEntryCard(onClick = onOpenPolicy)
        }
        item {
            MatchEntryCard(onClick = onOpenMatch)
        }
        item {
            NoticeBanner(
                text = HOME_COMPLIANCE_SENTENCE,
                tone = NoticeTone.COMPLIANCE,
            )
        }
        item {
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun HomeHeader(
    elderMode: Boolean,
    onElderModeChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "渝安青澜",
                style = MaterialTheme.typography.headlineMedium,
                color = QingLanGreenDark,
            )
            Text(
                text = "让告别回归自然，让思念有所安放",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics { contentDescription = "老年模式" },
        ) {
            Text(
                text = "老年模式",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.width(4.dp))
            Switch(
                checked = elderMode,
                onCheckedChange = onElderModeChange,
            )
        }
    }
}

@Composable
private fun ServiceGrid(
    onBurial: () -> Unit,
    onPolicy: () -> Unit,
    onMemorial: () -> Unit,
    onActivities: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ServiceEntryCard("生态葬式", "树葬 · 花葬 · 草坪葬", Icons.Outlined.Forest, Modifier.weight(1f), onBurial)
            ServiceEntryCard("政策预审", "补贴参考与区县查询", Icons.Outlined.Policy, Modifier.weight(1f), onPolicy)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ServiceEntryCard("云端追忆", "建立私人纪念空间", Icons.Outlined.FavoriteBorder, Modifier.weight(1f), onMemorial)
            ServiceEntryCard("公益活动", "集体纪念与生命教育", Icons.Outlined.Celebration, Modifier.weight(1f), onActivities)
        }
    }
}

@Composable
private fun ServiceEntryCard(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .height(104.dp)
            .clickable(role = Role.Button, onClickLabel = "进入$title", onClick = onClick),
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppDimensions.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(QingLanGreenSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = QingLanGreen,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LifeEdPreviewSection(
    state: DemoState<List<LifeEdCourse>>,
    onOpenAll: () -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        DemoState.Loading -> SectionProgressBox()
        is DemoState.Error -> SectionErrorRetry(message = state.message, onRetry = onRetry)
        DemoState.Empty -> EmptyState(title = "生命教育内容整理中", description = "敬请期待更多课程与主题。")
        is DemoState.Success -> {
            val courses = state.value.take(3)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(courses, key = { it.id }) { course ->
                    LifeEdMiniCard(course, onClick = onOpenAll)
                }
            }
        }
    }
}

@Composable
private fun LifeEdMiniCard(course: LifeEdCourse, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable(role = Role.Button, onClickLabel = "查看${course.title}", onClick = onClick),
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
            Text(
                text = course.category,
                style = MaterialTheme.typography.labelMedium,
                color = QingLanGreen,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = course.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "约 ${course.durationMinutes} 分钟",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = course.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ActivitiesPreviewSection(
    state: DemoState<List<ActivityEvent>>,
    onOpenActivity: () -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        DemoState.Loading -> SectionProgressBox()
        is DemoState.Error -> SectionErrorRetry(message = state.message, onRetry = onRetry)
        DemoState.Empty -> EmptyState(title = "暂无活动安排", description = "新的集体纪念与公益活动会在这里发布。")
        is DemoState.Success -> {
            val events = state.value
                .filter { it.status != ActivityStatus.ENDED }
                .ifEmpty { state.value }
                .take(2)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                events.forEach { event ->
                    ActivityMiniCard(event, onClick = onOpenActivity)
                }
            }
        }
    }
}

@Composable
private fun ActivityMiniCard(event: ActivityEvent, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = "查看${event.title}", onClick = onClick),
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
    ) {
        Row(
            modifier = Modifier.padding(AppDimensions.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${event.time} · ${event.location}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            StatusChip(status = event.status)
        }
    }
}

@Composable
internal fun StatusChip(status: ActivityStatus) {
    val container = when (status) {
        ActivityStatus.SIGNING -> QingLanGreenSoft
        ActivityStatus.UPCOMING -> SurfaceSoft
        ActivityStatus.ENDED -> SurfaceSoft
    }
    val content = when (status) {
        ActivityStatus.SIGNING -> QingLanGreenDark
        ActivityStatus.UPCOMING, ActivityStatus.ENDED -> TextSecondary
    }
    Surface(
        shape = RoundedCornerShape(AppDimensions.CompactRadius),
        color = container,
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun NewsPreviewSection(
    state: DemoState<List<NewsArticle>>,
    featured: List<NewsArticle>,
    onOpen: (String) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
) {
    when (state) {
        DemoState.Loading -> SectionProgressBox()
        is DemoState.Error -> SectionErrorRetry(message = state.message, onRetry = onRetry)
        DemoState.Empty -> EmptyState(title = "暂无资讯", description = "新的科普与资讯内容会在这里更新。")
        is DemoState.Success -> {
            if (featured.isEmpty()) {
                SectionErrorRetry(message = "资讯加载失败，请稍后重试。", onRetry = onRefresh)
                return
            }
            Card(
                shape = RoundedCornerShape(AppDimensions.CardRadius),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            ) {
                Column {
                    featured.forEachIndexed { index, article ->
                        NewsRow(article = article, onClick = { onOpen(article.id) })
                        if (index != featured.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0xFFEFE8D7)),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsRow(article: NewsArticle, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = "阅读${article.title}", onClick = onClick)
            .padding(horizontal = AppDimensions.CardPadding, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${article.source} · ${article.publishTime}",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            if (article.summary.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = article.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier
                .padding(start = 8.dp, top = 4.dp)
                .size(20.dp),
        )
    }
}

@Composable
private fun PolicyEntryCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = "进入政策专区", onClick = onClick),
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
    ) {
        Row(
            modifier = Modifier.padding(AppDimensions.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(QingLanGreenSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Policy,
                    contentDescription = null,
                    tint = QingLanGreen,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("政策与区县预审", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    text = "补贴参考、38 区县查询与本地规则预审",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
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

@Composable
private fun SectionProgressBox() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        contentAlignment = Alignment.Center,
    ) {
        LoadingState()
    }
}

@Composable
private fun SectionErrorRetry(message: String, onRetry: () -> Unit) {
    ErrorRetry(message = message, onRetry = onRetry)
}

@Composable
private fun MatchEntryCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = "进入智能匹配", onClick = onClick),
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        color = QingLanGreenSoft,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppDimensions.CardPadding, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "选择困难？先做一次智能匹配",
                    style = MaterialTheme.typography.titleMedium,
                    color = QingLanGreenDark,
                )
                Text(
                    text = "回答 4 个小问题，按本地规则给出安葬方向建议",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = QingLanGreenDark,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** 首页底部信息参考（供顶部/底部合规句复用）。 */
@Composable
internal fun HomeReferenceNote() {
    ReferenceNote(text = HOME_COMPLIANCE_SENTENCE)
}
