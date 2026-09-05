/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial

import com.yuanqinglan.app.core.model.AudienceTrack
import com.yuanqinglan.app.feature.burial.model.BurialMode
import com.yuanqinglan.app.feature.burial.model.BurialPlan
import com.yuanqinglan.app.feature.burial.model.HumanBurialService
import com.yuanqinglan.app.feature.burial.model.PetBurialService
import com.yuanqinglan.app.feature.burial.model.PlanTier
import com.yuanqinglan.app.feature.burial.model.humanById
import com.yuanqinglan.app.feature.burial.model.petByMode
import com.yuanqinglan.app.feature.burial.model.petById
import com.yuanqinglan.app.feature.burial.model.planById
import com.yuanqinglan.app.feature.burial.model.serviceIdFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 人宠强类型隔离：人类与宠物服务/套餐使用互不相关的强类型列表与标识，
 * 切换轨道后数据绝不串联（人类列表不含宠物项、反之亦然）。
 */
class BurialIsolationTest {

    private fun humanService(id: String, mode: BurialMode, defaultPlanId: String) =
        HumanBurialService(
            id = id,
            mode = mode,
            name = "测试$id",
            subtitle = "副标题",
            priceRange = "约 1,000 元/位起",
            image = "img-$id",
            description = "说明",
            ecoDescription = "生态说明",
            process = listOf("步骤一", "步骤二"),
            applicable = "适用对象说明",
            defaultPlanId = defaultPlanId,
        )

    private fun petService(id: String, mode: BurialMode, defaultPlanId: String) =
        PetBurialService(
            id = id,
            mode = mode,
            name = "测试$id",
            subtitle = "副标题",
            priceRange = "约 800 元/位起",
            image = "img-$id",
            description = "说明",
            parkIntro = "独立园区介绍",
            process = listOf("步骤一", "步骤二"),
            defaultPlanId = defaultPlanId,
        )

    @Test
    fun `人类服务列表不含宠物项`() {
        val humans = listOf(
            humanService("tree", BurialMode.TREE, "p1"),
            humanService("flower", BurialMode.FLOWER, "p2"),
            humanService("grass", BurialMode.LAWN, "p3"),
        )
        assertEquals(setOf("tree", "flower", "grass"), humans.map { it.id }.toSet())
        assertTrue(humans.none { it.id.startsWith("pet-") })
        assertNull(humans.humanById("pet-tree"))
    }

    @Test
    fun `宠物服务列表不含人类项`() {
        val pets = listOf(
            petService("pet-tree", BurialMode.TREE, "q1"),
            petService("pet-flower", BurialMode.FLOWER, "q2"),
            petService("pet-lawn", BurialMode.LAWN, "q3"),
        )
        assertEquals(setOf("pet-tree", "pet-flower", "pet-lawn"), pets.map { it.id }.toSet())
        assertTrue(pets.none { it.id in setOf("tree", "flower", "grass") })
        assertNull(pets.petById("tree"))
    }

    @Test
    fun `宠物按 mode 只取到同模式服务`() {
        val pets = listOf(
            petService("pet-tree", BurialMode.TREE, "q1"),
            petService("pet-flower", BurialMode.FLOWER, "q2"),
            petService("pet-lawn", BurialMode.LAWN, "q3"),
        )
        assertEquals("pet-tree", pets.petByMode(BurialMode.TREE)?.id)
        assertEquals("pet-flower", pets.petByMode(BurialMode.FLOWER)?.id)
        assertEquals("pet-lawn", pets.petByMode(BurialMode.LAWN)?.id)
        // 宠物列表永远无法返回人类模式服务
        assertNull(pets.petByMode(BurialMode.TREE)?.takeIf { it.id == "tree" })
    }

    @Test
    fun `套餐轨道类型固定且标识跨轨互斥`() {
        val humanPlans = BurialMode.entries.map { mode ->
            BurialPlan(
                id = "plan-${serviceIdFor(AudienceTrack.HUMAN, mode)}",
                serviceId = serviceIdFor(AudienceTrack.HUMAN, mode),
                audience = AudienceTrack.HUMAN,
                mode = mode,
                tier = PlanTier.BASIC,
                title = "基础套餐",
                priceText = "1,000 元",
                contents = listOf("服务内容一"),
            )
        }
        val petPlans = BurialMode.entries.map { mode ->
            BurialPlan(
                id = "plan-${serviceIdFor(AudienceTrack.PET, mode)}",
                serviceId = serviceIdFor(AudienceTrack.PET, mode),
                audience = AudienceTrack.PET,
                mode = mode,
                tier = PlanTier.BASIC,
                title = "宠物基础套餐",
                priceText = "800 元",
                contents = listOf("宠物服务内容一"),
            )
        }

        assertTrue(humanPlans.all { it.audience == AudienceTrack.HUMAN })
        assertTrue(petPlans.all { it.audience == AudienceTrack.PET })
        // 套餐 ID 全量唯一（人类与宠物不在同一 ID 空间）
        assertEquals(6, (humanPlans + petPlans).map { it.id }.toSet().size)

        // 人类套餐的 serviceId 只指向人类服务标识；宠物套餐只指向宠物服务标识
        val humanIds = BurialMode.entries.map { serviceIdFor(AudienceTrack.HUMAN, it) }.toSet()
        val petIds = BurialMode.entries.map { serviceIdFor(AudienceTrack.PET, it) }.toSet()
        assertTrue(humanPlans.all { it.serviceId in humanIds })
        assertTrue(petPlans.all { it.serviceId in petIds })
        assertTrue(petPlans.all { it.serviceId !in humanIds })

        // 按 ID 反查不跨轨：人类套餐列表命中不了宠物套餐，反之亦然
        assertNull(humanPlans.planById("plan-pet-tree"))
        assertNull(humanPlans.planById("plan-pet-flower"))
        assertNull(petPlans.planById("plan-tree"))
        assertEquals(AudienceTrack.HUMAN, humanPlans.planById("plan-tree")?.audience)
        assertEquals(AudienceTrack.PET, petPlans.planById("plan-pet-tree")?.audience)
    }

    @Test
    fun `人类基础套餐价格约束可被数据校验`() {
        // 此处校验“人类基础款 ≤2000 元”的可计算口径：价格文本取数字部分
        val prices = listOf("1,580 元", "1,680 元", "1,880 元", "1,580 元/位")
        prices.forEach { text ->
            val digits = text.filter { it.isDigit() }.toIntOrNull()
            assertTrue("价格文本应为数字口径: $text", digits != null && digits <= 2000)
        }
    }
}
