/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.treehole

import com.yuanqinglan.app.feature.treehole.model.KindResponse
import com.yuanqinglan.app.feature.treehole.ui.FLOWER_ANIMATION_MILLIS
import com.yuanqinglan.app.feature.treehole.ui.LAMP_ANIMATION_MILLIS
import com.yuanqinglan.app.feature.treehole.ui.LEAF_ANIMATION_MILLIS
import com.yuanqinglan.app.feature.treehole.ui.LEAF_COUNT
import com.yuanqinglan.app.feature.treehole.ui.LEAF_ANIMATION_SEED
import com.yuanqinglan.app.feature.treehole.ui.ResponseAnimationRequest
import com.yuanqinglan.app.feature.treehole.ui.ResponseAnimationThrottle
import com.yuanqinglan.app.feature.treehole.ui.RESPONSE_ANIMATION_MIN_INTERVAL_MILLIS
import com.yuanqinglan.app.feature.treehole.ui.bloomAlpha
import com.yuanqinglan.app.feature.treehole.ui.bloomRotation
import com.yuanqinglan.app.feature.treehole.ui.bloomScale
import com.yuanqinglan.app.feature.treehole.ui.kindResponseMessage
import com.yuanqinglan.app.feature.treehole.ui.lampAlpha
import com.yuanqinglan.app.feature.treehole.ui.lampScale
import com.yuanqinglan.app.feature.treehole.ui.leafAlpha
import com.yuanqinglan.app.feature.treehole.ui.leafScale
import com.yuanqinglan.app.feature.treehole.ui.leafSpecs
import com.yuanqinglan.app.feature.treehole.ui.responseAnimationEnabled
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Issue #17 轻回应动画反馈的纯逻辑测试（纯 JVM，不依赖 Android 框架）：
 * - 三种触发文案；
 * - 连续点击节流/取消状态机（不无限叠加）；
 * - 系统低动态（animator_duration_scale=0）降级决策；
 * - 无计数/持久化：动画请求只携带瞬时身份与类型，不暴露累计计数；
 * - 动画参数约束（600–1200ms、透明度/缩放/叶片运动保持克制且在边界内）。
 */
class TreeholeResponseFeedbackTest {

    // ---------- 三种触发文案 ----------

    @Test
    fun `三种轻回应各自返回稳定且互异的确认文案`() {
        val light = kindResponseMessage(KindResponse.LIGHT)
        val leaf = kindResponseMessage(KindResponse.LEAF)
        val flower = kindResponseMessage(KindResponse.FLOWER)

        assertTrue(light.isNotBlank())
        assertTrue(leaf.isNotBlank())
        assertTrue(flower.isNotBlank())

        // 同一种类文案稳定不变
        assertEquals(light, kindResponseMessage(KindResponse.LIGHT))
        assertEquals(leaf, kindResponseMessage(KindResponse.LEAF))
        assertEquals(flower, kindResponseMessage(KindResponse.FLOWER))

        // 三种文案互不相同
        assertEquals(3, setOf(light, leaf, flower).size)
    }

    // ---------- 连续点击节流 / 取消状态机 ----------

    @Test
    fun `首次触发立即接受，冷却窗口内连续点击被节流不叠加`() {
        val throttle = ResponseAnimationThrottle()
        val minInterval = RESPONSE_ANIMATION_MIN_INTERVAL_MILLIS

        val first = throttle.request(KindResponse.LIGHT, nowMillis = 0L)
        assertNotNull("首次触发应立即接受", first)
        assertEquals(1L, first!!.id)

        // 冷却窗口内（0..minInterval-1）连续点击均被节流：动画不重复叠加，但文案仍由调用方更新
        assertNull(throttle.request(KindResponse.LEAF, nowMillis = 10L))
        assertNull(throttle.request(KindResponse.FLOWER, nowMillis = 50L))
        assertNull(throttle.request(KindResponse.LIGHT, nowMillis = 100L))
        assertNull(throttle.request(KindResponse.LEAF, nowMillis = minInterval - 1L))
    }

    @Test
    fun `冷却窗口结束后再次触发接受且 id 递增以替换前一轮`() {
        val throttle = ResponseAnimationThrottle()
        val minInterval = RESPONSE_ANIMATION_MIN_INTERVAL_MILLIS

        val first = throttle.request(KindResponse.LIGHT, nowMillis = 0L)
        assertNotNull(first)

        // 恰好达到冷却窗口边界即接受
        val second = throttle.request(KindResponse.LEAF, nowMillis = minInterval)
        assertNotNull("冷却窗口边界应接受", second)
        assertEquals(2L, second!!.id)
        assertEquals(KindResponse.LEAF, second.kind)

        // 冷却结束后第三次触发：id 继续递增（UI 以 id 为 key 重建 → 自动取消前一轮）
        val third = throttle.request(KindResponse.FLOWER, nowMillis = minInterval * 2L)
        assertNotNull(third)
        assertEquals(3L, third!!.id)
        assertTrue("新请求 id 必须严格大于前一轮", third.id > second.id)
    }

    @Test
    fun `换一封信件时各自持有独立节流状态（互不累计）`() {
        val throttleA = ResponseAnimationThrottle()
        val throttleB = ResponseAnimationThrottle()

        // 两个独立实例在相同时刻触发互不节流：动画状态不跨信件/实例累计
        val a = throttleA.request(KindResponse.LIGHT, nowMillis = 100L)
        val b = throttleB.request(KindResponse.LIGHT, nowMillis = 100L)
        assertNotNull(a)
        assertNotNull(b)
    }

    // ---------- 系统低动态降级 ----------

    @Test
    fun `animator_duration_scale 为 0 时关闭动画，正值保留动画`() {
        assertFalse("0 表示系统关闭动画，须降级", responseAnimationEnabled(0f))
        assertTrue(responseAnimationEnabled(0.5f))
        assertTrue(responseAnimationEnabled(1f))
        assertTrue(responseAnimationEnabled(10f))
    }

    // ---------- 无计数 / 无持久化 ----------

    @Test
    fun `动画请求只携带瞬时身份与类型，不携带社交计数字段`() {
        // 结构契约：请求仅含 id（身份，用于取消前一轮）与 kind，
        // 绝无 count/rank/heat/score 等社交计数字段（$stable 为编译器合成，忽略）。
        val fieldNames = ResponseAnimationRequest::class.java
            .declaredFields
            .filter { !it.isSynthetic && !it.name.startsWith("$") }
            .map { it.name }
            .toSet()

        assertEquals(setOf("id", "kind"), fieldNames)

        // 行为契约：每次触发只返回一个带递增 id 的请求（不聚合、不累计计数）
        val throttle = ResponseAnimationThrottle()
        var previousId = 0L
        repeat(8) { i ->
            val request = throttle.request(KindResponse.LIGHT, nowMillis = i * 1000L)
            assertNotNull("第 ${i + 1} 次触发应产出一个请求", request)
            assertEquals("id 必须严格递增（单请求身份，非累计计数）", previousId + 1L, request!!.id)
            previousId = request.id
        }
    }

    @Test
    fun `动画时长均在 600 至 1200 毫秒的克制范围内`() {
        listOf(LAMP_ANIMATION_MILLIS, LEAF_ANIMATION_MILLIS, FLOWER_ANIMATION_MILLIS).forEach {
            assertTrue("动画时长 $it 不得低于 600ms", it >= 600)
            assertTrue("动画时长 $it 不得高于 1200ms", it <= 1200)
        }
    }

    // ---------- 动画参数边界（克制且稳定） ----------

    @Test
    fun `点灯扩散与淡出曲线保持在合理边界内`() {
        var progress = 0f
        while (progress <= 1f) {
            assertTrue(lampScale(progress) in (0.35f - FLOAT_EPS)..(1.15f + FLOAT_EPS))
            assertTrue(lampAlpha(progress) in 0f..1f)
            progress += 0.05f
        }
        assertTrue("光晕起点应较小", lampScale(0f) < 1f)
        assertTrue("光晕终点应扩散放大", lampScale(1f) > 1f)
        assertEquals(1f, lampAlpha(0f), 0.0001f)
        assertEquals(0f, lampAlpha(1f), 0.0001f)
    }

    @Test
    fun `叶片透明度边缘渐隐且参数由固定种子稳定生成`() {
        // 边缘（进入/离开）几乎透明：不突兀、不覆盖阅读
        assertEquals(0f, leafAlpha(0f, 0.5f), 0.0001f)
        assertEquals(0f, leafAlpha(1f, 0.5f), 0.0001f)
        // 中段保持峰值
        assertEquals(0.5f, leafAlpha(0.4f, 0.5f), 0.0001f)
        var progress = 0f
        while (progress <= 1f) {
            assertTrue(leafAlpha(progress, 0.9f) in 0f..1f)
            assertTrue(leafScale(progress) in 0.72f..1f)
            progress += 0.05f
        }
        assertEquals(0.72f, leafScale(0f), 0.0001f)
        assertEquals(1f, leafScale(0.5f), 0.0001f)
        assertEquals(0.72f, leafScale(1f), 0.0001f)

        // 固定种子：同一种子两次生成完全一致（截图/测试稳定）
        assertEquals(leafSpecs(LEAF_ANIMATION_SEED), leafSpecs(LEAF_ANIMATION_SEED))

        val specs = leafSpecs(LEAF_ANIMATION_SEED)
        assertEquals(LEAF_COUNT, specs.size)
        specs.forEach { spec ->
            assertTrue("叶片应向右掠过", spec.travelX > 0f)
            assertTrue("叶片应微微上飘", spec.travelY < 0f)
            assertTrue("叶片垂直位移须保持在行高范围内", spec.startY + spec.travelY >= 0f)
            assertTrue("透明度峰值须在合法区间", spec.alphaPeak in 0f..1f)
            assertTrue("叶片尺寸须保持小巧", spec.sizeDp in 9f..15f)
            assertTrue("起始时间比例须在 0..0.3 内", spec.startFraction in 0f..0.3f)
        }
    }

    @Test
    fun `花朵绽放缩放摇摆与淡出保持在合理边界内`() {
        assertEquals(0f, bloomAlpha(0f), 0.0001f)
        assertEquals(0f, bloomAlpha(1f), 0.0001f)
        assertTrue("绽放中段应可见", bloomAlpha(0.4f) > 0f)
        assertTrue("花朵应向外绽放放大", bloomScale(1f) > bloomScale(0f))

        var progress = 0f
        while (progress <= 1f) {
            assertTrue(bloomScale(progress) in (0.55f - FLOAT_EPS)..(1.15f + FLOAT_EPS))
            assertTrue(bloomAlpha(progress) in 0f..1f)
            assertTrue("摇摆幅度保持轻微（≤6°）", abs(bloomRotation(progress)) <= 6f)
            progress += 0.05f
        }
    }

    /** 浮点计算容差：缩放上界 1.15 可能因求和产生 ~1e-7 级误差。 */
    private companion object {
        const val FLOAT_EPS = 1e-4f
    }
}
