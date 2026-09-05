/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.home.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.QingLanGreen
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
import com.yuanqinglan.app.core.ui.ReferenceNote
import com.yuanqinglan.app.data.local.AppContainer
import com.yuanqinglan.app.feature.home.LifeEdViewModel
import com.yuanqinglan.app.feature.home.data.AssetHomeCatalogSource
import com.yuanqinglan.app.feature.home.model.LifeEdCourse

/** 生命教育列表页：课程卡片点击展开/收起详情，支持加载/空/失败/重试。 */
@Composable
fun LifeEdRoute(onBack: () -> Unit) {
    val vm: LifeEdViewModel = viewModel {
        LifeEdViewModel(source = AssetHomeCatalogSource(AppContainer.demoAssets))
    }
    val coursesState by vm.coursesState.collectAsStateWithLifecycle()
    val expandedId by vm.expandedId.collectAsStateWithLifecycle()

    AppScaffold(title = "生命教育", onBack = onBack) {
        when (val state = coursesState) {
            DemoState.Loading -> LoadingState()
            is DemoState.Error -> Box(modifier = Modifier.fillMaxSize()) {
                ErrorRetry(message = state.message, onRetry = vm::reload)
            }
            DemoState.Empty -> EmptyState(
                title = "生命教育内容整理中",
                description = "新的课程与主题会在这里发布。",
            )
            is DemoState.Success -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = 6.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        text = "从认识生命到从容告别，家庭需要的知识我们慢慢讲。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
                items(state.value, key = { it.id }) { course ->
                    LifeEdCourseCard(
                        course = course,
                        expanded = expandedId == course.id,
                        onToggle = { vm.toggleExpanded(course.id) },
                    )
                }
                item {
                    Spacer(Modifier.height(6.dp))
                    ReferenceNote(
                        text = "页面内容为知识介绍，仅供了解参考；具体课程与线下活动安排以组织单位公布为准。",
                    )
                }
            }
        }
    }
}

@Composable
private fun LifeEdCourseCard(
    course: LifeEdCourse,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = if (expanded) "收起${course.title}" else "展开${course.title}", onClick = onToggle),
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
            val res = HomeVisuals.lifeEdImage(course.imageKey)
            if (res != null) {
                Image(
                    painter = painterResource(res),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(AppDimensions.CompactRadius)),
                )
                Spacer(Modifier.height(12.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(AppDimensions.CompactRadius),
                            color = QingLanGreenSoft,
                        ) {
                            Text(
                                text = course.category,
                                style = MaterialTheme.typography.labelMedium,
                                color = QingLanGreenDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                        if (course.level != null) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = course.level,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = course.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "约 ${course.durationMinutes} 分钟",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
                Icon(
                    imageVector = if (expanded) {
                        Icons.Filled.KeyboardArrowUp
                    } else {
                        Icons.Filled.KeyboardArrowDown
                    },
                    contentDescription = if (expanded) "收起详情" else "展开详情",
                    tint = TextSecondary,
                )
            }
            if (expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .height(1.dp)
                        .background(Color(0xFFEFE8D7)),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = course.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                )
                course.paragraphs.forEach { paragraph ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = paragraph,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}
