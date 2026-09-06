/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.home.ui

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import com.yuanqinglan.app.R
import com.yuanqinglan.app.core.designsystem.AppDimensions
import kotlinx.coroutines.delay

/** 首页轮播单页：图片为氛围背景，文案由 Compose 叠加。 */
data class HomeCarouselPage(
    val title: String,
    val subtitle: String,
    val imageRes: Int,
    val onClick: () -> Unit,
)

/**
 * 首页 5 张轮播 Banner（2:1、Crop）。支持自动轮播（约 5 秒一页）与手动滑动，
 * 页面圆点指示当前页；整卡可点击进入对应入口。
 */
@Composable
fun HomeCarousel(
    pages: List<HomeCarouselPage>,
    modifier: Modifier = Modifier,
) {
    if (pages.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { pages.size })

    // 自动轮播：用户正在手动滑动时不推进。
    LaunchedEffect(pagerState) {
        while (true) {
            delay(5_000)
            if (!pagerState.isScrollInProgress) {
                val next = (pagerState.currentPage + 1) % pages.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { pageIndex ->
            val page = pages[pageIndex]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
                    .clip(RoundedCornerShape(AppDimensions.SceneRadius))
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "查看${page.title}",
                        onClick = page.onClick,
                    ),
            ) {
                Image(
                    painter = painterResource(page.imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                0f to Color(0x66000000),
                                0.55f to Color.Transparent,
                            ),
                        ),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = page.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = page.subtitle,
                        color = Color.White.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(pages.size) { index ->
                val selected = pagerState.currentPage == index
                val width by animateDpAsState(if (selected) 14.dp else 6.dp, label = "dotWidth")
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else Color(0xFFB9C4B5),
                        ),
                )
            }
        }
    }
}

/** 首页轮播页配置：5 张（重庆山城/纪念林地/花海/草坪/宠物草地）。 */
internal fun carouselPages(
    onOpenCityNews: () -> Unit,
    onOpenTree: () -> Unit,
    onOpenFlower: () -> Unit,
    onOpenLawn: () -> Unit,
    onOpenPet: () -> Unit,
): List<HomeCarouselPage> = listOf(
    HomeCarouselPage("山水有归处", "在熟悉的山城里，了解温和的告别方式", R.drawable.home_carousel_chongqing, onOpenCityNews),
    HomeCarouselPage("林间寄思念", "树葬：让纪念随树木一同生长", R.drawable.home_carousel_tree, onOpenTree),
    HomeCarouselPage("花开四季", "花葬：思念落在公共花园里", R.drawable.home_carousel_flower, onOpenFlower),
    HomeCarouselPage("草坪之上", "草坪葬：开阔宁静的追思空间", R.drawable.home_carousel_lawn, onOpenLawn),
    HomeCarouselPage("相伴自然", "宠物纪念：独立园区与林地", R.drawable.home_carousel_pet, onOpenPet),
)
