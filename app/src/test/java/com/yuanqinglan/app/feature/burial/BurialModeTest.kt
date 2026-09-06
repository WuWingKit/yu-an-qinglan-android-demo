/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial

import com.yuanqinglan.app.core.model.AudienceTrack
import com.yuanqinglan.app.feature.burial.model.BurialMode
import com.yuanqinglan.app.feature.burial.model.modeOfServiceId
import com.yuanqinglan.app.feature.burial.model.serviceDisplayName
import com.yuanqinglan.app.feature.burial.model.serviceIdFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BurialModeTest {

    @Test
    fun `mode 受控枚举覆盖三种葬式`() {
        assertEquals(listOf("TREE", "FLOWER", "LAWN"), BurialMode.entries.map { it.token })
        assertEquals(listOf("树葬", "花葬", "草坪葬"), BurialMode.entries.map { it.label })
    }

    @Test
    fun `route 参数合法值解析为对应模式`() {
        assertEquals(BurialMode.TREE, BurialMode.parseRouteMode("TREE"))
        assertEquals(BurialMode.FLOWER, BurialMode.parseRouteMode("FLOWER"))
        assertEquals(BurialMode.LAWN, BurialMode.parseRouteMode("LAWN"))
        // 大小写不敏感
        assertEquals(BurialMode.FLOWER, BurialMode.parseRouteMode("flower"))
    }

    @Test
    fun `route 参数非法或缺失一律回退 TREE`() {
        assertEquals(BurialMode.TREE, BurialMode.parseRouteMode(null))
        assertEquals(BurialMode.TREE, BurialMode.parseRouteMode(""))
        assertEquals(BurialMode.TREE, BurialMode.parseRouteMode("PARK"))
        assertEquals(BurialMode.TREE, BurialMode.parseRouteMode("TREE_BAD"))
    }

    @Test
    fun `轨道加模式推导的服务标识互不串轨`() {
        // 人类三标识（契约键 tree/flower/grass）
        assertEquals("tree", serviceIdFor(AudienceTrack.HUMAN, BurialMode.TREE))
        assertEquals("flower", serviceIdFor(AudienceTrack.HUMAN, BurialMode.FLOWER))
        assertEquals("grass", serviceIdFor(AudienceTrack.HUMAN, BurialMode.LAWN))
        // 宠物三标识
        assertEquals("pet-tree", serviceIdFor(AudienceTrack.PET, BurialMode.TREE))
        assertEquals("pet-flower", serviceIdFor(AudienceTrack.PET, BurialMode.FLOWER))
        assertEquals("pet-lawn", serviceIdFor(AudienceTrack.PET, BurialMode.LAWN))
        // 人类标识集合与宠物标识集合不相交
        val humanIds = BurialMode.entries.map { serviceIdFor(AudienceTrack.HUMAN, it) }.toSet()
        val petIds = BurialMode.entries.map { serviceIdFor(AudienceTrack.PET, it) }.toSet()
        assertTrue((humanIds intersect petIds).isEmpty())
    }

    @Test
    fun `服务标识可反推模式`() {
        assertEquals(BurialMode.TREE, modeOfServiceId(AudienceTrack.HUMAN, "tree"))
        assertEquals(BurialMode.LAWN, modeOfServiceId(AudienceTrack.HUMAN, "grass"))
        assertEquals(BurialMode.FLOWER, modeOfServiceId(AudienceTrack.PET, "pet-flower"))
        // 跨轨标识反推失败：人类轨不应接受 pet- 前缀
        assertNull(modeOfServiceId(AudienceTrack.HUMAN, "pet-tree"))
        assertNull(modeOfServiceId(AudienceTrack.PET, "grass"))
    }

    @Test
    fun `对外服务名按轨道区分`() {
        assertEquals("树葬", serviceDisplayName(AudienceTrack.HUMAN, BurialMode.TREE))
        assertEquals("宠物树葬", serviceDisplayName(AudienceTrack.PET, BurialMode.TREE))
        assertEquals("宠物草坪葬", serviceDisplayName(AudienceTrack.PET, BurialMode.LAWN))
    }
}
