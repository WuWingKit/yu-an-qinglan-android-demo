/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.policy

import com.yuanqinglan.app.feature.policy.logic.SubsidyEstimator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 政策预审补贴参考测算规则测试（金额拆分 / 合计 / 提示）。 */
class SubsidyEstimatorTest {

    @Test
    fun `eco burial modes match subsidy line with reference amount and total`() {
        listOf("tree", "flower", "lawn").forEach { mode ->
            val estimate = SubsidyEstimator.estimate(mode, "渝中区")
            assertTrue("$mode 应匹配补贴项目", estimate.hasMatch)
            assertEquals(1, estimate.lines.size)
            assertEquals(1000, estimate.lines[0].amountYuan)
            assertEquals(1000, estimate.totalYuan)
            assertTrue(estimate.notes.isNotEmpty())
        }
    }

    @Test
    fun `traditional burial has no matched subsidy and clear note`() {
        val estimate = SubsidyEstimator.estimate("traditional", "渝中区")
        assertFalse(estimate.hasMatch)
        assertTrue(estimate.lines.isEmpty())
        assertEquals(0, estimate.totalYuan)
        assertTrue(estimate.notes.any { it.contains("不属于") })
    }

    @Test
    fun `sea project is informational with zero amount`() {
        val estimate = SubsidyEstimator.estimate("sea", "万州区")
        assertFalse(estimate.hasMatch)
        assertEquals(1, estimate.lines.size)
        assertEquals(0, estimate.lines[0].amountYuan)
        assertEquals(0, estimate.totalYuan)
    }

    @Test
    fun `unknown mode falls back to safe notes`() {
        val estimate = SubsidyEstimator.estimate("whatever", "渝北区")
        assertFalse(estimate.hasMatch)
        assertTrue(estimate.lines.isEmpty())
        assertEquals(0, estimate.totalYuan)
        assertTrue(estimate.notes.isNotEmpty())
    }

    @Test
    fun `blank county name renders generic reference text`() {
        val estimate = SubsidyEstimator.estimate("tree", "  ")
        assertTrue(estimate.hasMatch)
        assertTrue(estimate.notes.any { it.contains("民政部门") })
    }
}
