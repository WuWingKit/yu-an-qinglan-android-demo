/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.home

import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.data.local.SettingsRepository
import com.yuanqinglan.app.feature.home.data.HomeCatalogSource
import com.yuanqinglan.app.feature.home.data.InMemorySettingsRepository
import com.yuanqinglan.app.feature.home.model.ActivityEvent
import com.yuanqinglan.app.feature.home.model.ActivityStatus
import com.yuanqinglan.app.feature.home.model.LifeEdCourse
import com.yuanqinglan.app.feature.home.logic.MatchCatalog
import com.yuanqinglan.app.feature.home.model.NewsArticle
import com.yuanqinglan.app.testutil.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private fun newsItem(index: Int) = NewsArticle(
    id = "news-$index",
    title = "资讯标题 $index",
    source = "渝安青澜资讯",
    author = "测试专栏",
    publishTime = "2026-01-01",
    summary = "摘要 $index",
    paragraphs = listOf("第一段", "第二段"),
)

private fun newsList(size: Int = 8): List<NewsArticle> = (1..size).map(::newsItem)

/** 首页各 ViewModel 状态机测试（加载/成功/失败/重试、老年模式、报名、匹配）。 */
class HomeViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeHomeCatalogSource(
        var news: List<NewsArticle> = newsList(8),
        var failNews: Boolean = false,
        var failLifeEd: Boolean = false,
    ) : HomeCatalogSource {
        override suspend fun loadNews(): List<NewsArticle> {
            if (failNews) throw IOException("news boom")
            return news
        }

        override suspend fun loadLifeEdCourses(): List<LifeEdCourse> {
            if (failLifeEd) throw IOException("life-ed boom")
            return listOf(
                LifeEdCourse("le-1", "课程一", "家庭必修", 40, null, "摘要", null, listOf("内容")),
            )
        }

        override suspend fun loadActivities(): List<ActivityEvent> {
            return listOf(
                ActivityEvent(
                    id = "act-1",
                    title = "清明集体纪念",
                    type = "集体纪念",
                    time = "2026年4月3日",
                    location = "渝北区纪念园",
                    status = ActivityStatus.SIGNING,
                    organizer = "园区组织",
                    summary = "集体纪念活动",
                    detail = listOf("环节说明"),
                ),
            )
        }
    }

    // ---------- 老年模式开关 ----------

    @Test
    fun `elder switch writes through SettingsRepository and can be toggled twice`() = runBlocking {
        val settings: SettingsRepository = InMemorySettingsRepository()
        val vm = HomeFeedViewModel(source = FakeHomeCatalogSource(), settings = settings)
        assertFalse(settings.elderMode.first())
        assertFalse(vm.elderMode.first())

        vm.setElderMode(true)
        assertTrue(settings.elderMode.first())
        assertTrue(vm.elderMode.first())

        vm.setElderMode(false)
        assertFalse(settings.elderMode.first())
        assertFalse(vm.elderMode.first())
    }

    // ---------- 首页资讯换一换（跨 ViewModel 状态） ----------

    @Test
    fun `home shows four featured news and refresh rotates remaining four`() {
        val vm = HomeFeedViewModel(source = FakeHomeCatalogSource(), settings = InMemorySettingsRepository())

        val state = vm.newsState.value
        assertTrue(state is DemoState.Success)
        assertEquals(8, (state as DemoState.Success).value.size)

        val first = vm.featuredNews.value
        assertEquals(4, first.size)
        assertEquals(4, first.map { it.id }.toSet().size)

        vm.refreshNews()
        val second = vm.featuredNews.value
        assertEquals(4, second.size)
        val union = (first.map { it.id } + second.map { it.id }).toSet()
        assertEquals("相邻两批应覆盖全部 8 篇且互不重复", 8, union.size)

        vm.refreshNews()
        assertEquals(4, vm.featuredNews.value.size)
        assertEquals(4, vm.featuredNews.value.map { it.id }.toSet().size)
    }

    @Test
    fun `news load failure is retryable and recovers`() {
        val source = FakeHomeCatalogSource(failNews = true)
        val vm = HomeFeedViewModel(source = source, settings = InMemorySettingsRepository())
        assertTrue(vm.newsState.value is DemoState.Error)

        source.failNews = false
        vm.reloadAll()
        assertTrue(vm.newsState.value is DemoState.Success)
        assertEquals(4, vm.featuredNews.value.size)
    }

    // ---------- 资讯详情 ----------

    @Test
    fun `news detail found by id shows article`() {
        val vm = NewsDetailViewModel(source = FakeHomeCatalogSource(), newsId = "news-3")
        val state = vm.articleState.value
        assertTrue(state is DemoState.Success)
        assertEquals("news-3", (state as DemoState.Success).value.id)
    }

    @Test
    fun `news detail unknown id shows explicit error and retry can load after source change`() {
        val source = FakeHomeCatalogSource()
        val vm = NewsDetailViewModel(source = source, newsId = "missing")
        val state = vm.articleState.value
        assertTrue(state is DemoState.Error)
        assertTrue((state as DemoState.Error).message.contains("未找到"))

        source.news = source.news + newsItem(99).copy(id = "missing")
        vm.load("missing")
        assertTrue(vm.articleState.value is DemoState.Success)
    }

    // ---------- 生命教育展开 ----------

    @Test
    fun `life ed list toggles single expanded course`() {
        val vm = LifeEdViewModel(source = FakeHomeCatalogSource())
        assertTrue(vm.coursesState.value is DemoState.Success)
        assertNull(vm.expandedId.value)

        vm.toggleExpanded("le-1")
        assertEquals("le-1", vm.expandedId.value)

        vm.toggleExpanded("le-1")
        assertNull(vm.expandedId.value)
    }

    @Test
    fun `life ed failure is retryable`() {
        val source = FakeHomeCatalogSource(failLifeEd = true)
        val vm = LifeEdViewModel(source = source)
        assertTrue(vm.coursesState.value is DemoState.Error)

        source.failLifeEd = false
        vm.reload()
        assertTrue(vm.coursesState.value is DemoState.Success)
    }

    // ---------- 活动报名（可重复 + 二次确认） ----------

    @Test
    fun `activity signup toggles locally and cancel needs confirmation`() {
        val vm = ActivitiesViewModel(source = FakeHomeCatalogSource())
        assertEquals(emptySet<String>(), vm.signedIds.value)

        // 报名：直接生效
        vm.requestSignup("act-1")
        assertEquals(setOf("act-1"), vm.signedIds.value)

        // 再次点击：进入取消确认，未确认前保持已报名
        vm.requestSignup("act-1")
        assertEquals(ActivitiesViewModel.PendingSignup("act-1", wantSign = false), vm.pendingSignup.value)
        assertEquals(setOf("act-1"), vm.signedIds.value)

        // 取消确认
        vm.confirmPending()
        assertEquals(emptySet<String>(), vm.signedIds.value)
        assertNull(vm.pendingSignup.value)
    }

    @Test
    fun `activity signup can be repeated after dismiss confirm`() {
        val vm = ActivitiesViewModel(source = FakeHomeCatalogSource())
        vm.requestSignup("act-1")
        vm.requestSignup("act-1")
        vm.dismissPending()
        assertEquals(setOf("act-1"), vm.signedIds.value)

        vm.requestSignup("act-1")
        vm.confirmPending()
        assertEquals(emptySet<String>(), vm.signedIds.value)
    }

    // ---------- 智能匹配问卷 ----------

    @Test
    fun `match requires all questions answered`() {
        val vm = MatchViewModel()
        vm.answer(MatchCatalog.Q_REGION, "region-center")
        assertFalse(vm.submit())
        assertTrue(vm.submitAttempted.value)
        assertNull(vm.result.value)
    }

    @Test
    fun `match full answers produce result and restart clears state`() {
        val vm = MatchViewModel()
        vm.answer(MatchCatalog.Q_REGION, "region-center")
        vm.answer(MatchCatalog.Q_BUDGET, "budget-mid")
        vm.answer(MatchCatalog.Q_PREF, "pref-flower")
        vm.answer(MatchCatalog.Q_MEMORIAL, "mem-natural")
        assertTrue(vm.submit())
        assertEquals("flower", vm.result.value?.targetKey)

        vm.restart()
        assertNull(vm.result.value)
        assertFalse(vm.submitAttempted.value)
        assertEquals(emptyMap<String, String>(), vm.answers.value)
    }
}
