/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiMemoryVideoRulesTest {
    @Test
    fun `four memorial spaces have independent authorized video sources`() {
        assertEquals(
            setOf("hm-001", "hm-002", "hm-003", "pm-002"),
            AiMemoryVideoRules.availableMemorialIds,
        )
        assertEquals(
            AiMemoryVideoRules.presets.size,
            AiMemoryVideoRules.presets.values.map { it.sourceDrawable }.toSet().size,
        )
        assertEquals(
            AiMemoryVideoRules.presets.size,
            AiMemoryVideoRules.presets.values.map { it.videoRaw }.toSet().size,
        )
    }

    @Test
    fun `memorial spaces without paired video stay unavailable`() {
        assertFalse(AiMemoryVideoRules.isAvailable("pm-001"))
        assertFalse(AiMemoryVideoRules.isAvailable(""))
    }

    @Test
    fun `human and pet generation stages are ordered and subject aware`() {
        val human = AiMemoryVideoRules.stagesFor(AiMemorySubjectKind.HUMAN)
        val pet = AiMemoryVideoRules.stagesFor(AiMemorySubjectKind.PET)

        listOf(human, pet).forEach { stages ->
            assertEquals(5, stages.size)
            assertTrue(stages.zipWithNext().all { (first, second) ->
                first.targetProgress < second.targetProgress
            })
            assertTrue(stages.last().title.contains("合成"))
            assertEquals(0.96f, stages.last().targetProgress)
        }
        assertTrue(human.any { it.detail.contains("面部") || it.detail.contains("五官") })
        assertTrue(pet.any { it.detail.contains("宠物") || it.detail.contains("毛发") })
    }

    @Test
    fun `photo restoration preview stays within memorial track`() {
        assertEquals(PetPortraitToken, aiRestorePreviewToken("pm-001"))
        assertEquals(DogPortraitToken, aiRestorePreviewToken("pm-002"))
        assertEquals(AiRestoreSampleToken, aiRestorePreviewToken("hm-001"))
        assertEquals(AiRestoreSampleToken, aiRestorePreviewToken(""))
    }
}
