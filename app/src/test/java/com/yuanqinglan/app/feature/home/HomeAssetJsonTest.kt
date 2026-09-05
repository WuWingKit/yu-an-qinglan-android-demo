/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.home

import com.yuanqinglan.app.data.local.AppJson
import com.yuanqinglan.app.feature.home.model.ActivityEvent
import com.yuanqinglan.app.feature.home.model.LifeEdCourse
import com.yuanqinglan.app.feature.home.model.NewsArticle
import java.io.File
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 首页本地 JSON 数据完整性测试：以与运行时 DemoAssetLoader 相同的
 * 解码器（AppJson + ListSerializer）读取 assets/demo/home 目录下的 JSON 文件，
 * 校验条数、id 唯一性、字段完整性与禁用文案。
 */
class HomeAssetJsonTest {

    private fun readAssetJson(name: String): String {
        val file = File("src/main/assets/demo/home/$name")
        assertTrue("找不到 $name（cwd=${File(".").absolutePath}）", file.exists())
        return file.readText(Charsets.UTF_8)
    }

    private val bannedWords = listOf("演示", "假数据", "原型", "纯前端")

    private fun assertNoBannedWording(text: String, where: String) {
        bannedWords.forEach { word ->
            assertTrue("$where 出现禁用文案[$word]", !text.contains(word))
        }
    }

    @Test
    fun `news json contains 8 articles with unique ids and full text`() {
        val text = readAssetJson("news.json")
        assertNoBannedWording(text, "news.json")

        val news: List<NewsArticle> = AppJson.decodeFromString(
            ListSerializer(NewsArticle.serializer()),
            text,
        )
        assertEquals(8, news.size)
        assertEquals(8, news.map { it.id }.toSet().size)
        news.forEach { article ->
            assertTrue("${article.id} 标题为空", article.title.isNotBlank())
            assertTrue("${article.id} 来源为空", article.source.isNotBlank())
            assertTrue("${article.id} 发布时间为空", article.publishTime.isNotBlank())
            assertTrue("${article.id} 正文段落过少", article.paragraphs.size >= 3)
        }
        // 至少两篇带可配图键（与素材清单对应）。
        assertTrue(news.count { it.imageKey != null } >= 2)
    }

    @Test
    fun `life ed json contains unique courses with duration`() {
        val text = readAssetJson("life-ed.json")
        assertNoBannedWording(text, "life-ed.json")

        val courses: List<LifeEdCourse> = AppJson.decodeFromString(
            ListSerializer(LifeEdCourse.serializer()),
            text,
        )
        assertEquals(6, courses.size)
        assertEquals(6, courses.map { it.id }.toSet().size)
        courses.forEach { course ->
            assertTrue("${course.id} 标题为空", course.title.isNotBlank())
            assertTrue("${course.id} 时长非法", course.durationMinutes > 0)
            assertTrue("${course.id} 缺少正文", course.paragraphs.isNotEmpty())
        }
    }

    @Test
    fun `activities json contains unique activities with valid status`() {
        val text = readAssetJson("activities.json")
        assertNoBannedWording(text, "activities.json")

        val activities: List<ActivityEvent> = AppJson.decodeFromString(
            ListSerializer(ActivityEvent.serializer()),
            text,
        )
        assertEquals(6, activities.size)
        assertEquals(6, activities.map { it.id }.toSet().size)
        activities.forEach { event ->
            assertTrue("${event.id} 标题为空", event.title.isNotBlank())
            assertTrue("${event.id} 时间为空", event.time.isNotBlank())
            assertTrue("${event.id} 地点为空", event.location.isNotBlank())
            assertTrue("${event.id} 详情为空", event.detail.isNotEmpty())
        }
        // 覆盖"报名中/即将开始/已结束"三类状态，保证卡片状态可展示。
        val statusKinds = activities.map { it.status }.toSet()
        assertTrue(statusKinds.size >= 3)
    }
}
