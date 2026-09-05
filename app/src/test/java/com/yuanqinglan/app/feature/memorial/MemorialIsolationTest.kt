/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial

import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.feature.memorial.data.HumanMemorialStore
import com.yuanqinglan.app.feature.memorial.data.PetMemorialStore
import com.yuanqinglan.app.feature.memorial.model.HumanMemorial
import com.yuanqinglan.app.feature.memorial.model.HumanMemorialDraft
import com.yuanqinglan.app.feature.memorial.model.MediaKind
import com.yuanqinglan.app.feature.memorial.model.MediaRef
import com.yuanqinglan.app.feature.memorial.model.MemorialIds
import com.yuanqinglan.app.feature.memorial.model.MemorialLetter
import com.yuanqinglan.app.feature.memorial.model.MemorialTrack
import com.yuanqinglan.app.feature.memorial.model.PaperStyle
import com.yuanqinglan.app.feature.memorial.model.PetMemorial
import com.yuanqinglan.app.feature.memorial.model.PetMemorialDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 人宠纪念空间数据强隔离：
 * 人类与宠物使用完全独立聚合类型、独立存储入口与互斥 ID 空间，
 * 任何新增/删除/内容写入都只落在所属轨，绝不串数据。
 */
class MemorialIsolationTest {

    private fun humanSeed(): HumanMemorial = HumanMemorial(
        id = "hm-seed-1",
        name = "陈永昌",
        relation = "外公",
        intro = "纪念介绍",
        createdAtMillis = 100L,
    )

    private fun petSeed(): PetMemorial = PetMemorial(
        id = "pm-seed-1",
        name = "年糕",
        relation = "我的伙伴",
        intro = "伙伴介绍",
        createdAtMillis = 200L,
    )

    @Test
    fun `人类与宠物存储各自只含本轨内容`() = runTest {
        val humanStore = HumanMemorialStore(seedProvider = { listOf(humanSeed()) })
        val petStore = PetMemorialStore(seedProvider = { listOf(petSeed()) })

        // 先触发惰性加载（space() 内部确保初始化），再读列表流。
        assertEquals("hm-seed-1", humanStore.space("hm-seed-1")?.id)
        assertEquals("pm-seed-1", petStore.space("pm-seed-1")?.id)

        val humanList = humanStore.spaces().first()
        val petList = petStore.spaces().first()
        assertTrue(humanList is DemoState.Success)
        assertTrue(petList is DemoState.Success)

        val humans = (humanList as DemoState.Success).value
        val pets = (petList as DemoState.Success).value
        assertEquals(listOf("hm-seed-1"), humans.map { it.id })
        assertEquals(listOf("pm-seed-1"), pets.map { it.id })
        // 任一轨都不可能出现对方前缀的 ID
        assertTrue(humans.none { it.id.startsWith("pm-") })
        assertTrue(pets.none { it.id.startsWith("hm-") })
    }

    @Test
    fun `新建空间按类型落到正确轨道且 ID 互斥`() = runTest {
        val humanStore = HumanMemorialStore(seedProvider = { listOf(humanSeed()) })
        val petStore = PetMemorialStore(seedProvider = { listOf(petSeed()) })

        val createdHuman = humanStore.create(
            HumanMemorialDraft(name = "林静萱", relation = "母亲", intro = ""),
        )
        val createdPet = petStore.create(
            PetMemorialDraft(name = "豆豆", relation = "伙伴", intro = ""),
        )

        assertTrue(createdHuman.id.startsWith(MemorialTrack.PREFIX_HUMAN))
        assertTrue(createdPet.id.startsWith(MemorialTrack.PREFIX_PET))
        assertFalse(createdHuman.id == createdPet.id)

        // 人类存储中找不到宠物 ID，反之亦然
        assertNull(humanStore.space(createdPet.id))
        assertNull(petStore.space(createdHuman.id))
        assertEquals(2, (humanStore.spaces().first() as DemoState.Success).value.size)
        assertEquals(2, (petStore.spaces().first() as DemoState.Success).value.size)
    }

    @Test
    fun `信件写入人类空间不影响宠物轨`() = runTest {
        val humanStore = HumanMemorialStore(seedProvider = { listOf(humanSeed()) })
        val petStore = PetMemorialStore(seedProvider = { listOf(petSeed()) })

        val letter = MemorialLetter(
            id = "hm-seed-1-ltr-1",
            memorialId = "hm-seed-1",
            title = "想念您的时候",
            body = "今天路过菜市场……",
            paper = PaperStyle.PLAIN,
            createdAtMillis = 300L,
        )
        assertTrue(humanStore.addLetter("hm-seed-1", letter))

        assertEquals(1, humanStore.allLetters().size)
        assertEquals(0, petStore.allLetters().size)
        assertTrue(petStore.allLetters().none { it.id == letter.id })
        // 对宠物轨追加相册也不影响人类轨
        val photo = MediaRef(
            id = MemorialIds.next("ph"),
            kind = MediaKind.IMAGE_FILE,
            value = "file:///tmp/pet.png",
            name = "pet.png",
            sizeBytes = 1024,
        )
        assertTrue(petStore.addGalleryMedia("pm-seed-1", photo))
        assertEquals(0, humanStore.space("hm-seed-1")?.gallery?.size ?: -1)
        assertEquals(1, petStore.space("pm-seed-1")?.gallery?.size)
    }

    @Test
    fun `按 ID 解析轨道且非法前缀抛错`() {
        assertEquals(MemorialTrack.HUMAN, MemorialTrack.ofId("hm-abc"))
        assertEquals(MemorialTrack.PET, MemorialTrack.ofId("pm-abc"))
        var threw = false
        try {
            MemorialTrack.ofId("xx-abc")
        } catch (expected: IllegalArgumentException) {
            threw = true
        }
        assertTrue("非法 ID 应明确抛错而非静默归轨", threw)
    }
}
