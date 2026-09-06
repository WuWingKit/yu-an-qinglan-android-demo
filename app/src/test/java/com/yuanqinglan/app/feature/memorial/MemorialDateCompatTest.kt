/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial

import com.yuanqinglan.app.data.local.AppJson
import com.yuanqinglan.app.feature.memorial.data.HumanMemorialsFile
import com.yuanqinglan.app.feature.memorial.data.HumanMemorialStore
import com.yuanqinglan.app.feature.memorial.data.MemorialSnapshotIo
import com.yuanqinglan.app.feature.memorial.data.PetMemorialsFile
import com.yuanqinglan.app.feature.memorial.data.PetMemorialStore
import com.yuanqinglan.app.feature.memorial.model.HumanMemorial
import com.yuanqinglan.app.feature.memorial.model.HumanMemorialDraft
import com.yuanqinglan.app.feature.memorial.model.MediaKind
import com.yuanqinglan.app.feature.memorial.model.MediaRef
import com.yuanqinglan.app.feature.memorial.model.MemorialDate
import com.yuanqinglan.app.feature.memorial.model.PetMemorial
import com.yuanqinglan.app.feature.memorial.model.PetMemorialDraft
import java.io.File
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Issue #18 兼容性与隔离测试：
 * - 旧 JSON / 旧快照缺日期字段 → 按 null 解码且内容完整，不丢纪念空间；
 * - 日期序列化往返、null 不写字段（与新快照字节兼容）；
 * - 新建/编辑/重启读取（快照恢复）闭环；人宠双轨日期完全隔离。
 */
class MemorialDateCompatTest {

    // ---------------- 旧 JSON 兼容 ----------------

    @Test
    fun `旧格式 JSON 缺日期字段按 null 解码且内容完整`() {
        val legacy = """
            {
              "memorials": [
                {
                  "id": "hm-old-1",
                  "name": "旧档案",
                  "portrait": "memorial_human_portrait",
                  "relation": "父亲",
                  "intro": "旧简介",
                  "createdAtMillis": 100,
                  "gallery": [
                    { "id": "g1", "kind": "DRAWABLE", "value": "memorial_gallery_family_tea", "name": "老照片" }
                  ],
                  "messages": [
                    { "id": "m1", "author": "我", "text": "旧留言", "createdAtMillis": 200 }
                  ]
                }
              ]
            }
        """.trimIndent()
        val decoded = AppJson.decodeFromString(HumanMemorialsFile.serializer(), legacy).memorials.single()

        assertNull("旧 JSON 缺少 birthDate 应解码为 null", decoded.birthDate)
        assertNull("旧 JSON 缺少 deathDate 应解码为 null", decoded.deathDate)
        assertEquals("旧档案", decoded.name)
        assertEquals(1, decoded.gallery.size)
        assertEquals(1, decoded.messages.size)
    }

    @Test
    fun `当前资产 JSON 日期字段可解析且精度正确`() {
        val humans = AppJson.decodeFromString(
            HumanMemorialsFile.serializer(),
            File("src/main/assets/demo/memorial/memorials_human.json").readText(Charsets.UTF_8),
        ).memorials
        val chen = humans.first { it.id == "hm-001" }
        assertEquals(MemorialDate(1935, 3, 12), chen.birthDate)
        assertEquals(MemorialDate(2024, 4, 4), chen.deathDate)

        val pets = AppJson.decodeFromString(
            PetMemorialsFile.serializer(),
            File("src/main/assets/demo/memorial/memorials_pet.json").readText(Charsets.UTF_8),
        ).memorials
        val cat = pets.first { it.id == "pm-001" }
        assertEquals(MemorialDate(2020, 4), cat.birthDate)
        assertEquals(MemorialDate(2025, 6, 28), cat.deathDate)
    }

    // ---------------- 序列化往返 ----------------

    @Test
    fun `日期序列化往返且 null 日期不写字段`() {
        val withDates = HumanMemorial(
            id = "hm-rt-1",
            name = "往返",
            relation = "外公",
            intro = "",
            createdAtMillis = 1L,
            birthDate = MemorialDate(1996, 2, 3),
            deathDate = null,
        )
        val encoded = AppJson.encodeToString(ListSerializer(HumanMemorial.serializer()), listOf(withDates))
        assertTrue("带日期时应写出 birthDate", encoded.contains("birthDate"))
        assertTrue("null deathDate 不应写字段（与新快照字节兼容）", !encoded.contains("deathDate"))

        val decoded = AppJson.decodeFromString(
            ListSerializer(HumanMemorial.serializer()),
            encoded,
        ).single()
        assertEquals(MemorialDate(1996, 2, 3), decoded.birthDate)
        assertNull(decoded.deathDate)
    }

    // ---------------- 旧快照兼容 ----------------

    @Test
    fun `旧快照缺日期字段可读且不丢其他内容`() = runTest {
        val legacySnapshot = """
            [
              {
                "id": "hm-snap-old",
                "name": "快照旧客",
                "relation": "祖母",
                "intro": "快照简介",
                "createdAtMillis": 50,
                "gallery": [
                  { "id": "g1", "kind": "DRAWABLE", "value": "memorial_gallery_family_tea", "name": "旧照" }
                ],
                "messages": [
                  { "id": "m1", "author": "我", "text": "快照留言", "createdAtMillis": 60 }
                ]
              }
            ]
        """.trimIndent()
        val io = FakeSnapshotIo().also { it.files["human_memorials.json"] = legacySnapshot }
        val store = HumanMemorialStore(seedProvider = { emptyList() }, snapshotIo = io)

        val loaded = store.space("hm-snap-old")
        assertNotNull("旧快照不应因缺日期字段回退或丢弃", loaded)
        loaded!!.let { m ->
            assertEquals("快照旧客", m.name)
            assertNull(m.birthDate)
            assertNull(m.deathDate)
            assertEquals(1, m.gallery.size)
            assertEquals(1, m.messages.size)
            assertFalse("快照内容应保留，不清空", m.gallery.isEmpty() && m.messages.isEmpty())
        }
    }

    // ---------------- 新建 / 编辑 / 重启读取 闭环 ----------------

    @Test
    fun `新建含日期空间重启后快照恢复读取`() = runTest {
        val io = FakeSnapshotIo()
        val seed = HumanMemorial(
            id = "hm-seed",
            name = "内置",
            relation = "外公",
            intro = "",
            createdAtMillis = 0L,
        )
        val store1 = HumanMemorialStore(seedProvider = { listOf(seed) }, snapshotIo = io)
        val created = store1.create(
            HumanMemorialDraft(
                name = "新客",
                relation = "母亲",
                intro = "",
                birthDate = MemorialDate(1965, 7, 8),
                deathDate = MemorialDate(2025, 5, 3),
            ),
        )
        assertEquals(MemorialDate(1965, 7, 8), created.birthDate)
        assertEquals(MemorialDate(2025, 5, 3), created.deathDate)

        // 同一快照 IO 重建存储，模拟进程重启
        val store2 = HumanMemorialStore(seedProvider = { listOf(seed) }, snapshotIo = io)
        val restored = store2.space(created.id)
        assertNotNull("重启后应能从快照恢复新建空间", restored)
        restored!!.let {
            assertEquals("新客", it.name)
            assertEquals(MemorialDate(1965, 7, 8), it.birthDate)
            assertEquals(MemorialDate(2025, 5, 3), it.deathDate)
        }
    }

    @Test
    fun `编辑日期持久化且可清空回未知`() = runTest {
        val store = HumanMemorialStore(
            seedProvider = {
                listOf(
                    HumanMemorial(id = "hm-edit", name = "编辑", relation = "父亲", intro = "", createdAtMillis = 0L),
                )
            },
        )
        val updated = store.updateMeta(
            "hm-edit",
            name = "编辑",
            relation = "父亲",
            intro = "",
            birthDate = MemorialDate(1960, 1, 1),
            deathDate = MemorialDate(2020, 12, 31),
        )
        assertEquals(MemorialDate(1960, 1, 1), updated?.birthDate)
        assertEquals(MemorialDate(2020, 12, 31), updated?.deathDate)

        val cleared = store.updateMeta("hm-edit", "编辑", "父亲", "", null, null)
        assertNull(cleared?.birthDate)
        assertNull(cleared?.deathDate)
    }

    // ---------------- 人宠双轨日期隔离 ----------------

    @Test
    fun `人宠双轨日期完全隔离互不影响`() = runTest {
        val humanStore = HumanMemorialStore(seedProvider = { emptyList() })
        val petStore = PetMemorialStore(seedProvider = { emptyList() })

        val human = humanStore.create(
            HumanMemorialDraft(
                name = "人",
                relation = "母亲",
                intro = "",
                birthDate = MemorialDate(1965, 7, 8),
                deathDate = MemorialDate(2025, 5, 3),
            ),
        )
        val pet = petStore.create(PetMemorialDraft(name = "宠", relation = "伙伴", intro = ""))

        assertEquals(MemorialDate(1965, 7, 8), human.birthDate)
        assertEquals(MemorialDate(2025, 5, 3), human.deathDate)
        // 宠物轨不携带人类日期
        assertNull(pet.birthDate)
        assertNull(pet.deathDate)
        // 人类空间在宠物轨不可见
        assertNull(petStore.space(human.id))
        assertNull(humanStore.space(pet.id))

        // 编辑人类日期不影响宠物轨
        humanStore.updateMeta(human.id, "人", "母亲", "", MemorialDate(1966, 1, 1), null)
        assertEquals(MemorialDate(1966, 1, 1), humanStore.space(human.id)?.birthDate)
        assertNull(petStore.space(pet.id)?.birthDate)
    }

    @Test
    fun `创建草稿默认空日期`() = runTest {
        val store = HumanMemorialStore(seedProvider = { emptyList() })
        val created = store.create(HumanMemorialDraft(name = "默认", relation = "朋友", intro = ""))
        assertNull(created.birthDate)
        assertNull(created.deathDate)
    }

    @Test
    fun `宠物轨创建含日期与人类轨类型互斥`() = runTest {
        val petStore = PetMemorialStore(seedProvider = { emptyList() })
        val pet = petStore.create(
            PetMemorialDraft(
                name = "年糕",
                relation = "伙伴",
                intro = "",
                birthDate = MemorialDate(2020, 4),
                deathDate = MemorialDate(2025, 6, 28),
            ),
        )
        assertTrue(pet.id.startsWith("pm-"))
        assertEquals(MemorialDate(2020, 4), pet.birthDate)
        assertEquals(MemorialDate(2025, 6, 28), pet.deathDate)
    }

    @Test
    fun `旧快照缺字段不会回退到种子内容`() = runTest {
        val legacySnapshot = """[{"id":"hm-snap-x","name":"快照客","relation":"朋友","intro":"","createdAtMillis":9}]"""
        val io = FakeSnapshotIo().also { it.files["human_memorials.json"] = legacySnapshot }
        val store = HumanMemorialStore(
            seedProvider = {
                listOf(HumanMemorial(id = "hm-seed-only", name = "种子", relation = "", intro = "", createdAtMillis = 0L))
            },
            snapshotIo = io,
        )
        // 快照中的纪念空间被恢复，种子未混入（快照存在即优先快照）
        assertNotNull(store.space("hm-snap-x"))
        assertNull("存在快照时不应回退到种子列表", store.space("hm-seed-only"))
    }

    @Test
    fun `媒体附件字段在日期加入后仍完整解码`() {
        val ref = MediaRef(
            id = "ph-1",
            kind = MediaKind.IMAGE_FILE,
            value = "file:///tmp/a.png",
            name = "a.png",
            sizeBytes = 1024,
        )
        val memorial = HumanMemorial(
            id = "hm-media",
            name = "媒体",
            relation = "",
            intro = "",
            createdAtMillis = 0L,
            gallery = listOf(ref),
            birthDate = MemorialDate(1990),
        )
        val roundTripped = AppJson.decodeFromString(
            HumanMemorial.serializer(),
            AppJson.encodeToString(HumanMemorial.serializer(), memorial),
        )
        assertEquals(MemorialDate(1990), roundTripped.birthDate)
        assertEquals(listOf("ph-1"), roundTripped.gallery.map { it.id })
        assertEquals(MediaKind.IMAGE_FILE, roundTripped.gallery.single().kind)
    }

    // ---------------- 内存快照 IO（测试替身） ----------------

    private class FakeSnapshotIo : MemorialSnapshotIo {
        val files = mutableMapOf<String, String>()
        override suspend fun read(name: String): String? = files[name]
        override suspend fun write(name: String, text: String) {
            files[name] = text
        }

        override suspend fun delete(name: String) {
            files.remove(name)
        }
    }
}
