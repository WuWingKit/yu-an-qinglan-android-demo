/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.home.ui

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yuanqinglan.app.R
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.ElderFontScale
import kotlin.math.floor
import kotlinx.coroutines.delay

/**
 * 轮播页深色遮罩配置（每页可配）。
 *
 * 遮罩以横向渐变叠在图片左侧，用于保证标题/副标题对比度；
 * 结束位置 [endFraction] 越靠左，遮罩面积越小，左右画面细节越可见。
 * 参数均要求落在 0..1，且透明度沿横向单调递减（结束位置透明）。
 *
 * 取值规则见 [carouselScrimFor]：按素材文字区实测亮度推导，而非套用参考图固定参数。
 */
data class HomeCarouselScrim(
    val startFraction: Float,
    val startAlpha: Float,
    val endFraction: Float,
    val endAlpha: Float,
) {
    init {
        require(startFraction in 0f..1f) { "遮罩起始位置需在 0..1：$startFraction" }
        require(endFraction in 0f..1f) { "遮罩结束位置需在 0..1：$endFraction" }
        require(startFraction < endFraction) { "遮罩起始位置必须小于结束位置：$startFraction / $endFraction" }
        require(startAlpha in 0f..1f) { "遮罩起始透明度需在 0..1：$startAlpha" }
        require(endAlpha in 0f..1f) { "遮罩结束透明度需在 0..1：$endAlpha" }
        require(endAlpha <= startAlpha) { "遮罩透明度应沿横向单调递减：$startAlpha -> $endAlpha" }
    }

    /** 由本配置构造横向渐变遮罩（左深右透）。 */
    fun brush(): Brush = Brush.horizontalGradient(
        startFraction to Color.Black.copy(alpha = startAlpha),
        endFraction to Color.Black.copy(alpha = endAlpha),
    )
}

/** 文字区目标等效亮度（0-255）：素材亮度经遮罩后应不高于该值，保证白色文字可读。 */
internal const val CAROUSEL_TEXT_ZONE_TARGET_LUMA = 105f

/** 轮播遮罩参数的允许范围（安全边界，供测试与后续校验复用）。 */
internal object CarouselScrimLimits {
    const val MIN_START_ALPHA = 0.18f
    const val MAX_START_ALPHA = 0.60f
    const val MIN_END_FRACTION = 0.30f
    const val MAX_END_FRACTION = 0.50f
}

/** 统一结束位置：遮罩在约 42% 宽度处淡出，右侧画面完整还原。 */
internal const val CAROUSEL_SCRIM_END_FRACTION = 0.42f

/**
 * 按素材文字区实测亮度推导每页遮罩（统一规则，非参考图固定参数）：
 * 遮罩起始透明度 = clamp(1 - 目标亮度 / 素材实测亮度, 下限, 上限)。
 * 素材越亮遮罩越重、越暗遮罩越轻，始终把文字区等效亮度压到安全区间。
 */
internal fun carouselScrimFor(textZoneLuma: Float): HomeCarouselScrim {
    require(textZoneLuma > 0f) { "素材文字区亮度必须为正：$textZoneLuma" }
    val needed = 1f - CAROUSEL_TEXT_ZONE_TARGET_LUMA / textZoneLuma
    return HomeCarouselScrim(
        startFraction = 0f,
        startAlpha = needed.coerceIn(CarouselScrimLimits.MIN_START_ALPHA, CarouselScrimLimits.MAX_START_ALPHA),
        endFraction = CAROUSEL_SCRIM_END_FRACTION,
        endAlpha = 0f,
    )
}

/** 首页轮播单页：图片为氛围背景，文案由 Compose 叠加，[scrim] 为每页可配的遮罩。 */
data class HomeCarouselPage(
    val title: String,
    val subtitle: String,
    val imageRes: Int,
    val scrim: HomeCarouselScrim,
    val onClick: () -> Unit,
)

/** 轮播文案左右内边距（与 [HomeCarousel] 中 Column 的 padding 保持一致，供溢出校验复用）。 */
internal const val CAROUSEL_TEXT_H_PADDING_DP = 16f

/** 轮播标题/副标题设计字号（sp，与 designsystem 的 titleLarge/bodyMedium 对应）。 */
internal const val CAROUSEL_TITLE_FONT_SP = 17f
internal const val CAROUSEL_SUBTITLE_FONT_SP = 13f

/**
 * 估算一行可容纳的 CJK 全角字符数：全角字符宽度 ≈ 字号。
 * 画布宽度、字号与老年缩放均为参数，避免硬编码挤压。
 */
internal fun cjkCharsPerLine(availableWidthDp: Float, fontSizeSp: Float, elderScale: Float = 1f): Int {
    val effectiveSize = fontSizeSp * elderScale
    require(effectiveSize > 0f) { "有效字号必须为正：$effectiveSize" }
    return floor(availableWidthDp / effectiveSize).toInt()
}

/**
 * 参数化校验：给定轮播画布宽度与老年缩放，判断标题（1 行）与副标题（2 行）
 * 在正常/老年模式下均不溢出。宽度与字号均为参数，避免针对单一机型硬编码。
 */
internal fun carouselPageTextFits(page: HomeCarouselPage, bannerWidthDp: Float, elderScale: Float): Boolean {
    val textWidth = bannerWidthDp - CAROUSEL_TEXT_H_PADDING_DP * 2f
    if (textWidth <= 0f) return false
    val titleFits = page.title.length <= cjkCharsPerLine(textWidth, CAROUSEL_TITLE_FONT_SP, elderScale)
    val subtitleFits = page.subtitle.length <= cjkCharsPerLine(textWidth, CAROUSEL_SUBTITLE_FONT_SP, elderScale) * 2
    return titleFits && subtitleFits
}

/** 底部统一对比层：副标题可能延伸到横向遮罩之外，此层保证文字条带的基础对比度。 */
internal fun carouselBottomVignette(): Brush = Brush.verticalGradient(
    0.55f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.30f),
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
                        .background(page.scrim.brush())
                        .background(carouselBottomVignette()),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = page.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(
                            shadow = Shadow(color = Color.Black.copy(alpha = 0.42f), blurRadius = 4f),
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = page.subtitle,
                        color = Color.White.copy(alpha = 0.95f),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            shadow = Shadow(color = Color.Black.copy(alpha = 0.38f), blurRadius = 4f),
                        ),
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

/**
 * 首页轮播页配置：5 张（重庆山城/纪念林地/花海/草坪/宠物草地）。
 * 每页遮罩依据素材文字区实测亮度（0-255，v11 素材 1774x887 亮度分析）推导，
 * 素材越亮遮罩越重、越暗越轻；结束位置统一约 42%，右侧画面完整还原。
 */
private const val LUMA_CHONGQING = 233.1f
private const val LUMA_TREE = 166.9f
private const val LUMA_FLOWER = 237.4f
private const val LUMA_LAWN = 136.9f
private const val LUMA_PET = 200.5f

/** 首页轮播页配置：5 张（重庆山城/纪念林地/花海/草坪/宠物草地）。 */
internal fun carouselPages(
    onOpenCityNews: () -> Unit,
    onOpenTree: () -> Unit,
    onOpenFlower: () -> Unit,
    onOpenLawn: () -> Unit,
    onOpenPet: () -> Unit,
): List<HomeCarouselPage> = listOf(
    HomeCarouselPage(
        title = "山水有归处",
        subtitle = "在熟悉的山城里，了解温和的告别方式",
        imageRes = R.drawable.home_carousel_chongqing,
        scrim = carouselScrimFor(LUMA_CHONGQING),
        onClick = onOpenCityNews,
    ),
    HomeCarouselPage(
        title = "林间寄思念",
        subtitle = "树葬：让纪念随树木一同生长",
        imageRes = R.drawable.home_carousel_tree,
        scrim = carouselScrimFor(LUMA_TREE),
        onClick = onOpenTree,
    ),
    HomeCarouselPage(
        title = "花开四季",
        subtitle = "花葬：思念落在公共花园里",
        imageRes = R.drawable.home_carousel_flower,
        scrim = carouselScrimFor(LUMA_FLOWER),
        onClick = onOpenFlower,
    ),
    HomeCarouselPage(
        title = "草坪之上",
        subtitle = "草坪葬：开阔宁静的追思空间",
        imageRes = R.drawable.home_carousel_lawn,
        scrim = carouselScrimFor(LUMA_LAWN),
        onClick = onOpenLawn,
    ),
    HomeCarouselPage(
        title = "相伴自然",
        subtitle = "宠物纪念：独立园区与林地",
        imageRes = R.drawable.home_carousel_pet,
        scrim = carouselScrimFor(LUMA_PET),
        onClick = onOpenPet,
    ),
)
