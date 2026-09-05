/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.EmptyState
import com.yuanqinglan.app.core.ui.ErrorRetry
import com.yuanqinglan.app.core.ui.LoadingState
import com.yuanqinglan.app.core.ui.NoticeBanner
import com.yuanqinglan.app.core.ui.NoticeTone
import com.yuanqinglan.app.core.ui.PrimaryButton
import com.yuanqinglan.app.core.ui.SecondaryButton
import com.yuanqinglan.app.data.local.AppContainer
import com.yuanqinglan.app.feature.memorial.data.MemorialRepository
import com.yuanqinglan.app.feature.memorial.data.MemorialServiceLocator
import com.yuanqinglan.app.feature.memorial.model.MemorialLike
import com.yuanqinglan.app.feature.memorial.model.MemorialStory
import com.yuanqinglan.app.feature.memorial.model.StoryAlbumExport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 生命故事时间线 ViewModel：按纪念空间 ID 观察对应轨空间（只读公共形态）。
 * 页面只消费 [MemorialLike.sortedStories]，不持有任何跨轨集合。
 */
class MemorialStoryViewModel(
    private val repository: MemorialRepository,
    val memorialId: String,
) : ViewModel() {

    private val _state = MutableStateFlow<DemoState<MemorialLike>>(DemoState.Loading)
    val state: StateFlow<DemoState<MemorialLike>> = _state.asStateFlow()

    private var collectJob: kotlinx.coroutines.Job? = null

    init {
        startCollect()
    }

    private fun startCollect() {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            repository.observeSpace(memorialId).collect { _state.value = it }
        }
    }

    fun refresh() {
        startCollect()
    }

    override fun onCleared() {
        collectJob?.cancel()
        super.onCleared()
    }
}

/** 一次导出成功的结果（文件名 + 落盘 Uri）。 */
private data class ExportedAlbumInfo(
    val fileName: String,
    val uriText: String,
)

/**
 * 生命故事时间线：节点按时间升序展示；支持新增节点与整册导出到本机。
 * 路由：memorial-story/{memorialId}
 */
@Composable
fun MemorialStoryScreen(
    memorialId: String,
    navController: NavHostController,
) {
    val context = LocalContext.current
    val repository = remember(context) { MemorialServiceLocator.repository(context) }
    remember(context) { AppContainer.init(context.applicationContext) }
    val viewModel: MemorialStoryViewModel = viewModel(
        factory = remember(repository, memorialId) {
            MemorialViewModelFactory { MemorialStoryViewModel(repository, memorialId) }
        },
    )
    MemorialStoryContent(
        viewModel = viewModel,
        memorialId = memorialId,
        navController = navController,
    )
}

@Composable
private fun MemorialStoryContent(
    viewModel: MemorialStoryViewModel,
    memorialId: String,
    navController: NavHostController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val current = state) {
        DemoState.Loading -> AppScaffold(
            title = "生命故事",
            onBack = { navController.popBackStack() },
        ) {
            LoadingState()
        }
        is DemoState.Error -> AppScaffold(
            title = "生命故事",
            onBack = { navController.popBackStack() },
        ) {
            ErrorRetry(message = current.message, onRetry = { viewModel.refresh() })
        }
        DemoState.Empty -> AppScaffold(
            title = "生命故事",
            onBack = { navController.popBackStack() },
        ) {
            NoStoryState(
                onAdd = { navController.navigate(MemorialRoutes.storyAdd(memorialId)) },
            )
        }
        is DemoState.Success -> {
            val space = current.value
            StoryTimelineContent(
                spaceName = space.name,
                stories = space.sortedStories(),
                onBack = { navController.popBackStack() },
                onAdd = { navController.navigate(MemorialRoutes.storyAdd(space.id)) },
            )
        }
    }
}

@Composable
private fun StoryTimelineContent(
    spaceName: String,
    stories: List<MemorialStory>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exported by remember { mutableStateOf<ExportedAlbumInfo?>(null) }
    var exportFailed by remember { mutableStateOf(false) }
    val exporting = remember { mutableStateOf(false) }

    fun exportAlbum() {
        if (exporting.value) return
        exporting.value = true
        exportFailed = false
        scope.launch {
            val result = runCatching {
                val text = StoryAlbumExport.build(spaceName, stories)
                val fileName = "story_album_${System.currentTimeMillis()}.txt"
                val uri = AppContainer.fileStorage.save(
                    bytes = text.toByteArray(Charsets.UTF_8),
                    directoryName = "memorial",
                    fileName = fileName,
                )
                ExportedAlbumInfo(fileName = fileName, uriText = uri.toString())
            }
            exporting.value = false
            result.onSuccess { exported = it }
                .onFailure { exportFailed = true }
        }
    }

    AppScaffold(
        title = "$spaceName · 生命故事",
        onBack = onBack,
    ) {
        if (stories.isEmpty()) {
            NoStoryState(onAdd = onAdd)
            return@AppScaffold
        }

        Column(modifier = Modifier.fillMaxSize()) {
            if (exportFailed) {
                NoticeBanner(
                    text = "纪念册导出失败，请稍后重试。",
                    tone = NoticeTone.WARNING,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    PrimaryButton(text = "新增故事节点", onClick = onAdd)
                }
                item {
                    SecondaryButton(
                        text = "导出纪念册",
                        onClick = { exportAlbum() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Text(
                        text = "时间线 · ${stories.size} 个节点",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                    )
                }
                items(stories, key = { it.id }) { story ->
                    StoryNodeCard(story)
                }
            }
        }
    }

    exported?.let { info ->
        AlertDialog(
            onDismissRequest = { exported = null },
            title = { Text("纪念册已导出到本机", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Text("文件名：${info.fileName}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "保存位置：${info.uriText}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { exported = null }) {
                    Text("知道了")
                }
            },
        )
    }
}

/** 无节点时的统一空态：说明按时间排序的语义 + 新增节点动作。 */
@Composable
private fun NoStoryState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        EmptyState(
            title = "还没有故事节点",
            description = "把想记住的时光写成节点，它们会按时间排成一条线。",
            actionLabel = "新增节点",
            onAction = onAdd,
        )
    }
}

/** 单个故事节点卡：时间文本/标题/正文/可选配图。 */
@Composable
private fun StoryNodeCard(story: MemorialStory) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        color = SurfaceCard,
    ) {
        Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
            Text(
                text = story.dateText,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            if (story.title.isNotBlank()) {
                Text(
                    text = story.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (story.body.isNotBlank()) {
                Text(
                    text = story.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            if (story.image != null) {
                Spacer(Modifier.height(10.dp))
                MediaThumb(
                    ref = story.image,
                    contentDescription = "故事节点配图",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .clip(RoundedCornerShape(AppDimensions.CompactRadius)),
                )
            }
        }
    }
}
