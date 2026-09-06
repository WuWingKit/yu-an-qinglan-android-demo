/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.LawnSoft
import com.yuanqinglan.app.core.designsystem.QingLanGreen
import com.yuanqinglan.app.core.designsystem.QingLanGreenSoft
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.ConfirmDangerDialog
import com.yuanqinglan.app.core.ui.EmptyState
import com.yuanqinglan.app.core.ui.ErrorRetry
import com.yuanqinglan.app.core.ui.FormTextField
import com.yuanqinglan.app.core.ui.LoadingState
import com.yuanqinglan.app.core.ui.NoticeBanner
import com.yuanqinglan.app.core.ui.NoticeTone
import com.yuanqinglan.app.core.ui.PrimaryButton
import com.yuanqinglan.app.core.ui.ReferenceNote
import com.yuanqinglan.app.core.ui.SecondaryButton
import com.yuanqinglan.app.data.local.AppContainer
import com.yuanqinglan.app.feature.memorial.data.DaijiCollectiveStore
import com.yuanqinglan.app.feature.memorial.data.MemorialRepository
import com.yuanqinglan.app.feature.memorial.data.MemorialServiceLocator
import com.yuanqinglan.app.feature.memorial.model.CollectiveActivity
import com.yuanqinglan.app.feature.memorial.model.CollectiveSignup
import com.yuanqinglan.app.feature.memorial.model.DaijiOrder
import com.yuanqinglan.app.feature.memorial.model.DaijiOrderStatus
import com.yuanqinglan.app.feature.memorial.model.DaijiPackage
import com.yuanqinglan.app.feature.memorial.model.MediaKind
import com.yuanqinglan.app.feature.memorial.model.MediaRef
import com.yuanqinglan.app.feature.memorial.model.MemorialIds
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ======================================================================
// 异地代祭：付费预约 + 线下履约（独立于线上共祭）。
// ======================================================================

/** 异地代祭页流程步骤（本地状态机：套餐 → 预约信息 → 履约）。 */
private enum class DaijiFlowStep {
    PACKAGE,
    BOOKING,
    ORDER,
}

/**
 * 异地代祭 ViewModel：套餐目录、订单状态机推进/重置、履约影像归档。
 * 订单状态来自 DaijiCollectiveStore.orderState（StateFlow 实时刷新）。
 */
class DaijiViewModel(
    private val repository: MemorialRepository,
    private val daijiStore: DaijiCollectiveStore,
    val memorialId: String,
) : ViewModel() {

    private val _spaceName = MutableStateFlow<String?>(null)
    val spaceName: StateFlow<String?> = _spaceName.asStateFlow()

    private val _spaceMissing = MutableStateFlow(false)
    val spaceMissing: StateFlow<Boolean> = _spaceMissing.asStateFlow()

    private val _packages = MutableStateFlow<DemoState<List<DaijiPackage>>>(DemoState.Loading)
    val packages: StateFlow<DemoState<List<DaijiPackage>>> = _packages.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    private val _currentOrder = MutableStateFlow<DaijiOrder?>(null)
    val currentOrder: StateFlow<DaijiOrder?> = _currentOrder.asStateFlow()

    private var packagesJob: Job? = null
    private var orderJob: Job? = null

    init {
        viewModelScope.launch {
            val space = runCatching { repository.space(memorialId) }.getOrNull()
            if (space == null) {
                _spaceMissing.value = true
            } else {
                _spaceName.value = space.name
            }
        }
        loadPackages()
        orderJob = viewModelScope.launch {
            daijiStore.orderState.collect { orders ->
                val current = _currentOrder.value
                if (current != null) {
                    _currentOrder.value = orders[current.id]
                }
            }
        }
    }

    fun loadPackages() {
        packagesJob?.cancel()
        packagesJob = viewModelScope.launch {
            daijiStore.packages().collect { _packages.value = it }
        }
    }

    /** 提交代祭预约：创建本地订单并切换为当前订单。 */
    fun createOrder(
        pkg: DaijiPackage,
        entrustName: String,
        expectDate: LocalDate,
        message: String,
    ) {
        if (_submitting.value) return
        val name = _spaceName.value ?: return
        _submitting.value = true
        viewModelScope.launch {
            val order = daijiStore.createOrder(
                memorialId = memorialId,
                memorialName = name,
                pkg = pkg,
                entrustName = entrustName,
                expectDateText = dateTextOf(expectDate),
                message = message,
            )
            _currentOrder.value = order
            _submitting.value = false
        }
    }

    /** 推进当前订单履约状态。 */
    fun advanceCurrentOrder() {
        val order = _currentOrder.value ?: return
        viewModelScope.launch {
            daijiStore.advanceOrder(order.id)?.let { _currentOrder.value = it }
        }
    }

    /** 重置当前订单履约状态。 */
    fun resetCurrentOrder() {
        val order = _currentOrder.value ?: return
        viewModelScope.launch {
            daijiStore.resetOrderProgress(order.id)?.let { _currentOrder.value = it }
        }
    }

    /** 履约影像归档：写入订单，同时归档进纪念空间相册。 */
    fun archiveImages(refs: List<MediaRef>) {
        val order = _currentOrder.value ?: return
        viewModelScope.launch {
            daijiStore.archiveImagesToOrder(order.id, refs)
            refs.forEach { repository.addGalleryMedia(memorialId, it) }
        }
    }

    override fun onCleared() {
        packagesJob?.cancel()
        orderJob?.cancel()
        super.onCleared()
    }
}

@Composable
fun DaijiScreen(
    memorialId: String,
    navController: NavHostController,
) {
    val context = LocalContext.current
    val repository = remember(context) { MemorialServiceLocator.repository(context) }
    val daijiStore = remember(context) { MemorialServiceLocator.daijiCollective(context) }
    val viewModel: DaijiViewModel = viewModel(
        factory = remember(repository, daijiStore, memorialId) {
            MemorialViewModelFactory { DaijiViewModel(repository, daijiStore, memorialId) }
        },
    )
    DaijiContent(viewModel = viewModel, navController = navController)
}

@Composable
private fun DaijiContent(
    viewModel: DaijiViewModel,
    navController: NavHostController,
) {
    val spaceName by viewModel.spaceName.collectAsStateWithLifecycle()
    val spaceMissing by viewModel.spaceMissing.collectAsStateWithLifecycle()
    val packages by viewModel.packages.collectAsStateWithLifecycle()
    val submitting by viewModel.submitting.collectAsStateWithLifecycle()
    val currentOrder by viewModel.currentOrder.collectAsStateWithLifecycle()

    var stepName by rememberSaveable(viewModel.memorialId) { mutableStateOf(DaijiFlowStep.PACKAGE) }
    var chosenPackageId by rememberSaveable(viewModel.memorialId) { mutableStateOf<String?>(null) }

    // 订单创建成功后同页切换到履约状态页。
    LaunchedEffect(currentOrder?.id) {
        if (stepName == DaijiFlowStep.BOOKING && currentOrder != null) {
            stepName = DaijiFlowStep.ORDER
        }
    }

    AppScaffold(
        title = spaceName?.let { "异地代祭 · $it" } ?: "异地代祭",
        onBack = { navController.popBackStack() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NoticeBanner(
                text = "异地代祭为线下服务机构履约的付费预约服务；预约信息保存在本机，不自动提交真实个人信息。",
                tone = NoticeTone.COMPLIANCE,
            )
            if (spaceMissing) {
                EmptyState(
                    title = "纪念空间不存在",
                    description = "可能已被删除，无法发起代祭预约",
                    actionLabel = "返回",
                    onAction = { navController.popBackStack() },
                )
                return@Column
            }

            when (stepName) {
                DaijiFlowStep.PACKAGE -> DaijiPackageStep(
                    packages = packages,
                    onPick = { pkg ->
                        chosenPackageId = pkg.id
                        stepName = DaijiFlowStep.BOOKING
                    },
                    onRetry = viewModel::loadPackages,
                )
                DaijiFlowStep.BOOKING -> {
                    val chosen = (packages as? DemoState.Success)?.value
                        ?.firstOrNull { it.id == chosenPackageId }
                    if (chosen != null) {
                        DaijiBookingStep(
                            pkg = chosen,
                            submitting = submitting,
                            onBack = {
                                chosenPackageId = null
                                stepName = DaijiFlowStep.PACKAGE
                            },
                            onSubmit = { entrust, date, message ->
                                viewModel.createOrder(chosen, entrust, date, message)
                            },
                        )
                    } else {
                        EmptyState(
                            title = "请先选择代祭套餐",
                            description = "所选套餐信息未能加载，请返回重新选择",
                            actionLabel = "返回选择套餐",
                            onAction = {
                                chosenPackageId = null
                                stepName = DaijiFlowStep.PACKAGE
                            },
                        )
                    }
                }
                DaijiFlowStep.ORDER -> {
                    if (currentOrder == null) {
                        EmptyState(
                            title = "订单暂不可用",
                            description = "本地订单加载失败，请返回重新预约",
                            actionLabel = "返回重新预约",
                            onAction = { stepName = DaijiFlowStep.PACKAGE },
                        )
                    } else {
                        DaijiOrderStep(
                            order = currentOrder!!,
                            onAdvance = viewModel::advanceCurrentOrder,
                            onReset = viewModel::resetCurrentOrder,
                            onArchive = viewModel::archiveImages,
                        )
                        SecondaryButton(
                            text = "再约一次",
                            onClick = {
                                chosenPackageId = null
                                stepName = DaijiFlowStep.PACKAGE
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            ReferenceNote(text = "费用与履约以服务机构最终公布为准。")
        }
    }
}

/** 步骤一：套餐选择（Loading/Error/空/列表）。 */
@Composable
private fun DaijiPackageStep(
    packages: DemoState<List<DaijiPackage>>,
    onPick: (DaijiPackage) -> Unit,
    onRetry: () -> Unit,
) {
    when (packages) {
        DemoState.Loading -> Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
            LoadingState()
        }
        is DemoState.Error -> ErrorRetry(message = packages.message, onRetry = onRetry)
        DemoState.Empty -> EmptyState(
            title = "暂无可选套餐",
            description = "当前没有可预约的代祭套餐",
        )
        is DemoState.Success -> {
            Text(
                text = "请选择代祭套餐",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            packages.value.forEach { pkg ->
                PackageCard(
                    pkg = pkg,
                    onPick = { onPick(pkg) },
                )
            }
        }
    }
}

@Composable
private fun PackageCard(
    pkg: DaijiPackage,
    onPick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        color = SurfaceCard,
    ) {
        Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = pkg.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = pkg.priceText,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(modifier = Modifier.padding(top = 2.dp)) {
                Text(
                    text = pkg.durationText,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
            if (pkg.description.isNotBlank()) {
                Text(
                    text = pkg.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            if (pkg.contents.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    pkg.contents.forEach { content ->
                        Row(
                            modifier = Modifier.padding(top = 2.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            PrimaryButton(text = "选择此套餐", onClick = onPick)
        }
    }
}

/** 步骤二：预约信息表单。 */
@Composable
private fun DaijiBookingStep(
    pkg: DaijiPackage,
    submitting: Boolean,
    onBack: () -> Unit,
    onSubmit: (entrust: String, date: LocalDate, message: String) -> Unit,
) {
    var entrustName by rememberSaveable(pkg.id) { mutableStateOf("") }
    var expectDate by rememberSaveable(pkg.id) { mutableStateOf<LocalDate?>(null) }
    var message by rememberSaveable(pkg.id) { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    val canSubmit = !submitting &&
        entrustName.isNotBlank() &&
        expectDate != null

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimensions.CardRadius),
            color = SurfaceCard,
        ) {
            Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
                Text(
                    text = "已选套餐：${pkg.title} · ${pkg.priceText}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                if (pkg.durationText.isNotBlank()) {
                    Text(
                        text = pkg.durationText,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
        Text(
            text = "填写预约信息（不收集真实姓名与手机号）",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
        FormTextField(
            label = "委托人称呼 *",
            value = entrustName,
            onValueChange = { entrustName = it },
            supportingText = "不填真实姓名，如：女儿",
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .background(QingLanGreenSoft, RoundedCornerShape(AppDimensions.CompactRadius))
                .clickable { showDatePicker = true }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Event,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = expectDate?.let { dateTextOf(it) } ?: "选择期望代祭日期",
                style = MaterialTheme.typography.bodyMedium,
                color = if (expectDate != null) TextPrimary else TextSecondary,
                modifier = Modifier.weight(1f),
            )
            if (expectDate == null) {
                Text(
                    text = "必填",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                Text(
                    text = "更改",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        FormTextField(
            label = "给家人的留言（选填）",
            value = message,
            onValueChange = { message = it },
        )
        PrimaryButton(
            text = "提交代祭预约",
            enabled = canSubmit,
            onClick = {
                val date = expectDate ?: return@PrimaryButton
                onSubmit(entrustName, date, message)
            },
        )
        SecondaryButton(text = "返回选择套餐", onClick = onBack)
    }

    if (showDatePicker) {
        MemorialDatePickerDialog(
            title = "选择期望代祭日期",
            initialDate = expectDate,
            onConfirm = { date ->
                expectDate = date
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

/** 步骤三：订单履约状态页（推进 / 重置 / 影像归档）。 */
@Composable
private fun DaijiOrderStep(
    order: DaijiOrder,
    onAdvance: () -> Unit,
    onReset: () -> Unit,
    onArchive: (List<MediaRef>) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var viewer by remember { mutableStateOf<MediaRef?>(null) }
    var archiving by remember { mutableStateOf(false) }
    var archiveMessage by remember { mutableStateOf<String?>(null) }

    val pickArchiveImage = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        archiving = false
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = readUriBytes(context, uri)
            if (bytes == null || bytes.isEmpty()) {
                archiveMessage = "读取照片失败，请重试"
                return@launch
            }
            val saved = AppContainer.fileStorage.saveImage(bytes)
            onArchive(
                listOf(
                    MediaRef(
                        id = MemorialIds.next("arch"),
                        kind = MediaKind.IMAGE_FILE,
                        value = saved.toString(),
                        name = uri.lastPathSegment ?: "履约影像",
                        sizeBytes = bytes.size.toLong(),
                    ),
                ),
            )
            archiveMessage = null
        }
    }

    LaunchedEffect(archiving) {
        if (archiving) pickArchiveImage.launch("image/*")
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimensions.CardRadius),
            color = SurfaceCard,
        ) {
            Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
                Text(
                    text = "预约单：${order.orderNo}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Text(
                    text = "${order.packageTitle} · ${order.priceText}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = "委托人：${order.entrustName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "期望日期：${order.expectDateText}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
                if (order.message.isNotBlank()) {
                    Text(
                        text = "留言：${order.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Text(
                    text = "${order.status.title}：${order.status.description}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        DaijiOrderProgressSteps(current = order.status)

        PrimaryButton(
            text = "更新履约状态",
            enabled = order.status.canAdvance(),
            onClick = onAdvance,
        )
        SecondaryButton(
            text = "重置状态",
            onClick = onReset,
        )

        // ---- 履约影像归档 ----
        archiveMessage?.let { message ->
            NoticeBanner(text = message, tone = NoticeTone.WARNING)
        }
        NoticeBanner(
            text = "履约影像归档到本单的同时，也会存入该纪念空间的相册。",
            tone = NoticeTone.INFO,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.AddAPhoto,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "履约影像归档（${order.archiveImages.size}）",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = { archiving = true },
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            ) {
                Text("归档照片")
            }
        }
        if (order.archiveImages.isEmpty()) {
            Text(
                text = "还没有归档影像，履约完成后可上传照片留档。",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        } else {
            order.archiveImages.chunked(3).forEach { rowImages ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowImages.forEach { ref ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(AppDimensions.CompactRadius))
                                .background(SurfaceCard)
                                .clickable { viewer = ref },
                            contentAlignment = Alignment.Center,
                        ) {
                            MediaThumb(
                                ref = ref,
                                contentDescription = "履约影像归档照片",
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    repeat(3 - rowImages.size) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
            Text(
                text = "点击缩略图可放大查看。",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
        }
    }

    viewer?.let { photo ->
        FullscreenMediaDialog(
            photo = photo,
            description = "履约影像归档照片",
            onDismiss = { viewer = null },
        )
    }
}

/** 横向三步履约进度（当前步骤高亮）。 */
@Composable
private fun DaijiOrderProgressSteps(current: DaijiOrderStatus) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DaijiOrderStatus.entries.forEachIndexed { index, status ->
            val reached = index <= current.ordinal
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            color = if (reached) QingLanGreen else SurfaceCard,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (reached) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                    }
                }
                Text(
                    text = status.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (reached) TextPrimary else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

// ======================================================================
// 线上集体共祭：免费公益活动（与异地代祭完全独立）。
// ======================================================================

/**
 * 线上共祭历史 ViewModel：活动目录 + 我的报名记录（本地幂等）。
 */
class JitiHistoryViewModel(
    private val daijiStore: DaijiCollectiveStore,
) : ViewModel() {

    private val _activities = MutableStateFlow<DemoState<List<CollectiveActivity>>>(DemoState.Loading)
    val activities: StateFlow<DemoState<List<CollectiveActivity>>> = _activities.asStateFlow()

    private val _signups = MutableStateFlow<List<CollectiveSignup>>(emptyList())
    val signups: StateFlow<List<CollectiveSignup>> = _signups.asStateFlow()

    private var activitiesJob: Job? = null
    private var signupsJob: Job? = null

    init {
        loadActivities()
        signupsJob = viewModelScope.launch {
            daijiStore.mySignups().collect { _signups.value = it }
        }
    }

    fun loadActivities() {
        activitiesJob?.cancel()
        activitiesJob = viewModelScope.launch {
            daijiStore.activities().collect { _activities.value = it }
        }
    }

    fun isSignedUp(activityId: String): Boolean =
        _signups.value.any { it.activityId == activityId }

    /** 报名（幂等，同一活动仅保留一条）。 */
    fun signUp(activity: CollectiveActivity) {
        viewModelScope.launch {
            daijiStore.signUp(activity)
        }
    }

    /** 取消报名；不存在时静默返回。 */
    fun cancelSignup(activityId: String) {
        viewModelScope.launch {
            daijiStore.cancelSignup(activityId)
        }
    }

    override fun onCleared() {
        activitiesJob?.cancel()
        signupsJob?.cancel()
        super.onCleared()
    }
}

@Composable
fun JitiHistoryScreen(navController: NavHostController) {
    val context = LocalContext.current
    val daijiStore = remember(context) { MemorialServiceLocator.daijiCollective(context) }
    val viewModel: JitiHistoryViewModel = viewModel(
        factory = remember(daijiStore) {
            MemorialViewModelFactory { JitiHistoryViewModel(daijiStore) }
        },
    )
    JitiHistoryContent(viewModel = viewModel, navController = navController)
}

@Composable
private fun JitiHistoryContent(
    viewModel: JitiHistoryViewModel,
    navController: NavHostController,
) {
    val activitiesState = viewModel.activities.collectAsStateWithLifecycle().value
    val signups = viewModel.signups.collectAsStateWithLifecycle().value

    var confirmTarget by remember { mutableStateOf<CollectiveActivity?>(null) }
    var cancelTarget by remember { mutableStateOf<String?>(null) }

    AppScaffold(
        title = "线上集体共祭",
        onBack = { navController.popBackStack() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NoticeBanner(
                text = "线上集体共祭是免费公益活动，与异地代祭（付费预约）相互独立。",
                tone = NoticeTone.INFO,
            )
            when (activitiesState) {
                DemoState.Loading -> Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                    LoadingState()
                }
                is DemoState.Error -> ErrorRetry(
                    message = activitiesState.message,
                    onRetry = viewModel::loadActivities,
                )
                DemoState.Empty -> EmptyState(
                    title = "当前没有开放的活动",
                    description = "稍后再来看看",
                )
                is DemoState.Success -> {
                    activitiesState.value.forEach { activity ->
                        CollectiveActivityCard(
                            activity = activity,
                            signedUp = viewModel.isSignedUp(activity.id),
                            onSignUp = { confirmTarget = activity },
                            onCancel = { cancelTarget = activity.id },
                        )
                    }
                }
            }

            // ---- 我的共祭历史 ----
            MemorialSectionTitle(text = "我的共祭历史")
            if (signups.isEmpty()) {
                MemorialEmptyHint("还没有参与记录", "报名公益活动后，参与记录会显示在这里")
            } else {
                signups.forEach { signup ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppDimensions.CardRadius),
                        color = SurfaceCard,
                    ) {
                        Row(
                            modifier = Modifier.padding(AppDimensions.CardPadding),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VolunteerActivism,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = signup.activityTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "参与于 ${formatDateTimeText(signup.joinedAtMillis)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    confirmTarget?.let { activity ->
        ConfirmDangerDialog(
            title = "报名参与活动",
            message = "确定报名「${activity.title}」吗？本活动为免费公益活动，报名信息仅保存在本机。",
            confirmLabel = "确认报名",
            onConfirm = {
                viewModel.signUp(activity)
                confirmTarget = null
            },
            onDismiss = { confirmTarget = null },
        )
    }
    cancelTarget?.let { activityId ->
        ConfirmDangerDialog(
            title = "取消报名",
            message = "确定取消该活动的报名吗？",
            confirmLabel = "取消报名",
            onConfirm = {
                viewModel.cancelSignup(activityId)
                cancelTarget = null
            },
            onDismiss = { cancelTarget = null },
        )
    }
}

@Composable
private fun CollectiveActivityCard(
    activity: CollectiveActivity,
    signedUp: Boolean,
    onSignUp: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        color = SurfaceCard,
    ) {
        Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = RoundedCornerShape(AppDimensions.CompactRadius),
                    color = LawnSoft,
                ) {
                    Text(
                        text = "公益免费",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Campaign,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = activity.dateText,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                if (activity.location.isNotBlank()) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = activity.location,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
            }
            if (activity.description.isNotBlank()) {
                Text(
                    text = activity.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            if (signedUp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = QingLanGreen,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "已报名",
                        style = MaterialTheme.typography.bodyMedium,
                        color = QingLanGreen,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    ) {
                        Text("取消报名")
                    }
                }
            } else {
                SecondaryButton(text = "报名参与", onClick = onSignUp)
            }
        }
    }
}
