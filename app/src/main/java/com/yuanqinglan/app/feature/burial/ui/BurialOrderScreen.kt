/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.yuanqinglan.app.core.designsystem.QingLanGreen
import com.yuanqinglan.app.core.designsystem.QingLanGreenSoft
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.model.AudienceTrack
import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.EmptyState
import com.yuanqinglan.app.core.ui.InfoRow
import com.yuanqinglan.app.core.ui.LoadingState
import com.yuanqinglan.app.core.ui.PrimaryButton
import com.yuanqinglan.app.core.ui.ReferenceNote
import com.yuanqinglan.app.core.ui.SecondaryButton
import com.yuanqinglan.app.feature.burial.data.BurialRepository
import com.yuanqinglan.app.feature.burial.data.BurialServiceLocator
import com.yuanqinglan.app.feature.burial.model.BurialOrder
import com.yuanqinglan.app.feature.burial.model.BurialOrderStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 订单页 ViewModel：观察本地订单状态，可重复推进/重置本机进度。 */
class BurialOrderViewModel(
    private val orderId: String,
    private val repository: BurialRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<DemoState<BurialOrder>>(DemoState.Loading)
    val state: StateFlow<DemoState<BurialOrder>> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeOrder(orderId).collect { _state.value = it }
        }
    }

    fun advance() {
        viewModelScope.launch { repository.advanceOrder(orderId) }
    }

    /** 重置本机查看进度，可再次从头查看流程（本地状态）。 */
    fun resetProgress() {
        viewModelScope.launch { repository.resetOrderProgress(orderId) }
    }
}

/** 订单进度页（order/{orderId}）：纵向时间轴 + 订单信息 + 金额。 */
@Composable
fun BurialOrderScreen(
    orderId: String,
    navController: NavHostController,
) {
    val context = LocalContext.current
    val repository = remember(context) { BurialServiceLocator.repository(context) }
    val viewModel: BurialOrderViewModel = viewModel(
        factory = remember(orderId, repository) {
            BurialViewModelFactory { BurialOrderViewModel(orderId, repository) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppScaffold(
        title = "订单进度",
        onBack = { navController.popBackStack() },
    ) {
        when (val current = state) {
            DemoState.Loading -> LoadingState()
            is DemoState.Error -> EmptyState(
                title = "未找到该订单",
                description = current.message,
                actionLabel = "返回",
                onAction = { navController.popBackStack() },
            )
            DemoState.Empty -> EmptyState(
                title = "未找到该订单",
                description = "本机未保存该订单信息，请返回安葬服务重新查看。",
                actionLabel = "返回",
                onAction = { navController.popBackStack() },
            )
            is DemoState.Success -> OrderContent(
                order = current.value,
                onAdvance = viewModel::advance,
                onReset = viewModel::resetProgress,
            )
        }
    }
}

@Composable
private fun OrderContent(
    order: BurialOrder,
    onAdvance: () -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(12.dp))

        BurialCard {
            Column {
                Text("进度状态", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                BurialProgressTimeline(current = order.status)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = order.status.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }

        BurialSectionTitle("订单信息")
        BurialCard {
            Column {
                InfoRow(label = "订单号", value = order.orderNo)
                InfoRow(label = "服务", value = order.serviceName)
                InfoRow(label = "套餐", value = order.planTitle)
                InfoRow(
                    label = if (order.audience == AudienceTrack.HUMAN) "逝者姓名" else "宠物昵称",
                    value = order.deceasedName,
                )
                InfoRow(label = "期望日期", value = formatCnDate(order.expectDate))
                InfoRow(label = "提交时间", value = formatDateTime(order.createdAtMillis))
                InfoRow(label = "联系人", value = order.contactName)
                InfoRow(label = "联系电话", value = maskPhone(order.phone))
            }
        }

        BurialSectionTitle("费用说明")
        BurialCard {
            Column {
                InfoRow(label = "套餐金额", value = order.amountText)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "以上金额为所选套餐参考价，具体费用以服务机构最终公布为准。",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            text = if (order.status.canAdvance()) "更新本机进度" else "服务已完成",
            onClick = onAdvance,
            enabled = order.status.canAdvance(),
        )
        Spacer(Modifier.height(8.dp))
        SecondaryButton(text = "重新查看进度", onClick = onReset)
        Spacer(Modifier.height(6.dp))
        ReferenceNote(text = "订单进度仅保存在本机，用于查看流程参考。$BURIAL_REFERENCE_TEXT")
        Spacer(Modifier.height(20.dp))
    }
}

/** 纵向进度时间轴：已完成节点实心高亮，当前节点描边，后续节点浅色。 */
@Composable
private fun BurialProgressTimeline(current: BurialOrderStatus) {
    Column {
        BurialOrderStatus.entries.forEachIndexed { index, status ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(timelineColor(status, current)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${status.step}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (status.step <= current.step) Color.White else TextSecondary,
                        )
                    }
                    if (index < BurialOrderStatus.entries.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(26.dp)
                                .background(
                                    if (status.step < current.step) QingLanGreen else TimelineLineSoft,
                                ),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.padding(bottom = 10.dp)) {
                    Text(
                        text = status.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (status.step <= current.step) TextPrimary else TextSecondary,
                    )
                }
            }
        }
    }
}

private fun timelineColor(status: BurialOrderStatus, current: BurialOrderStatus): Color = when {
    status.step <= current.step -> QingLanGreen
    else -> QingLanGreenSoft
}

private val TimelineLineSoft = TextSecondary.copy(alpha = 0.35f)

private fun formatCnDate(date: LocalDate?): String =
    date?.let { "%d 年 %d 月 %d 日".format(it.year, it.monthValue, it.dayOfMonth) } ?: "—"

private fun formatDateTime(epochMillis: Long): String =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

private fun maskPhone(phone: String): String =
    if (phone.length == 11) phone.replaceRange(3, 7, "****") else phone
