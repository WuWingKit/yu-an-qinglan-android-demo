/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial

import com.yuanqinglan.app.data.local.AppJson
import com.yuanqinglan.app.feature.memorial.data.CollectiveActivitiesFile
import com.yuanqinglan.app.feature.memorial.data.DaijiPackagesFile
import com.yuanqinglan.app.feature.memorial.data.HumanMemorialsFile
import com.yuanqinglan.app.feature.memorial.data.PetMemorialsFile
import com.yuanqinglan.app.feature.memorial.model.MemorialTrack
import com.yuanqinglan.app.feature.treehole.data.HumanLettersFile
import com.yuanqinglan.app.feature.treehole.data.PetLettersFile
import com.yuanqinglan.app.feature.treehole.model.TreeholeLetterState
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 追忆/树洞内置 JSON 数据完整性：与运行时同一解码器读取 assets/demo 文件，
 * 校验结构可解析、ID 唯一、轨道前缀正确、无禁用文案。
 */
class MemorialTreeholeAssetJsonTest {

    private fun read(name: String): String {
        val file = File("src/main/assets/demo/$name")
        assertTrue("找不到 $name（cwd=${File(".").absolutePath}）", file.exists())
        return file.readText(Charsets.UTF_8)
    }

    private val banned = listOf("演示", "假数据", "原型", "纯前端")

    private fun assertClean(text: String, where: String) {
        banned.forEach { word ->
            assertTrue("$where 出现禁用文案[$word]", !text.contains(word))
        }
    }

    @Test
    fun `人类纪念空间 JSON 合法且轨道与内容自洽`() {
        val text = read("memorial/memorials_human.json")
        assertClean(text, "memorials_human.json")
        val file = AppJson.decodeFromString(HumanMemorialsFile.serializer(), text)
        assertEquals(2, file.memorials.size)
        file.memorials.forEach { m ->
            assertTrue(m.id.startsWith(MemorialTrack.PREFIX_HUMAN))
            assertTrue(m.name.isNotBlank())
            assertTrue(m.gallery.all { it.isDrawable })
            assertEquals(m.id, m.letters.firstOrNull()?.memorialId ?: m.id)
        }
    }

    @Test
    fun `宠物纪念空间 JSON 合法且不与人类共用 ID 空间`() {
        val humanText = read("memorial/memorials_human.json")
        val petText = read("memorial/memorials_pet.json")
        assertClean(petText, "memorials_pet.json")
        val humans = AppJson.decodeFromString(HumanMemorialsFile.serializer(), humanText).memorials
        val pets = AppJson.decodeFromString(PetMemorialsFile.serializer(), petText).memorials
        pets.forEach { m ->
            assertTrue(m.id.startsWith(MemorialTrack.PREFIX_PET))
            assertTrue(humans.none { it.id == m.id })
        }
    }

    @Test
    fun `代祭与共祭目录 JSON 均可解析且独立成文`() {
        val packages = AppJson.decodeFromString(
            DaijiPackagesFile.serializer(),
            read("memorial/daiji_packages.json"),
        ).packages
        assertTrue(packages.size >= 3)
        assertEquals(packages.size, packages.map { it.id }.toSet().size)
        packages.forEach { p -> assertTrue(p.priceText.isNotBlank() && p.contents.isNotEmpty()) }

        val activities = AppJson.decodeFromString(
            CollectiveActivitiesFile.serializer(),
            read("memorial/collective_activities.json"),
        ).activities
        assertTrue(activities.isNotEmpty())
        assertEquals(activities.size, activities.map { it.id }.toSet().size)
        activities.forEach { a -> assertTrue(a.title.isNotBlank() && a.description.isNotBlank()) }
    }

    @Test
    fun `树洞双池 JSON 合法且各自为已发布池内容`() {
        val humanText = read("treehole/human-letters.json")
        val petText = read("treehole/pet-letters.json")
        assertClean(humanText, "human-letters.json")
        assertClean(petText, "pet-letters.json")

        val humanLetters = AppJson.decodeFromString(HumanLettersFile.serializer(), humanText).letters
        val petLetters = AppJson.decodeFromString(PetLettersFile.serializer(), petText).letters

        assertTrue(humanLetters.size >= 4)
        assertTrue(petLetters.size >= 3)
        humanLetters.forEach { l ->
            assertTrue(l.id.startsWith("tlh-"))
            assertTrue(l.body.isNotBlank())
            assertEquals(TreeholeLetterState.PUBLISHED, l.state)
        }
        petLetters.forEach { l ->
            assertTrue(l.id.startsWith("tlp-"))
            assertTrue(l.body.isNotBlank())
            assertEquals(TreeholeLetterState.PUBLISHED, l.state)
        }
        // 人间池与生灵池 ID 空间互斥
        assertTrue(humanLetters.none { it.id.startsWith("tlp-") })
        assertTrue(petLetters.none { it.id.startsWith("tlh-") })
    }
}
