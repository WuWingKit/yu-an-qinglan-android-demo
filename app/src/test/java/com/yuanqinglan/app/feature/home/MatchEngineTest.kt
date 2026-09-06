/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.home

import com.yuanqinglan.app.feature.home.logic.MatchCatalog
import com.yuanqinglan.app.feature.home.logic.MatchEngine
import com.yuanqinglan.app.feature.home.model.BurialModeLabels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 智能匹配本地规则测试：偏好、预算、纪念需求、缺省输入。 */
class MatchEngineTest {

    private fun answers(
        region: String = "region-center",
        budget: String = "budget-mid",
        pref: String = "pref-unsure",
        memorial: String = "mem-unsure",
    ): Map<String, String> = mapOf(
        MatchCatalog.Q_REGION to region,
        MatchCatalog.Q_BUDGET to budget,
        MatchCatalog.Q_PREF to pref,
        MatchCatalog.Q_MEMORIAL to memorial,
    )

    @Test
    fun `respects explicit tree preference`() {
        val result = MatchEngine.recommend(answers(pref = "pref-tree"))
        assertEquals("tree", result.targetKey)
        assertTrue(result.reasons.isNotEmpty())
        assertTrue(result.nextActions.isNotEmpty())
    }

    @Test
    fun `respects explicit flower and lawn preference`() {
        assertEquals("flower", MatchEngine.recommend(answers(pref = "pref-flower")).targetKey)
        assertEquals("lawn", MatchEngine.recommend(answers(pref = "pref-lawn")).targetKey)
    }

    @Test
    fun `traditional preference with low budget suggests lawn`() {
        val result = MatchEngine.recommend(
            answers(pref = "pref-traditional", budget = "budget-low", memorial = "mem-nomarker"),
        )
        assertEquals("tree", result.targetKey)
        assertTrue(result.title.contains(BurialModeLabels.TREE))
    }

    @Test
    fun `no preference and low budget leads to lawn suggestion`() {
        val result = MatchEngine.recommend(answers(pref = "pref-unsure", budget = "budget-low"))
        assertEquals("lawn", result.targetKey)
        assertTrue(result.title.contains(BurialModeLabels.LAWN))
    }

    @Test
    fun `natural memorial wish without preference leads to tree`() {
        val result = MatchEngine.recommend(answers(pref = "pref-unsure", budget = "budget-mid", memorial = "mem-natural"))
        assertEquals("tree", result.targetKey)
    }

    @Test
    fun `empty answers still returns a fallback recommendation`() {
        val result = MatchEngine.recommend(emptyMap())
        assertEquals("general", result.targetKey)
        assertTrue(result.reasons.isNotEmpty())
    }

    @Test
    fun `unknown option values are tolerated without crashing`() {
        val result = MatchEngine.recommend(
            mapOf(
                MatchCatalog.Q_PREF to "pref-unknown-value",
                MatchCatalog.Q_BUDGET to "budget-???",
                MatchCatalog.Q_REGION to "mars",
            ),
        )
        assertTrue(result.reasons.isNotEmpty())
        assertEquals("general", result.targetKey)
    }

    @Test
    fun `question catalog has four questions each with options`() {
        assertEquals(4, MatchCatalog.questions.size)
        MatchCatalog.questions.forEach { question ->
            assertTrue(question.options.isNotEmpty())
            assertEquals(question.options.size, question.options.map { it.value }.toSet().size)
        }
    }
}
