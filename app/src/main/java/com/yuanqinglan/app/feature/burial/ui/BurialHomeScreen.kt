/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.yuanqinglan.app.core.ui.ServiceSceneCard
import com.yuanqinglan.app.feature.burial.data.BurialRepository
import com.yuanqinglan.app.feature.burial.data.BurialServiceLocator
import com.yuanqinglan.app.feature.burial.model.BurialMode
import com.yuanqinglan.app.feature.burial.model.HumanBurialService
import com.yuanqinglan.app.feature.burial.model.PetBurialService
import com.yuanqinglan.app.feature.burial.model.PetCompliance
import com.yuanqinglan.app.navigation.AppRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 安葬首页 ViewModel：人/宠双轨各自独立的强类型状态流，
 * 切换轨道只改变展示位，数据源互不串联；两轨可独立加载/失败/重试。
 */
class BurialHomeViewModel(
    private val repository: BurialRepository,
) : ViewModel() {

    private val _track = MutableStateFlow(AudienceTrack.HUMAN)
    val track: StateFlow<AudienceTrack> = _track.asStateFlow()

    private val _humanState =
        MutableStateFlow<DemoState<List<HumanBurialService>>>(DemoState.Loading)
    val humanState: StateFlow<DemoState<List<HumanBurialService>>> = _humanState.asStateFlow()

    private val _petState =
        MutableStateFlow<DemoState<List<PetBurialService>>>(DemoState.Loading)
    val petState: StateFlow<DemoState<List<PetBurialService>>> = _petState.asStateFlow()

    private var humanJob: Job? = null
    private var petJob: Job? = null

    init {
        refreshAll()
    }

    fun selectTrack(newTrack: AudienceTrack) {
        _track.value = newTrack
    }

    /** 重新加载双轨数据（重试入口，可重复操作）。 */
    fun refreshAll() {
        humanJob?.cancel()
        petJob?.cancel()
        humanJob = viewModelScope.launch {
            repository.humanServices().collect { _humanState.value = it }
        }
        petJob = viewModelScope.launch {
            repository.petServices().collect { _petState.value = it }
        }
    }

    override fun onCleared() {
        humanJob?.cancel()
        petJob?.cancel()
        super.onCleared()
    }
}

@Composable
fun BurialHomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val repository = remember(context) { BurialServiceLocator.repository(context) }
    val viewModel: BurialHomeViewModel = viewModel(
        factory = remember(repository) {
            BurialViewModelFactory { BurialHomeViewModel(repository) }
        },
    )
    BurialHomeContent(viewModel = viewModel, navController = navController)
}

@Composable
private fun BurialHomeContent(
    viewModel: BurialHomeViewModel,
    navController: NavHostController,
) {
    val track by viewModel.track.collectAsStateWithLifecycle()
    val humanState by viewModel.humanState.collectAsStateWithLifecycle()
    val petState by viewModel.petState.collectAsStateWithLifecycle()

    // 双轨各自独立滚动位，切换轨道互不影响。
    val humanListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val petListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

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
            Spacer(Modifier.height(14.dp))
            Text("生态安葬", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "人类生态葬式与宠物独立园区服务",
                color = TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(12.dp))
            AudienceSegment(
                selected = track,
                onSelect = viewModel::selectTrack,
            )
            Spacer(Modifier.height(8.dp))
        }

        // 宠物轨顶部固定合规说明：无害化处理前置、无民政补贴、独立园区、场地隔离。
        if (track == AudienceTrack.PET) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimensions.PageHorizontal),
            ) {
                NoticeBanner(
                    text = PetCompliance.bannerText,
                    tone = NoticeTone.COMPLIANCE,
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (track) {
                AudienceTrack.HUMAN -> HumanTrackContent(
                    state = humanState,
                    listState = humanListState,
                    onRetry = viewModel::refreshAll,
                    onOpen = { mode -> navController.navigate(humanDetailRoute(mode)) },
                )
                AudienceTrack.PET -> PetTrackContent(
                    state = petState,
                    listState = petListState,
                    onRetry = viewModel::refreshAll,
                    onOpen = { mode -> navController.navigate(BurialRoutes.petDetail(mode)) },
                )
            }
        }
    }
}

@Composable
private fun HumanTrackContent(
    state: DemoState<List<HumanBurialService>>,
    listState: LazyListState,
    onRetry: () -> Unit,
    onOpen: (BurialMode) -> Unit,
) {
    when (state) {
        DemoState.Loading -> LoadingState()
        is DemoState.Error -> ErrorRetry(message = state.message, onRetry = onRetry)
        DemoState.Empty -> EmptyState(title = "暂无服务信息", description = "服务信息整理中，请稍后重试。")
        is DemoState.Success -> LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = AppDimensions.PageHorizontal,
                top = 10.dp,
                end = AppDimensions.PageHorizontal,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items = state.value, key = { it.id }) { service ->
                ServiceSceneCard(
                    imageRes = BurialArtwork.imageRes(service.image),
                    title = service.name,
                    subtitle = service.subtitle,
                    price = service.priceRange,
                    onClick = { onOpen(service.mode) },
                )
            }
            item { DisclosureFootNote() }
        }
    }
}

@Composable
private fun PetTrackContent(
    state: DemoState<List<PetBurialService>>,
    listState: LazyListState,
    onRetry: () -> Unit,
    onOpen: (BurialMode) -> Unit,
) {
    when (state) {
        DemoState.Loading -> LoadingState()
        is DemoState.Error -> ErrorRetry(message = state.message, onRetry = onRetry)
        DemoState.Empty -> EmptyState(title = "暂无服务信息", description = "服务信息整理中，请稍后重试。")
        is DemoState.Success -> LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = AppDimensions.PageHorizontal,
                top = 10.dp,
                end = AppDimensions.PageHorizontal,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items = state.value, key = { it.id }) { service ->
                ServiceSceneCard(
                    imageRes = BurialArtwork.imageRes(service.image),
                    title = service.name,
                    subtitle = service.subtitle,
                    price = service.priceRange,
                    onClick = { onOpen(service.mode) },
                )
            }
            item { DisclosureFootNote() }
        }
    }
}

@Composable
private fun DisclosureFootNote() {
    Text(
        text = BURIAL_REFERENCE_TEXT,
        color = TextSecondary,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(top = 6.dp),
    )
}

private fun humanDetailRoute(mode: BurialMode): String = when (mode) {
    BurialMode.TREE -> AppRoute.TREE.route
    BurialMode.FLOWER -> AppRoute.FLOWER.route
    BurialMode.LAWN -> AppRoute.GRASS.route
}
