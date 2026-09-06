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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.yuanqinglan.app.feature.burial.data.BurialRepository
import com.yuanqinglan.app.feature.burial.data.BurialServiceLocator
import com.yuanqinglan.app.feature.burial.model.BurialMode
import com.yuanqinglan.app.feature.burial.model.PetBurialService
import com.yuanqinglan.app.feature.burial.model.PetCompliance
import com.yuanqinglan.app.feature.burial.model.petByMode
import com.yuanqinglan.app.navigation.AppRoute
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 独立宠物园区的参观预约（本地状态，可重复预约/取消）。 */
data class ParkVisitState(
    val requested: Boolean = false,
    val date: LocalDate? = null,
)

/** 宠物独立园区 ViewModel：按 mode 定位宠物服务 + 本地参观预约状态。 */
class PetParkViewModel(
    private val mode: BurialMode,
    private val repository: BurialRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<DemoState<PetBurialService>>(DemoState.Loading)
    val state: StateFlow<DemoState<PetBurialService>> = _state.asStateFlow()

    private val _visit = MutableStateFlow(ParkVisitState())
    val visit: StateFlow<ParkVisitState> = _visit.asStateFlow()

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

    fun requestVisit(date: LocalDate) {
        _visit.value = ParkVisitState(requested = true, date = date)
    }

    fun cancelVisit() {
        _visit.value = ParkVisitState()
    }

    override fun onCleared() {
        loadJob?.cancel()
        super.onCleared()
    }
}

/**
 * 宠物独立园区（pet-park?mode=）：园区环境 + 隔离说明 + 参观预约（本地状态），
 * 图片与文案随 mode 切换。
 */
@Composable
fun PetParkScreen(
    mode: BurialMode,
    navController: NavHostController,
) {
    val context = LocalContext.current
    val repository = remember(context) { BurialServiceLocator.repository(context) }
    val viewModel: PetParkViewModel = viewModel(
        factory = remember(mode, repository) {
            BurialViewModelFactory { PetParkViewModel(mode, repository) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val visit by viewModel.visit.collectAsStateWithLifecycle()

    AppScaffold(
        title = "宠物独立园区",
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
                    title = "未找到园区信息",
                    description = "园区信息暂不可用，请返回后重试。",
                )
                is DemoState.Success -> {
                    val service = current.value
                    PetParkContent(
                        service = service,
                        visit = visit,
                        onRequestVisit = { date -> viewModel.requestVisit(date) },
                        onCancelVisit = viewModel::cancelVisit,
                        onPlan = {
                            navController.navigate(AppRoute.PLAN.route)
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
private fun PetParkContent(
    service: PetBurialService,
    visit: ParkVisitState,
    onRequestVisit: (LocalDate) -> Unit,
    onCancelVisit: () -> Unit,
    onPlan: () -> Unit,
    onNavigate: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        ) {
            NoticeBanner(text = PetCompliance.bannerText, tone = NoticeTone.COMPLIANCE)
        }

        BurialSceneImage(
            imageRes = BurialArtwork.imageRes(service.image),
            contentDescription = "${service.name}独立园区景观",
        )
        Spacer(Modifier.height(14.dp))
        Text(service.name, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(
            text = service.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
        )

        BurialSectionTitle("园区环境")
        BurialBodyText(service.parkIntro)

        BurialSectionTitle("场地隔离说明")
        BurialCheckList(
            items = listOf(
                "宠物安葬区独立成园，与人类安葬区分区管理。",
                "人宠区域物理隔离，通道与养护互不交叉。",
                "园区按节令统一养护，家属按开放时间探望。",
            ),
            accent = service.mode.accentContainer(),
        )

        BurialSectionTitle("参观预约")
        VisitBooking(
            serviceName = service.name,
            visit = visit,
            onRequestVisit = onRequestVisit,
            onCancelVisit = onCancelVisit,
        )
        Spacer(Modifier.height(6.dp))
        ReferenceNote(text = "参观时间以园区当日开放安排为准。$BURIAL_REFERENCE_TEXT")
        Spacer(Modifier.height(16.dp))

        PrimaryButton(text = "查看宠物套餐", onClick = onPlan)
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onNavigate,
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimensions.MinimumTouchTarget),
        ) {
            Text("园区导览")
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun VisitBooking(
    serviceName: String,
    visit: ParkVisitState,
    onRequestVisit: (LocalDate) -> Unit,
    onCancelVisit: () -> Unit,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var pendingDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val pendingDate = pendingDay?.let(LocalDate::ofEpochDay)

    if (visit.requested && visit.date != null) {
        BurialCard {
            Column {
                Text(
                    text = "已登记参观：$serviceName · ${formatCnDate(visit.date)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "该登记仅保存在本机，用于查看参观安排参考。",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(8.dp))
                SecondaryButton(text = "取消本次预约", onClick = onCancelVisit)
            }
        }
        return
    }

    BurialCard {
        Column {
            Text(
                text = "如需到访，可选择期望日期并登记（本地状态，不提交外部）。",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = { showDatePicker = true }) {
                Text(if (pendingDate == null) "选择参观日期" else formatCnDate(pendingDate))
            }
            Spacer(Modifier.height(10.dp))
            PrimaryButton(
                text = "预约参观",
                onClick = { pendingDate?.let(onRequestVisit) },
                enabled = pendingDate != null,
            )
        }
    }

    if (showDatePicker) {
        BurialDatePickerDialog(
            title = "选择参观日期",
            initialDate = pendingDate ?: LocalDate.now(),
            selectable = { day -> !day.isBefore(LocalDate.now()) },
            onConfirm = { day ->
                pendingDay = day.toEpochDay()
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

private fun formatCnDate(date: LocalDate): String =
    "%d 年 %d 月 %d 日".format(date.year, date.monthValue, date.dayOfMonth)
