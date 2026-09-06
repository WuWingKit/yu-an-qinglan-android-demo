/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.data.local.SettingsRepository
import com.yuanqinglan.app.feature.home.data.HomeCatalogSource
import com.yuanqinglan.app.feature.home.data.loadListState
import com.yuanqinglan.app.feature.home.logic.MatchCatalog
import com.yuanqinglan.app.feature.home.logic.MatchEngine
import com.yuanqinglan.app.feature.home.logic.NewsShuffleEngine
import com.yuanqinglan.app.feature.home.model.ActivityEvent
import com.yuanqinglan.app.feature.home.model.LifeEdCourse
import com.yuanqinglan.app.feature.home.model.MatchQuestion
import com.yuanqinglan.app.feature.home.model.MatchRecommendation
import com.yuanqinglan.app.feature.home.model.NewsArticle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 首页聚合 ViewModel：轮播下资讯（随机 4 条 + 换一换）、生命教育预览、
 * 近期活动预览、老年模式开关。资讯换一换使用 [NewsShuffleEngine] 保证
 * 单批不重复且全部文章都有机会出现。
 */
class HomeFeedViewModel(
    private val source: HomeCatalogSource,
    settings: SettingsRepository,
) : ViewModel() {

    private val _newsState = MutableStateFlow<DemoState<List<NewsArticle>>>(DemoState.Loading)
    val newsState: StateFlow<DemoState<List<NewsArticle>>> = _newsState.asStateFlow()

    private val _lifeEdState = MutableStateFlow<DemoState<List<LifeEdCourse>>>(DemoState.Loading)
    val lifeEdState: StateFlow<DemoState<List<LifeEdCourse>>> = _lifeEdState.asStateFlow()

    private val _activitiesState = MutableStateFlow<DemoState<List<ActivityEvent>>>(DemoState.Loading)
    val activitiesState: StateFlow<DemoState<List<ActivityEvent>>> = _activitiesState.asStateFlow()

    /** 当前展示的 4 条资讯（随机不重复）。 */
    private val _featuredNews = MutableStateFlow<List<NewsArticle>>(emptyList())
    val featuredNews: StateFlow<List<NewsArticle>> = _featuredNews.asStateFlow()

    /** 换一换引擎；在资讯加载成功后按实际条数创建。 */
    private var shuffleEngine: NewsShuffleEngine? = null

    /** 老年模式开关状态（与全局 SettingsRepository 同源）。 */
    val elderMode: StateFlow<Boolean> = settings.elderMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )

    private val settingsRepository = settings

    init {
        reloadAll()
    }

    fun setElderMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setElderMode(enabled) }
    }

    /** 重新加载全部区块（重试入口）。 */
    fun reloadAll() {
        loadNews()
        loadLifeEd()
        loadActivities()
    }

    fun refreshNews() {
        val list = (_newsState.value as? DemoState.Success)?.value ?: return
        val engine = shuffleEngine
        if (engine == null) {
            val created = NewsShuffleEngine(list.size, NEWS_BATCH_SIZE)
            shuffleEngine = created
            _featuredNews.value = created.nextBatch().map { list[it] }
        } else {
            _featuredNews.value = engine.nextBatch().map { list[it] }
        }
    }

    private fun loadNews() {
        viewModelScope.launch {
            _newsState.value = DemoState.Loading
            _newsState.value = loadListState({ source.loadNews() }, "资讯加载失败，请稍后重试。")
            val list = (_newsState.value as? DemoState.Success)?.value.orEmpty()
            if (list.isNotEmpty()) {
                val engine = NewsShuffleEngine(list.size, NEWS_BATCH_SIZE)
                shuffleEngine = engine
                _featuredNews.value = engine.nextBatch().map { list[it] }
            }
        }
    }

    private fun loadLifeEd() {
        viewModelScope.launch {
            _lifeEdState.value = DemoState.Loading
            _lifeEdState.value = loadListState({ source.loadLifeEdCourses() }, "生命教育内容加载失败，请稍后重试。")
        }
    }

    private fun loadActivities() {
        viewModelScope.launch {
            _activitiesState.value = DemoState.Loading
            _activitiesState.value = loadListState({ source.loadActivities() }, "活动加载失败，请稍后重试。")
        }
    }

    companion object {
        const val NEWS_BATCH_SIZE = 4
    }
}

/** 生命教育列表页 ViewModel（页内展开详情）。 */
class LifeEdViewModel(
    source: HomeCatalogSource,
) : ViewModel() {

    private val _coursesState = MutableStateFlow<DemoState<List<LifeEdCourse>>>(DemoState.Loading)
    val coursesState: StateFlow<DemoState<List<LifeEdCourse>>> = _coursesState.asStateFlow()

    /** 当前展开的课程 id；null 表示全部收起。 */
    private val _expandedId = MutableStateFlow<String?>(null)
    val expandedId: StateFlow<String?> = _expandedId.asStateFlow()

    private val catalogSource = source

    init {
        reload()
    }

    fun toggleExpanded(courseId: String) {
        _expandedId.value = if (_expandedId.value == courseId) null else courseId
    }

    fun reload() {
        viewModelScope.launch {
            _coursesState.value = DemoState.Loading
            _coursesState.value = loadListState(
                { catalogSource.loadLifeEdCourses() },
                "生命教育内容加载失败，请稍后重试。",
            )
        }
    }
}

/** 近期活动 ViewModel：列表 / 详情 / 本地报名状态（可重复操作，二次确认）。 */
class ActivitiesViewModel(
    source: HomeCatalogSource,
) : ViewModel() {

    private val _listState = MutableStateFlow<DemoState<List<ActivityEvent>>>(DemoState.Loading)
    val listState: StateFlow<DemoState<List<ActivityEvent>>> = _listState.asStateFlow()

    /** 当前查看详情的活动 id；null 表示列表页。 */
    private val _detailId = MutableStateFlow<String?>(null)
    val detailId: StateFlow<String?> = _detailId.asStateFlow()

    /** 本地报名状态：已报名活动 id 集合。 */
    private val _signedIds = MutableStateFlow<Set<String>>(emptySet())
    val signedIds: StateFlow<Set<String>> = _signedIds.asStateFlow()

    /** 等待二次确认的报名/取消请求。 */
    data class PendingSignup(val activityId: String, val wantSign: Boolean)

    private val _pending = MutableStateFlow<PendingSignup?>(null)
    val pendingSignup: StateFlow<PendingSignup?> = _pending.asStateFlow()

    private val catalogSource = source

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            _listState.value = DemoState.Loading
            _listState.value = loadListState({ catalogSource.loadActivities() }, "活动加载失败，请稍后重试。")
        }
    }

    fun openDetail(activityId: String) {
        _detailId.value = activityId
    }

    fun closeDetail() {
        _detailId.value = null
    }

    /** 请求报名（未报名时）或取消报名（已报名时先弹二次确认）。 */
    fun requestSignup(activityId: String) {
        if (_signedIds.value.contains(activityId)) {
            _pending.value = PendingSignup(activityId, wantSign = false)
        } else {
            _signedIds.update { it + activityId }
        }
    }

    fun confirmPending() {
        val pending = _pending.value ?: return
        if (!pending.wantSign) {
            _signedIds.update { it - pending.activityId }
        } else {
            _signedIds.update { it + pending.activityId }
        }
        _pending.value = null
    }

    fun dismissPending() {
        _pending.value = null
    }
}

/** 资讯详情 ViewModel：按 id 加载完整正文。 */
class NewsDetailViewModel(
    source: HomeCatalogSource,
    private val newsId: String,
) : ViewModel() {

    private val _articleState = MutableStateFlow<DemoState<NewsArticle>>(DemoState.Loading)
    val articleState: StateFlow<DemoState<NewsArticle>> = _articleState.asStateFlow()

    private val catalogSource = source

    init {
        load(newsId)
    }

    fun load(id: String) {
        viewModelScope.launch {
            _articleState.value = DemoState.Loading
            _articleState.value = try {
                val article = catalogSource.loadNews().firstOrNull { it.id == id }
                if (article == null) {
                    DemoState.Error("未找到该资讯，可能已下架，请返回重试。")
                } else {
                    DemoState.Success(article)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                DemoState.Error("资讯加载失败，请稍后重试。")
            }
        }
    }
}

/** 智能匹配 ViewModel：问卷作答 → 本地规则推荐 → 可重新匹配。 */
class MatchViewModel : ViewModel() {

    /** 题目固定来自本地规则常量。 */
    val questions: List<MatchQuestion> = MatchCatalog.questions

    private val _answers = MutableStateFlow<Map<String, String>>(emptyMap())
    val answers: StateFlow<Map<String, String>> = _answers.asStateFlow()

    private val _result = MutableStateFlow<MatchRecommendation?>(null)
    val result: StateFlow<MatchRecommendation?> = _result.asStateFlow()

    /** 是否尝试过提交（用于展示“请完成全部问题”提示）。 */
    private val _submitAttempted = MutableStateFlow(false)
    val submitAttempted: StateFlow<Boolean> = _submitAttempted.asStateFlow()

    fun answer(questionId: String, optionValue: String) {
        _answers.update { it + (questionId to optionValue) }
    }

    fun submit(): Boolean {
        _submitAttempted.value = true
        val unanswered = questions.any { _answers.value[it.id].isNullOrBlank() }
        if (unanswered) return false
        _result.value = MatchEngine.recommend(_answers.value)
        return true
    }

    fun restart() {
        _answers.value = emptyMap()
        _result.value = null
        _submitAttempted.value = false
    }
}
