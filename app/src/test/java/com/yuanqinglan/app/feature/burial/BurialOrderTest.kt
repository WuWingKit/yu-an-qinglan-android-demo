/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial

import com.yuanqinglan.app.feature.burial.model.BurialOrderStatus
import com.yuanqinglan.app.feature.burial.model.OrderNumberGenerator
import com.yuanqinglan.app.feature.burial.model.OrderProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * 订单进度状态机：合法单向迁移、可推进、可重置、终态幂等；
 * 订单号本地生成，格式 YQ-YYYYMMDD-NNNN。
 */
class BurialOrderTest {

    @Test
    fun `状态顺序与文案固定`() {
        assertEquals(
            listOf("提交成功", "机构确认", "服务安排", "已完成"),
            BurialOrderStatus.entries.map { it.title },
        )
        assertEquals(listOf(1, 2, 3, 4), BurialOrderStatus.entries.map { it.step })
    }

    @Test
    fun `合法迁移逐步推进`() {
        var status = OrderProgress.reset()
        assertEquals(BurialOrderStatus.SUBMITTED, status)

        status = OrderProgress.advance(status)
        assertEquals(BurialOrderStatus.CONFIRMED, status)
        assertTrue(status.canAdvance())

        status = OrderProgress.advance(status)
        assertEquals(BurialOrderStatus.ARRANGED, status)

        status = OrderProgress.advance(status)
        assertEquals(BurialOrderStatus.COMPLETED, status)
        assertFalse(status.canAdvance())

        // 终态推进幂等，不会越界
        assertEquals(BurialOrderStatus.COMPLETED, OrderProgress.advance(status))
    }

    @Test
    fun `不可跳级`() {
        // nextOrNull 只返回相邻下一状态，无法从 SUBMITTED 直接到 ARRANGED
        assertEquals(BurialOrderStatus.CONFIRMED, BurialOrderStatus.SUBMITTED.nextOrNull())
        assertNull(BurialOrderStatus.COMPLETED.nextOrNull())
    }

    @Test
    fun `可重置回初始状态`() {
        var status = BurialOrderStatus.ARRANGED
        status = OrderProgress.advance(status)
        assertEquals(BurialOrderStatus.COMPLETED, status)

        assertEquals(BurialOrderStatus.SUBMITTED, OrderProgress.reset())
    }

    @Test
    fun `订单号生成格式固定且可注入时间`() {
        val generator = OrderNumberGenerator(now = { LocalDateTime.of(2026, 9, 5, 10, 30) })
        assertEquals("YQ-20260905-0001", generator.next(1))
        assertEquals("YQ-20260905-0042", generator.next(42))
        // 顺序推进生成不同订单号
        val a = generator.next(1)
        val b = generator.next(2)
        assertTrue(a != b)
        assertTrue(Regex("^YQ-\\d{8}-\\d{4}\$").matches(a))
    }
}
