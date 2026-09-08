/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial

import com.yuanqinglan.app.data.local.AppJson
import com.yuanqinglan.app.feature.memorial.data.HumanMemorialStore
import com.yuanqinglan.app.feature.memorial.data.MemorialSnapshotIo
import com.yuanqinglan.app.feature.memorial.data.PetMemorialStore
import com.yuanqinglan.app.feature.memorial.model.HumanMemorial
import com.yuanqinglan.app.feature.memorial.model.PetMemorial
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MemorialBundledAdditionMigrationTest {
    @Test
    fun `legacy human snapshot receives woman space without losing existing space`() = runTest {
        val existing = HumanMemorial(
            id = "hm-001",
            name = "外公",
            relation = "外公",
            intro = "原有内容",
            createdAtMillis = 1L,
        )
        val addition = HumanMemorial(
            id = "hm-003",
            name = "周雅琴",
            relation = "姑姑",
            intro = "新增内容",
            createdAtMillis = 2L,
        )
        val io = MemorySnapshotIo(
            "human_memorials.json",
            AppJson.encodeToString(ListSerializer(HumanMemorial.serializer()), listOf(existing)),
        )
        val store = HumanMemorialStore(
            seedProvider = { listOf(existing, addition) },
            snapshotIo = io,
        )

        assertNotNull(store.space("hm-001"))
        assertNotNull(store.space("hm-003"))
        val persisted = AppJson.decodeFromString(
            ListSerializer(HumanMemorial.serializer()),
            requireNotNull(io.files["human_memorials.json"]),
        )
        assertEquals(setOf("hm-001", "hm-003"), persisted.map { it.id }.toSet())
    }

    @Test
    fun `legacy pet snapshot receives dog space without losing existing space`() = runTest {
        val existing = PetMemorial(
            id = "pm-001",
            name = "年糕",
            relation = "我的伙伴",
            intro = "原有内容",
            createdAtMillis = 1L,
        )
        val addition = PetMemorial(
            id = "pm-002",
            name = "小满",
            relation = "我的伙伴",
            intro = "新增内容",
            createdAtMillis = 2L,
        )
        val io = MemorySnapshotIo(
            "pet_memorials.json",
            AppJson.encodeToString(ListSerializer(PetMemorial.serializer()), listOf(existing)),
        )
        val store = PetMemorialStore(
            seedProvider = { listOf(existing, addition) },
            snapshotIo = io,
        )

        assertNotNull(store.space("pm-001"))
        assertNotNull(store.space("pm-002"))
        val persisted = AppJson.decodeFromString(
            ListSerializer(PetMemorial.serializer()),
            requireNotNull(io.files["pet_memorials.json"]),
        )
        assertEquals(setOf("pm-001", "pm-002"), persisted.map { it.id }.toSet())
    }

    private class MemorySnapshotIo(name: String, content: String) : MemorialSnapshotIo {
        val files = mutableMapOf(name to content)

        override suspend fun read(name: String): String? = files[name]

        override suspend fun write(name: String, text: String) {
            files[name] = text
        }

        override suspend fun delete(name: String) {
            files.remove(name)
        }
    }
}
