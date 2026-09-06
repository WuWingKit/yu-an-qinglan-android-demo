/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial

import com.yuanqinglan.app.feature.memorial.model.MemorialDate
import com.yuanqinglan.app.feature.memorial.model.MemorialDateRules
import com.yuanqinglan.app.feature.memorial.model.MemorialDateRules.toLocalDateOrNull
import com.yuanqinglan.app.feature.memorial.model.MemorialDateRules.toMemorialDate
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Issue #18 日期规则纯函数测试：空值 / 仅年份 / 年月 / 完整日期 / 闰日 / 同日 /
 * 逆序 / 未来日期拦截 / 精度感知比较 / 自然格式化。全部无状态，不依赖 Android。
 */
class MemorialDateRulesTest {

    private val today: LocalDate = LocalDate.of(2030, 6, 15)

    // ---------------- 空值与格式化 ----------------

    @Test
    fun `空日期默认可提交且格式化为未知`() {
        val result = MemorialDateRules.validate(null, null, today)
        assertTrue(result.isOk)
        assertNull(result.birthError)
        assertNull(result.deathError)
        assertEquals("未知", MemorialDateRules.formatMemorialDate(null))
    }

    @Test
    fun `三种精度自然格式化不含脏占位`() {
        assertEquals("1996年", MemorialDateRules.formatMemorialDate(MemorialDate(1996)))
        assertEquals("1996年2月", MemorialDateRules.formatMemorialDate(MemorialDate(1996, 2)))
        assertEquals("1996年2月3日", MemorialDateRules.formatMemorialDate(MemorialDate(1996, 2, 3)))
    }

    // ---------------- 仅年份 ----------------

    @Test
    fun `仅年份可提交且不与同年离世误报逆序`() {
        val birth = MemorialDate(1996)
        assertTrue(MemorialDateRules.validate(birth, null, today).isOk)
        // 仅年份出生 + 同年 5 月离世：无法证明逆序，应通过
        assertTrue(MemorialDateRules.validate(birth, MemorialDate(1996, 5), today).isOk)
    }

    @Test
    fun `仅年份未来拦截按年判断`() {
        assertEquals(
            "出生日期不能晚于今天",
            MemorialDateRules.birthError(MemorialDate(2099), today),
        )
        assertEquals(
            "离世日期不能晚于今天",
            MemorialDateRules.deathError(MemorialDate(2099), today),
        )
        assertNull(MemorialDateRules.birthError(MemorialDate(1996), today))
    }

    // ---------------- 闰日与历法合法性 ----------------

    @Test
    fun `闰年 2 月 29 日合法而非闰年同日不合法`() {
        assertNull(MemorialDateRules.birthError(MemorialDate(2024, 2, 29), today))
        assertEquals(
            "出生日期不合法",
            MemorialDateRules.birthError(MemorialDate(2023, 2, 29), today),
        )
        assertEquals(
            "离世日期不合法",
            MemorialDateRules.deathError(MemorialDate(1900, 2, 29), today),
        )
    }

    @Test
    fun `非法月日被拒绝`() {
        assertEquals("出生日期不合法", MemorialDateRules.birthError(MemorialDate(1996, 13, 1), today))
        assertEquals("出生日期不合法", MemorialDateRules.birthError(MemorialDate(1996, 4, 31), today))
        assertEquals("出生日期不合法", MemorialDateRules.birthError(MemorialDate(1996, 2, 0), today))
    }

    // ---------------- 同日 / 逆序 ----------------

    @Test
    fun `出生与离世同日可提交`() {
        val same = MemorialDate(1996, 2, 3)
        assertTrue(MemorialDateRules.validate(same, same, today).isOk)
    }

    @Test
    fun `出生晚于离世被拦截并就近报错`() {
        val birth = MemorialDate(1996, 2, 3)
        val death = MemorialDate(1995, 12, 31)
        val result = MemorialDateRules.validate(birth, death, today)
        assertFalse(result.isOk)
        assertEquals("出生日期不能晚于离世日期", result.birthError)
        assertNull(result.deathError)
    }

    @Test
    fun `仅年份逆序可识别而部分精度不误报`() {
        // 出生 1997 > 离世 1996：明确逆序
        assertEquals(
            "出生日期不能晚于离世日期",
            MemorialDateRules.orderError(MemorialDate(1997), MemorialDate(1996)),
        )
        // 出生 1996 < 离世 1997：正常
        assertNull(MemorialDateRules.orderError(MemorialDate(1996), MemorialDate(1997)))
        // 出生 1996-05、离世 1996（仅年份）：无法证明逆序
        assertNull(MemorialDateRules.orderError(MemorialDate(1996, 5), MemorialDate(1996)))
        // 出生 1996-05-10、离世 1996-05（年月）：同月，无法证明逆序
        assertNull(MemorialDateRules.orderError(MemorialDate(1996, 5, 10), MemorialDate(1996, 5)))
        // 出生 1996-05-10、离世 1996-04：可证逆序
        assertEquals(
            "出生日期不能晚于离世日期",
            MemorialDateRules.orderError(MemorialDate(1996, 5, 10), MemorialDate(1996, 4)),
        )
    }

    // ---------------- 未来日期 ----------------

    @Test
    fun `未来完整日期按日拦截`() {
        val futureBirth = LocalDate.of(2031, 1, 1)
        assertEquals(
            "出生日期不能晚于今天",
            MemorialDateRules.birthError(futureBirth.toMemorialDate(), today),
        )
        assertEquals(
            "离世日期不能晚于今天",
            MemorialDateRules.deathError(LocalDate.of(2030, 12, 31).toMemorialDate(), today),
        )
    }

    @Test
    fun `今天及更早的日期可提交`() {
        assertNull(MemorialDateRules.deathError(today.toMemorialDate(), today))
        assertNull(MemorialDateRules.deathError(today.minusDays(1).toMemorialDate(), today))
    }

    // ---------------- 转换 ----------------

    @Test
    fun `完整日期与 LocalDate 互转往返`() {
        assertEquals(LocalDate.of(1996, 2, 3), MemorialDate(1996, 2, 3).toLocalDateOrNull())
        assertEquals(MemorialDate(2024, 4, 4), LocalDate.of(2024, 4, 4).toMemorialDate())
        // 仅年份/年月无法回填选择器
        assertNull(MemorialDate(1996).toLocalDateOrNull())
        assertNull(MemorialDate(1996, 2).toLocalDateOrNull())
    }

    // ---------------- 提交门禁组合 ----------------

    @Test
    fun `同时未来且逆序时先报字段级未来错误`() {
        val birth = LocalDate.of(2031, 1, 1).toMemorialDate()
        val death = MemorialDate(2020)
        val result = MemorialDateRules.validate(birth, death, today)
        assertEquals("出生日期不能晚于今天", result.birthError)
    }
}
