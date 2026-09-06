/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.yuanqinglan.app.core.designsystem.FlowerSoft
import com.yuanqinglan.app.core.designsystem.LawnSoft
import com.yuanqinglan.app.core.designsystem.QingLanGreenSoft
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.model.AudienceTrack
import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.EmptyState
import com.yuanqinglan.app.core.ui.ErrorRetry
import com.yuanqinglan.app.core.ui.LoadingState
import com.yuanqinglan.app.core.ui.PrimaryButton
import com.yuanqinglan.app.feature.burial.data.BurialFlowStore
import com.yuanqinglan.app.feature.burial.data.BurialPlanContext
import com.yuanqinglan.app.feature.burial.data.BurialRepository
import com.yuanqinglan.app.feature.burial.data.BurialServiceLocator
import com.yuanqinglan.app.feature.burial.model.BurialPlan
import com.yuanqinglan.app.feature.burial.model.PlanTier
import com.yuanqinglan.app.feature.burial.model.plansOfService
import com.yuanqinglan.app.feature.burial.model.serviceIdFor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 套餐页 ViewModel：人类/宠物套餐强类型分流，重试可复现加载/失败状态。 */
class BurialPlanViewModel(
    private val repository: BurialRepository,
) : ViewModel() {

    private val _humanPlans = MutableStateFlow<DemoState<List<BurialPlan>>>(DemoState.Loading)
    val humanPlans: StateFlow<DemoState<List<BurialPlan>>> = _humanPlans.asStateFlow()

    private val _petPlans = MutableStateFlow<DemoState<List<BurialPlan>>>(DemoState.Loading)
    val petPlans: StateFlow<DemoState<List<BurialPlan>>> = _petPlans.asStateFlow()

    private var humanJob: Job? = null
    private var petJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        humanJob?.cancel()
        petJob?.cancel()
        humanJob = viewModelScope.launch {
            repository.humanPlans().collect { _humanPlans.value = it }
        }
        petJob = viewModelScope.launch {
            repository.petPlans().collect { _petPlans.value = it }
        }
    }

    override fun onCleared() {
        humanJob?.cancel()
        petJob?.cancel()
        super.onCleared()
    }
}

/** 套餐列表：按当前轨道/模式上下文展示对应服务的套餐（基础款在前）。 */
@Composable
fun BurialPlanScreen(navController: NavHostController) {
    val context = LocalContext.current
    val repository = remember(context) { BurialServiceLocator.repository(context) }
    val viewModel: BurialPlanViewModel = viewModel(
        factory = remember(repository) { BurialViewModelFactory { BurialPlanViewModel(repository) } },
    )
    val ctx by BurialFlowStore.context.collectAsStateWithLifecycle()
    val humanPlans by viewModel.humanPlans.collectAsStateWithLifecycle()
    val petPlans by viewModel.petPlans.collectAsStateWithLifecycle()

    val title = if (ctx.track == AudienceTrack.HUMAN) "安葬套餐" else "宠物安葬套餐"
    AppScaffold(
        title = title,
        onBack = { navController.popBackStack() },
    ) {
        when (ctx.track) {
            AudienceTrack.HUMAN -> BurialPlanList(
                context = ctx,
                state = humanPlans,
                onRetry = viewModel::refresh,
                onReserve = { planId -> navController.navigate(BurialRoutes.planForm(planId)) },
            )
            AudienceTrack.PET -> BurialPlanList(
                context = ctx,
                state = petPlans,
                onRetry = viewModel::refresh,
                onReserve = { planId -> navController.navigate(BurialRoutes.planForm(planId)) },
            )
        }
    }
}

@Composable
private fun BurialPlanList(
    context: BurialPlanContext,
    state: DemoState<List<BurialPlan>>,
    onRetry: () -> Unit,
    onReserve: (String) -> Unit,
) {
    when (state) {
        DemoState.Loading -> LoadingState()
        is DemoState.Error -> ErrorRetry(message = state.message, onRetry = onRetry)
        DemoState.Empty -> EmptyState(title = "暂无套餐信息", description = "套餐信息整理中，请稍后重试。")
        is DemoState.Success -> {
            val plans = state.value.plansOfService(serviceIdFor(context.track, context.mode))
            if (plans.isEmpty()) {
                EmptyState(
                    title = "暂无对应套餐",
                    description = "该服务的套餐信息整理中，请稍后再查看。",
                )
                return
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 12.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = "${context.mode.label}相关套餐",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                    )
                }
                items(count = plans.size, key = { plans[it].id }) { index ->
                    PlanCard(plan = plans[index], onReserve = { onReserve(plans[index].id) })
                }
                item {
                    Text(
                        text = BURIAL_REFERENCE_TEXT,
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: BurialPlan,
    onReserve: () -> Unit,
) {
    BurialCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = plan.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                BurialTag(
                    text = plan.tier.label,
                    container = if (plan.tier == PlanTier.BASIC) {
                        QingLanGreenSoft
                    } else {
                        FlowerSoft
                    },
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = plan.priceText,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            Spacer(Modifier.height(10.dp))
            BurialCheckList(items = plan.contents, accent = planAccent(plan))
            Spacer(Modifier.height(12.dp))
            PrimaryButton(text = "预约此套餐", onClick = onReserve)
        }
    }
}

/** 套餐内容圆点主题色：按强类型轨道区分（宠物浅黄 / 人类浅绿），不依赖字符串判断。 */
private fun planAccent(plan: BurialPlan): Color = when (plan.audience) {
    AudienceTrack.HUMAN -> QingLanGreenSoft
    AudienceTrack.PET -> LawnSoft
}
