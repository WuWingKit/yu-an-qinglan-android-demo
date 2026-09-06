/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial

import com.yuanqinglan.app.core.model.AudienceTrack
import com.yuanqinglan.app.feature.burial.model.BurialMode
import com.yuanqinglan.app.feature.burial.model.BurialPlan
import com.yuanqinglan.app.feature.burial.model.PlanPriceOption
import com.yuanqinglan.app.feature.burial.model.PlanTier
import com.yuanqinglan.app.feature.burial.model.plansOfService
import com.yuanqinglan.app.feature.burial.model.quote
import org.junit.Assert.assertEquals
import org.junit.Test

class BurialPlanPricingTest {

    private val plan = BurialPlan(
        id = "plan-human-tree-sincere",
        serviceId = "tree",
        audience = AudienceTrack.HUMAN,
        mode = BurialMode.TREE,
        tier = PlanTier.SINCERE,
        title = "诚·念 / 树葬",
        priceText = "19,800 元/位",
        contents = listOf("中等景观位"),
        priceYuan = 19_800,
        includedManagementYears = 5,
        renewalStartYear = 6,
        renewalAnnualYuan = 500,
        subsidyYuan = 3_000,
        managementPrepay = listOf(PlanPriceOption("management-10", "预付 10 年", 4_000, 10)),
        addOns = listOf(
            PlanPriceOption("flowers", "鲜花订阅", 288),
            PlanPriceOption("digital", "高级电子纪念馆", 8_800),
        ),
    )

    @Test
    fun `实付按套餐管理费增值项减补贴计算`() {
        val quote = plan.quote(10, setOf("flowers", "digital"), applySubsidy = true)

        assertEquals(4_000, quote.prepaidManagementYuan)
        assertEquals(9_088, quote.addOnYuan)
        assertEquals(3_000, quote.subsidyYuan)
        assertEquals(29_888, quote.totalYuan)
    }

    @Test
    fun `无效选择不计费且可关闭补贴`() {
        val quote = plan.quote(20, setOf("unknown"), applySubsidy = false)

        assertEquals(0, quote.prepaidYears)
        assertEquals(0, quote.prepaidManagementYuan)
        assertEquals(0, quote.addOnYuan)
        assertEquals(0, quote.subsidyYuan)
        assertEquals(19_800, quote.totalYuan)
    }

    @Test
    fun `三档套餐按简归诚念至念排序`() {
        val plans = listOf(
            plan.copy(id = "premium", tier = PlanTier.PREMIUM),
            plan.copy(id = "simple", tier = PlanTier.SIMPLE),
            plan.copy(id = "sincere", tier = PlanTier.SINCERE),
        ).plansOfService("tree")

        assertEquals(listOf("simple", "sincere", "premium"), plans.map { it.id })
    }
}
