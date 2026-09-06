/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.WavingHand
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.yuanqinglan.app.core.designsystem.AppBackground
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.model.AudienceTrack
import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.core.ui.AudienceSegment
import com.yuanqinglan.app.core.ui.EmptyState
import com.yuanqinglan.app.core.ui.ErrorRetry
import com.yuanqinglan.app.core.ui.LoadingState
import com.yuanqinglan.app.core.ui.NoticeBanner
import com.yuanqinglan.app.core.ui.NoticeTone
import com.yuanqinglan.app.feature.memorial.data.MemorialRepository
import com.yuanqinglan.app.feature.memorial.data.AiFlowGate
import com.yuanqinglan.app.feature.memorial.data.MemorialServiceLocator
import com.yuanqinglan.app.feature.memorial.model.HumanMemorial
import com.yuanqinglan.app.feature.memorial.model.MemorialLike
import com.yuanqinglan.app.feature.memorial.model.MemorialTrack
import com.yuanqinglan.app.feature.memorial.model.PetMemorial
import com.yuanqinglan.app.navigation.AppRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 追忆 Tab 根：人/宠双轨独立列表 + 新建纪念空间 + 模块功能入口。
 * 双轨数据来自两个独立状态流；入口含生命故事/祭扫时光/信件/AI 追忆/异地代祭/共祭历史
 * （需要纪念空间的功能先经 [SpacePickerDialog] 选择目标，绝不跨轨复用）。
 */
class MemorialHomeViewModel(
    private val repository: MemorialRepository,
) : ViewModel() {

    private val _track = MutableStateFlow(AudienceTrack.HUMAN)
    val track: StateFlow<AudienceTrack> = _track.asStateFlow()

    private val _humanState = MutableStateFlow<DemoState<List<HumanMemorial>>>(DemoState.Loading)
    val humanState: StateFlow<DemoState<List<HumanMemorial>>> = _humanState.asStateFlow()

    private val _petState = MutableStateFlow<DemoState<List<PetMemorial>>>(DemoState.Loading)
    val petState: StateFlow<DemoState<List<PetMemorial>>> = _petState.asStateFlow()

    private var humanJob: Job? = null
    private var petJob: Job? = null

    init {
        refresh()
    }

    fun selectTrack(newTrack: AudienceTrack) {
        _track.value = newTrack
    }

    fun refresh() {
        humanJob?.cancel()
        petJob?.cancel()
        humanJob = viewModelScope.launch {
            repository.humanSpaces().collect { _humanState.value = it }
        }
        petJob = viewModelScope.launch {
            repository.petSpaces().collect { _petState.value = it }
        }
    }

    /** 当前轨列表（渲染/选择器）。 */
    fun spacesOf(track: AudienceTrack): List<MemorialLike> = when (track) {
        AudienceTrack.HUMAN -> (_humanState.value as? DemoState.Success)?.value ?: emptyList()
        AudienceTrack.PET -> (_petState.value as? DemoState.Success)?.value ?: emptyList()
    }

    override fun onCleared() {
        humanJob?.cancel()
        petJob?.cancel()
        super.onCleared()
    }
}

@Composable
fun MemorialHomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val repository = remember(context) { MemorialServiceLocator.repository(context) }
    val viewModel: MemorialHomeViewModel = viewModel(
        factory = remember(repository) {
            MemorialViewModelFactory { MemorialHomeViewModel(repository) }
        },
    )
    MemorialHomeContent(viewModel = viewModel, navController = navController)
}

/** 需要纪念空间作为目标的功能动作。 */
private enum class SpaceAction {
    STORY,
    LETTER,
    AI,
    DAIJI,
}

@Composable
private fun MemorialHomeContent(
    viewModel: MemorialHomeViewModel,
    navController: NavHostController,
) {
    val track by viewModel.track.collectAsStateWithLifecycle()
    val humanState by viewModel.humanState.collectAsStateWithLifecycle()
    val petState by viewModel.petState.collectAsStateWithLifecycle()

    var pickAction by remember { mutableStateOf<SpaceAction?>(null) }
    val listState = rememberLazyListState()

    val spaces = viewModel.spacesOf(track)

    fun openCreate() {
        navController.navigate(AppRoute.MEMORIAL_CREATE.route)
    }

    fun performSpaceAction(action: SpaceAction, space: MemorialLike) {
        when (action) {
            SpaceAction.STORY -> navController.navigate(MemorialRoutes.story(space.id))
            SpaceAction.LETTER -> navController.navigate(MemorialRoutes.letterWrite(space.id))
            SpaceAction.AI -> {
                AiFlowGate.prepare(space.id)
                navController.navigate(AppRoute.AI_ETHICS.route)
            }
            SpaceAction.DAIJI -> navController.navigate(MemorialRoutes.daiji(space.id))
        }
    }

    /** 生命故事/写信/AI 追忆/异地代祭先选择目标纪念空间（保证入口不串轨、不误操作）。 */
    fun openSpacePickedAction(action: SpaceAction) {
        if (spaces.isEmpty()) {
            pickAction = action
        } else if (spaces.size == 1) {
            performSpaceAction(action, spaces.first())
        } else {
            pickAction = action
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimensions.PageHorizontal),
        ) {
            Spacer(Modifier.height(12.dp))
            Text("云端追忆", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "为珍视的人与伙伴，留一座可以安放思念的空间",
                color = TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(10.dp))
            AudienceSegment(selected = track, onSelect = viewModel::selectTrack)
            Spacer(Modifier.height(8.dp))
            if (track == AudienceTrack.PET) {
                NoticeBanner(
                    text = "宠物纪念空间仅用于私人纪念，内容与人类纪念空间数据相互独立。",
                    tone = NoticeTone.COMPLIANCE,
                )
                Spacer(Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "纪念空间",
                    style = MaterialTheme.typography.titleLarge,
                    color = com.yuanqinglan.app.core.designsystem.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = ::openCreate) {
                    Text("新建纪念空间")
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (val state = if (track == AudienceTrack.HUMAN) humanState else petState) {
                DemoState.Loading -> LoadingState()
                is DemoState.Error -> ErrorRetry(message = state.message, onRetry = viewModel::refresh)
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = AppDimensions.PageHorizontal,
                        end = AppDimensions.PageHorizontal,
                        bottom = 28.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val currentSpaces = spaces
                    if (currentSpaces.isEmpty()) {
                        item {
                            EmptyState(
                                title = "还没有纪念空间",
                                description = "为你珍视的人或伙伴创建第一座纪念空间",
                                actionLabel = "新建纪念空间",
                                onAction = ::openCreate,
                            )
                        }
                    } else {
                        items(currentSpaces, key = { it.id }) { space ->
                            MemorialSpaceCard(
                                memorial = space,
                                vocab = MemorialVocab.ofMemorialId(space.id),
                                onClick = {
                                    val target = if (space.id.startsWith(MemorialTrack.PREFIX_HUMAN)) {
                                        MemorialRoutes.detail(space.id)
                                    } else {
                                        MemorialRoutes.petMemorial(space.id)
                                    }
                                    navController.navigate(target)
                                },
                            )
                        }
                    }

                    item {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "更多追忆服务",
                            style = MaterialTheme.typography.titleLarge,
                            color = com.yuanqinglan.app.core.designsystem.TextPrimary,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                        )
                    }
                    item {
                        // 双列功能入口：祭扫时光/线上共祭无目标空间，其余先选空间。
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MemorialEntryTile(
                                icon = Icons.Outlined.History,
                                label = "祭扫时光",
                                onClick = { navController.navigate(AppRoute.MEMORIAL_TIME.route) },
                                modifier = Modifier.weight(1f),
                            )
                            MemorialEntryTile(
                                icon = Icons.Outlined.WavingHand,
                                label = "线上共祭",
                                onClick = { navController.navigate(AppRoute.COLLECTIVE_HISTORY.route) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MemorialEntryTile(
                                icon = Icons.Outlined.AutoStories,
                                label = "生命故事",
                                onClick = { openSpacePickedAction(SpaceAction.STORY) },
                                modifier = Modifier.weight(1f),
                            )
                            MemorialEntryTile(
                                icon = Icons.Outlined.Email,
                                label = "写信",
                                onClick = { openSpacePickedAction(SpaceAction.LETTER) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MemorialEntryTile(
                                icon = Icons.Outlined.SelfImprovement,
                                label = "AI 追忆",
                                onClick = { openSpacePickedAction(SpaceAction.AI) },
                                modifier = Modifier.weight(1f),
                            )
                            MemorialEntryTile(
                                icon = Icons.Outlined.Park,
                                label = "异地代祭",
                                onClick = { openSpacePickedAction(SpaceAction.DAIJI) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    item {
                        NoticeBanner(
                            text = "AI 追忆仅处理你授权上传的私人影像；异地代祭与线上共祭是两类相互独立的服务。",
                            tone = NoticeTone.COMPLIANCE,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }

    pickAction?.let { action ->
        SpacePickerDialog(
            title = "选择纪念空间",
            spaces = spaces,
            onPick = { space ->
                pickAction = null
                performSpaceAction(action, space)
            },
            onDismiss = { pickAction = null },
        )
    }
}
