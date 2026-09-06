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
    fun `featured mother memorial has authorized video source`() {
        assertTrue(AiMemoryVideoRules.isAvailable("hm-002"))
    }

    @Test
    fun `other memorial spaces do not reuse the featured portrait`() {
        assertFalse(AiMemoryVideoRules.isAvailable("hm-001"))
        assertFalse(AiMemoryVideoRules.isAvailable("pm-001"))
        assertFalse(AiMemoryVideoRules.isAvailable(""))
    }

    @Test
    fun `generation phases are ordered and complete`() {
        assertTrue(AiMemoryVideoRules.phases.size >= 4)
        assertTrue(AiMemoryVideoRules.phases.first().contains("面部"))
        assertTrue(AiMemoryVideoRules.phases.last().contains("合成"))
    }

    @Test
    fun `photo restoration preview stays within memorial track`() {
        assertEquals(PetPortraitToken, aiRestorePreviewToken("pm-001"))
        assertEquals(AiRestoreSampleToken, aiRestorePreviewToken("hm-001"))
        assertEquals(AiRestoreSampleToken, aiRestorePreviewToken(""))
    }
}
