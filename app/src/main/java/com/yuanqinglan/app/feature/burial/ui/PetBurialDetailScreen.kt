/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.model.AudienceTrack
import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.EmptyState
import com.yuanqinglan.app.core.ui.ErrorRetry
import com.yuanqinglan.app.core.ui.LoadingState
import com.yuanqinglan.app.core.ui.NoticeBanner
import com.yuanqinglan.app.core.ui.NoticeTone
import com.yuanqinglan.app.core.ui.PrimaryButton
import com.yuanqinglan.app.core.ui.ReferenceNote
import com.yuanqinglan.app.core.ui.SecondaryButton
import com.yuanqinglan.app.feature.burial.data.BurialFlowStore
import com.yuanqinglan.app.feature.burial.data.BurialRepository
import com.yuanqinglan.app.feature.burial.data.BurialServiceLocator
import com.yuanqinglan.app.feature.burial.model.BurialMode
import com.yuanqinglan.app.feature.burial.model.PetBurialService
import com.yuanqinglan.app.feature.burial.model.PetCompliance
import com.yuanqinglan.app.feature.burial.model.petByMode
import com.yuanqinglan.app.navigation.AppRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 宠物葬式详情 ViewModel：按受控枚举 mode 从宠物强类型数据流中定位对应服务。 */
class PetBurialDetailViewModel(
    private val mode: BurialMode,
    private val repository: BurialRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<DemoState<PetBurialService>>(DemoState.Loading)
    val state: StateFlow<DemoState<PetBurialService>> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.petServices().collect { demo ->
                _state.value = when (demo) {
                    DemoState.Loading -> DemoState.Loading
                    is DemoState.Error -> demo
                    DemoState.Empty -> DemoState.Empty
                    is DemoState.Success -> demo.value.petByMode(mode)
                        ?.let { DemoState.Success(it) }
                        ?: DemoState.Empty
                }
            }
        }
    }

    override fun onCleared() {
        loadJob?.cancel()
        super.onCleared()
    }
}

/**
 * 宠物葬式详情：单参数化模板承载 pet-tree?mode=TREE|FLOWER|LAWN，
 * mode 决定对应图片/文案；非法参数已在路由层回退 TREE，此处 mode 恒为合法枚举。
 */
@Composable
fun PetBurialDetailScreen(
    mode: BurialMode,
    navController: NavHostController,
) {
    val context = LocalContext.current
    val repository = remember(context) { BurialServiceLocator.repository(context) }
    val viewModel: PetBurialDetailViewModel = viewModel(
        factory = remember(mode, repository) {
            BurialViewModelFactory { PetBurialDetailViewModel(mode, repository) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    val title = when (state) {
        is DemoState.Success -> (state as DemoState.Success<PetBurialService>).value.name
        else -> "宠物${mode.label}"
    }

    AppScaffold(
        title = title,
        onBack = { navController.popBackStack() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            when (val current = state) {
                DemoState.Loading -> LoadingState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                )
                is DemoState.Error -> ErrorRetry(
                    message = current.message,
                    onRetry = viewModel::refresh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                )
                DemoState.Empty -> EmptyState(
                    title = "未找到该服务",
                    description = "服务信息暂不可用，请返回后重试。",
                )
                is DemoState.Success -> {
                    val service = current.value
                    PetServiceDetailContent(
                        service = service,
                        onPlan = {
                            BurialFlowStore.setPlanContext(AudienceTrack.PET, service.mode)
                            navController.navigate(AppRoute.PLAN.route)
                        },
                        onReserve = {
                            navController.navigate(BurialRoutes.planForm(service.defaultPlanId))
                        },
                        onPark = {
                            navController.navigate(BurialRoutes.petPark(service.mode))
                        },
                        onNavigate = {
                            navController.navigate(AppRoute.NAVIGATE.route)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PetServiceDetailContent(
    service: PetBurialService,
    onPlan: () -> Unit,
    onReserve: () -> Unit,
    onPark: () -> Unit,
    onNavigate: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Spacer(Modifier.height(12.dp))

        // 首屏固定合规说明（无害化处理前置 / 无民政补贴 / 独立园区 / 场地隔离）。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        ) {
            NoticeBanner(
                text = PetCompliance.bannerText,
                tone = NoticeTone.COMPLIANCE,
            )
        }

        BurialSceneImage(
            imageRes = BurialArtwork.imageRes(service.image),
            contentDescription = "${service.name}园区景观",
        )
        Spacer(Modifier.height(14.dp))
        Text(service.name, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(
            text = service.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
        )
        Spacer(Modifier.height(10.dp))
        BurialPriceTag(text = service.priceRange)

        BurialSectionTitle("服务说明")
        BurialBodyText(service.description)

        BurialSectionTitle("办理流程")
        BurialProcessList(items = service.process, accent = service.mode.accentContainer())
        Spacer(Modifier.height(16.dp))

        PrimaryButton(text = "宠物套餐", onClick = onPlan)
        Spacer(Modifier.height(10.dp))
        SecondaryButton(text = "立即预约", onClick = onReserve)
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onPark,
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimensions.MinimumTouchTarget),
        ) {
            Icon(
                imageVector = Icons.Outlined.Pets,
                contentDescription = null,
                modifier = Modifier.padding(end = 6.dp),
            )
            Text("宠物独立园区")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onNavigate,
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimensions.MinimumTouchTarget),
        ) {
            Icon(
                imageVector = Icons.Outlined.Map,
                contentDescription = null,
                modifier = Modifier.padding(end = 6.dp),
            )
            Text("园区导览")
        }
        Spacer(Modifier.height(6.dp))
        ReferenceNote(text = BURIAL_REFERENCE_TEXT)
        Spacer(Modifier.height(20.dp))
    }
}
