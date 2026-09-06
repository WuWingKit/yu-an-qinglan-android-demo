/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.home

import com.yuanqinglan.app.R
import com.yuanqinglan.app.core.designsystem.ElderFontScale
import com.yuanqinglan.app.feature.home.ui.CarouselScrimLimits
import com.yuanqinglan.app.feature.home.ui.HomeCarouselPage
import com.yuanqinglan.app.feature.home.ui.HomeCarouselScrim
import com.yuanqinglan.app.feature.home.ui.carouselPageTextFits
import com.yuanqinglan.app.feature.home.ui.carouselPages
import com.yuanqinglan.app.feature.home.ui.carouselScrimFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 首页轮播遮罩与文案溢出测试：
 * 1) 每页 scrim 的明暗参数（起止位置/透明度）必须落在安全范围；
 * 2) 遮罩按素材实测亮度推导，把文字区等效亮度压到安全区间；
 * 3) 普通/老年模式在常见画布宽度下标题、副标题均不溢出（参数化）。
 */
class HomeCarouselScrimTest {

    /** 素材文字区实测亮度（0-255，v11 素材 1774x887 亮度分析，与 carouselPages 一致）。 */
    private val measuredLumas = listOf(233.1f, 166.9f, 237.4f, 136.9f, 200.5f)

    @Test
    fun `每页遮罩参数落在安全范围且单调递减`() {
        val pages = carouselPages({}, {}, {}, {}, {})
        pages.forEach { page ->
            val s = page.scrim
            assertTrue(
                "${page.title} endFraction 越界：${s.endFraction}",
                s.endFraction in CarouselScrimLimits.MIN_END_FRACTION..CarouselScrimLimits.MAX_END_FRACTION,
            )
            assertTrue(
                "${page.title} startAlpha 越界：${s.startAlpha}",
                s.startAlpha in CarouselScrimLimits.MIN_START_ALPHA..CarouselScrimLimits.MAX_START_ALPHA,
            )
            assertEquals("${page.title} 遮罩应起始于左侧边缘", 0f, s.startFraction, 1e-6f)
            assertEquals("${page.title} 遮罩结束处应完全透明", 0f, s.endAlpha, 1e-6f)
            assertTrue("${page.title} 遮罩透明度应单调递减", s.endAlpha <= s.startAlpha)
        }
    }

    @Test
    fun `亮度规则把文字区等效亮度压到安全区间`() {
        measuredLumas.forEach { luma ->
            val s = carouselScrimFor(luma)
            val effective = luma * (1f - s.startAlpha)
            assertTrue("luma=$luma 等效亮度过亮：$effective", effective <= 150f)
            assertTrue("luma=$luma 等效亮度过暗：$effective", effective >= 50f)
        }
    }

    @Test
    fun `素材越亮遮罩越重`() {
        val dark = carouselScrimFor(136.9f)
        val mid = carouselScrimFor(200.5f)
        val bright = carouselScrimFor(237.4f)
        assertTrue("暗素材遮罩应更轻", dark.startAlpha < mid.startAlpha)
        assertTrue("亮素材遮罩应更重", mid.startAlpha < bright.startAlpha)
    }

    @Test
    fun `遮罩参数越界被拒绝`() {
        assertThrows(IllegalArgumentException::class.java) {
            HomeCarouselScrim(0f, 0.4f, 0f, 0f) // 起始位置不小于结束位置
        }
        assertThrows(IllegalArgumentException::class.java) {
            HomeCarouselScrim(-0.1f, 0.4f, 0.5f, 0f) // 起始位置越界
        }
        assertThrows(IllegalArgumentException::class.java) {
            HomeCarouselScrim(0f, 0.3f, 0.5f, 0.4f) // 透明度单调递增
        }
        assertThrows(IllegalArgumentException::class.java) {
            carouselScrimFor(0f) // 素材亮度非正
        }
    }

    @Test
    fun `普通与老年模式文案在常见宽度下均不溢出（参数化）`() {
        val pages = carouselPages({}, {}, {}, {}, {})
        // 轮播实际宽度 = 画布宽度 - 页面左右边距 14dp*2；以实际轮播宽度校验，保证最小机型也不溢出。
        listOf(272f, 292f, 332f, 347f, 383f).forEach { bannerWidth ->
            listOf(1f, ElderFontScale).forEach { scale ->
                pages.forEach { page ->
                    assertTrue(
                        "bannerWidth=$bannerWidth scale=$scale ${page.title} 溢出",
                        carouselPageTextFits(page, bannerWidth, scale),
                    )
                }
            }
        }
    }

    @Test
    fun `最长标题与副标题在最小宽度老年模式下仍容纳`() {
        val page = HomeCarouselPage(
            title = "六个字标题样例",
            subtitle = "十三个字的副标题内容样例测试文本",
            imageRes = R.drawable.home_carousel_tree,
            scrim = HomeCarouselScrim(0f, 0.4f, 0.42f, 0f),
            onClick = {},
        )
        assertTrue(carouselPageTextFits(page, 272f, ElderFontScale))
    }
}
