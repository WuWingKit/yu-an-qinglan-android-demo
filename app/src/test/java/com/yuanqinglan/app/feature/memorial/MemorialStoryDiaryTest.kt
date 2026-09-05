/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial

import com.yuanqinglan.app.feature.memorial.data.HumanMemorialStore
import com.yuanqinglan.app.feature.memorial.model.AlbumSelect
import com.yuanqinglan.app.feature.memorial.model.HumanMemorial
import com.yuanqinglan.app.feature.memorial.model.MediaKind
import com.yuanqinglan.app.feature.memorial.model.MediaRef
import com.yuanqinglan.app.feature.memorial.model.MemorialDiaryEntry
import com.yuanqinglan.app.feature.memorial.model.MemorialIds
import com.yuanqinglan.app.feature.memorial.model.MemorialStory
import com.yuanqinglan.app.feature.memorial.model.StoryAlbumExport
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 生命故事：新增节点立即按时间插入正确位置；导出纪念册必须包含新增节点。
 * 相册：多选状态切换/裁剪；多选删除只删选中项。
 * 日记：附件新增/编辑回填/删除闭环。
 */
class MemorialStoryDiaryTest {

    private val humanId = "hm-t-1"

    private fun store(vararg stories: MemorialStory) = HumanMemorialStore(
        seedProvider = {
            listOf(
                HumanMemorial(
                    id = humanId,
                    name = "陈永昌",
                    relation = "外公",
                    intro = "",
                    createdAtMillis = 10L,
                    stories = stories.toList(),
                ),
            )
        },
    )

    private fun story(id: String, dateMillis: Long, title: String, body: String = "正文-$id") =
        MemorialStory(
            id = id,
            title = title,
            dateMillis = dateMillis,
            dateText = "1996年1月",
            body = body,
            image = null,
        )

    @Test
    fun `新增节点插入正确时间位置并按时间升序`() = runTest {
        val s = store(
            story("s-old", 1000L, "较早的事"),
            story("s-new", 3000L, "后来的事"),
        )
        assertTrue(s.addStory(humanId, story("s-mid", 2000L, "中间的事")))

        val space = s.space(humanId)!!
        assertEquals(listOf("s-old", "s-mid", "s-new"), space.sortedStories().map { it.id })
        assertEquals(
            listOf(1000L, 2000L, 3000L),
            space.sortedStories().map { it.dateMillis },
        )
    }

    @Test
    fun `导出纪念册按时间排序且包含新增节点全文`() = runTest {
        val s = store(story("s-old", 1000L, "较早的事", "老的正文"))
        assertTrue(s.addStory(humanId, story("s-new", 2000L, "新增节点", "新增节点正文")))

        val space = s.space(humanId)!!
        val text = StoryAlbumExport.build(space.name, space.sortedStories())

        assertTrue("导出应包含新增节点标题", text.contains("新增节点"))
        assertTrue("导出应包含新增节点正文", text.contains("新增节点正文"))
        assertTrue("导出应包含既有节点", text.contains("较早的事"))
        // 时间顺序：较早节点出现在新增节点之前
        val oldIndex = text.indexOf("较早的事")
        val newIndex = text.indexOf("新增节点")
        assertTrue("导出顺序应遵守时间线", oldIndex in 0 until newIndex)
    }

    @Test
    fun `空故事导出有兜底文案`() = runTest {
        val text = StoryAlbumExport.build("某人", emptyList())
        assertTrue(text.contains("暂无故事节点"))
    }

    @Test
    fun `相册多选切换与裁剪`() {
        var selection: Set<String> = emptySet()
        selection = AlbumSelect.toggle(selection, "a")
        selection = AlbumSelect.toggle(selection, "b")
        assertEquals(setOf("a", "b"), selection)
        selection = AlbumSelect.toggle(selection, "a")
        assertEquals(setOf("b"), selection)
        selection = AlbumSelect.prune(selection, setOf("x", "y"))
        assertEquals(emptySet<String>(), selection)
    }

    @Test
    fun `多选批量删除只删除选中项`() = runTest {
        val s = store()
        val photos = listOf(
            MediaRef(MemorialIds.next("ph"), MediaKind.IMAGE_FILE, "file:///1", "1", 1),
            MediaRef(MemorialIds.next("ph"), MediaKind.IMAGE_FILE, "file:///2", "2", 2),
            MediaRef(MemorialIds.next("ph"), MediaKind.IMAGE_FILE, "file:///3", "3", 3),
        )
        photos.forEach { s.addGalleryMedia(humanId, it) }
        assertEquals(3, s.space(humanId)?.gallery?.size)

        val removed = s.removeGalleryMediaBatch(humanId, setOf(photos[0].id, photos[2].id))
        assertEquals(2, removed)
        val remaining = s.space(humanId)!!.gallery.map { it.id }
        assertEquals(listOf(photos[1].id), remaining)

        // 不存在项不会误删
        assertEquals(0, s.removeGalleryMediaBatch(humanId, setOf("not-exist")))
    }

    @Test
    fun `日记附件新增编辑回填删除闭环`() = runTest {
        val s = store()
        val imageA = MediaRef("img-a", MediaKind.IMAGE_FILE, "file:///a", "a.jpg", 10)
        val audio = MediaRef("aud-a", MediaKind.AUDIO_FILE, "file:///b", "b.m4a", 20)

        val entry = MemorialDiaryEntry(
            id = "d-1",
            title = "第一篇",
            body = "今天有点想你",
            createdAtMillis = 1L,
            updatedAtMillis = 1L,
            images = listOf(imageA),
            audio = audio,
        )
        assertTrue(s.addDiaryEntry(humanId, entry))
        val loaded = s.space(humanId)!!.diaryById("d-1")!!
        assertTrue(loaded.hasAttachment())
        assertEquals(listOf("img-a"), loaded.images.map { it.id })
        assertEquals("aud-a", loaded.audio?.id)

        // 编辑回填：替换正文并移除音频
        val edited = entry.copy(
            body = "改了正文",
            updatedAtMillis = 2L,
            images = entry.images + MediaRef("img-b", MediaKind.IMAGE_FILE, "file:///c", "c.jpg", 30),
            audio = null,
        )
        assertTrue(s.updateDiaryEntry(humanId, edited))
        val reloaded = s.space(humanId)!!.diaryById("d-1")!!
        assertEquals("改了正文", reloaded.body)
        assertNull(reloaded.audio)
        assertEquals(listOf("img-a", "img-b"), reloaded.images.map { it.id })

        // 不存在的条目更新/删除返回 false
        assertFalse(s.updateDiaryEntry(humanId, entry.copy(id = "nope", title = "x", body = "x")))
        assertFalse(s.removeDiaryEntry(humanId, "nope"))

        assertTrue(s.removeDiaryEntry(humanId, "d-1"))
        assertTrue(s.space(humanId)!!.diary.isEmpty())
    }
}
