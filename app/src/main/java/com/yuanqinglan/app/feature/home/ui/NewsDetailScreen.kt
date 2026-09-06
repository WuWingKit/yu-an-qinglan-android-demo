/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.home.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.QingLanGreenDark
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
import com.yuanqinglan.app.feature.home.NewsDetailViewModel
import com.yuanqinglan.app.feature.home.data.AssetHomeCatalogSource
import com.yuanqinglan.app.feature.home.model.NewsArticle

/** 资讯详情页：标题/来源/发布时间/正文多段（可配图）；支持加载/失败/重试。 */
@Composable
fun NewsDetailRoute(newsId: String, onBack: () -> Unit) {
    val vm: NewsDetailViewModel = viewModel(key = "news-detail-$newsId") {
        NewsDetailViewModel(source = AssetHomeCatalogSource(AppContainer.demoAssets), newsId = newsId)
    }
    val articleState by vm.articleState.collectAsStateWithLifecycle()

    AppScaffold(title = "资讯详情", onBack = onBack) {
        when (val state = articleState) {
            DemoState.Loading -> LoadingState()
            is DemoState.Error -> Box(modifier = Modifier.fillMaxSize()) {
                ErrorRetry(message = state.message, onRetry = { vm.load(newsId) })
            }
            DemoState.Empty -> EmptyState(
                title = "暂无内容",
                description = "该资讯暂时不可用，请返回重试。",
                actionLabel = "返回",
                onAction = onBack,
            )
            is DemoState.Success -> NewsArticleContent(article = state.value)
        }
    }
}

@Composable
private fun NewsArticleContent(article: NewsArticle) {
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
                text = article.title,
                style = MaterialTheme.typography.headlineMedium,
                color = QingLanGreenDark,
            )
        }
        item {
            Column {
                Text(
                    text = "${article.author} · ${article.source}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                Text(
                    text = "发布时间：${article.publishTime}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
        }
        val imageRes = HomeVisuals.newsImage(article.imageKey)
        if (imageRes != null) {
            item {
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = article.summary.ifBlank { article.title },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .background(SurfaceCard, RoundedCornerShape(AppDimensions.CardRadius)),
                )
            }
        }
        item {
            Text(
                text = article.summary,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
        article.paragraphs.forEach { paragraph ->
            item {
                Text(
                    text = paragraph,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                )
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            ReferenceNote(text = HOME_COMPLIANCE_SENTENCE)
        }
    }
}
