/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.model.AudienceTrack
import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.EmptyState
import com.yuanqinglan.app.core.ui.ErrorRetry
import com.yuanqinglan.app.core.ui.LoadingState
import com.yuanqinglan.app.core.ui.PrimaryButton
import com.yuanqinglan.app.core.ui.ReferenceNote
import com.yuanqinglan.app.feature.burial.data.BurialOrderDraft
import com.yuanqinglan.app.feature.burial.data.BurialRepository
import com.yuanqinglan.app.feature.burial.data.BurialServiceLocator
import com.yuanqinglan.app.feature.burial.model.BurialFormField
import com.yuanqinglan.app.feature.burial.model.BurialFormReport
import com.yuanqinglan.app.feature.burial.model.BurialFormRules
import com.yuanqinglan.app.feature.burial.model.BurialPlan
import com.yuanqinglan.app.feature.burial.model.HumanPlanFormInput
import com.yuanqinglan.app.feature.burial.model.PetPlanFormInput
import com.yuanqinglan.app.feature.burial.model.planById
import com.yuanqinglan.app.feature.burial.model.quote
import com.yuanqinglan.app.feature.burial.model.serviceDisplayName
import com.yuanqinglan.app.navigation.AppRoute
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 预约表单 ViewModel：先按 planId 在两份强类型套餐流中定位套餐（audience 由套餐类型决定），
 * 再按轨道展开人类/宠物专属表单字段；提交前本地校验，成功后生成本地订单号并交给订单页。
 * 所有信息仅在本机流转，不提交外部。
 */
class BurialPlanFormViewModel(
    private val planId: String,
    private val repository: BurialRepository,
) : ViewModel() {

    private val _planState = MutableStateFlow<DemoState<BurialPlan>>(DemoState.Loading)
    val planState: StateFlow<DemoState<BurialPlan>> = _planState.asStateFlow()

    private val _humanInput = MutableStateFlow(HumanPlanFormInput())
    val humanInput: StateFlow<HumanPlanFormInput> = _humanInput.asStateFlow()

    private val _petInput = MutableStateFlow(PetPlanFormInput())
    val petInput: StateFlow<PetPlanFormInput> = _petInput.asStateFlow()

    private val _report = MutableStateFlow<BurialFormReport?>(null)
    val report: StateFlow<BurialFormReport?> = _report.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    private val _createdOrderId = MutableStateFlow<String?>(null)
    val createdOrderId: StateFlow<String?> = _createdOrderId.asStateFlow()

    private val _photoPicked = MutableStateFlow(false)
    val photoPicked: StateFlow<Boolean> = _photoPicked.asStateFlow()

    private val _prepaidYears = MutableStateFlow(0)
    val prepaidYears: StateFlow<Int> = _prepaidYears.asStateFlow()

    private val _selectedAddOnIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedAddOnIds: StateFlow<Set<String>> = _selectedAddOnIds.asStateFlow()

    private val _applySubsidy = MutableStateFlow(true)
    val applySubsidy: StateFlow<Boolean> = _applySubsidy.asStateFlow()

    private var loadJob: Job? = null
    private var submitJob: Job? = null

    init {
        reload()
    }

    fun reload() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _planState.value = locatePlan()
        }
    }

    // ---------- 输入 ----------

    fun updateHuman(transform: (HumanPlanFormInput) -> HumanPlanFormInput) {
        _humanInput.update(transform)
        _report.value = null
    }

    fun updatePet(transform: (PetPlanFormInput) -> PetPlanFormInput) {
        _petInput.update(transform)
        _report.value = null
    }

    fun setPhotoPicked(picked: Boolean) {
        _photoPicked.value = picked
    }

    fun setPrepaidYears(years: Int) {
        _prepaidYears.value = years
    }

    fun toggleAddOn(id: String) {
        _selectedAddOnIds.update { selected ->
            if (id in selected) selected - id else selected + id
        }
    }

    fun setApplySubsidy(apply: Boolean) {
        _applySubsidy.value = apply
    }

    /** 重新填写：清空当前轨表单与校验结果。 */
    fun resetForm() {
        _humanInput.value = HumanPlanFormInput()
        _petInput.value = PetPlanFormInput()
        _report.value = null
        _photoPicked.value = false
        _prepaidYears.value = 0
        _selectedAddOnIds.value = emptySet()
        _applySubsidy.value = true
    }

    // ---------- 提交 ----------

    fun submit() {
        val plan = (_planState.value as? DemoState.Success)?.value ?: return
        if (_submitting.value) return

        val today = LocalDate.now()
        val report = when (plan.audience) {
            AudienceTrack.HUMAN -> BurialFormRules.validateHuman(_humanInput.value, today)
            AudienceTrack.PET -> BurialFormRules.validatePet(_petInput.value, today)
        }
        _report.value = report
        if (!report.isValid) return

        submitJob?.cancel()
        submitJob = viewModelScope.launch {
            _submitting.value = true
            try {
                val draft = buildDraft(plan)
                val order = repository.createOrder(draft)
                _createdOrderId.value = order.id
            } finally {
                _submitting.value = false
            }
        }
    }

    private fun buildDraft(plan: BurialPlan): BurialOrderDraft {
        val (deceasedName, contactName, phone, expectDate) = when (plan.audience) {
            AudienceTrack.HUMAN -> {
                val input = _humanInput.value
                InputPart(input.deceasedName, input.contactName, input.phone, input.expectDate)
            }
            AudienceTrack.PET -> {
                val input = _petInput.value
                InputPart(input.petNickname, input.contactName, input.phone, input.expectDate)
            }
        }
        val quote = plan.quote(_prepaidYears.value, _selectedAddOnIds.value, _applySubsidy.value)
        val hasDetailedPricing = plan.priceYuan != null
        return BurialOrderDraft(
            audience = plan.audience,
            serviceId = plan.serviceId,
            serviceName = serviceDisplayName(plan.audience, plan.mode),
            mode = plan.mode,
            planId = plan.id,
            planTitle = plan.title,
            amountText = plan.priceText,
            planPriceYuan = plan.priceYuan,
            prepaidYears = quote.prepaidYears,
            prepaidManagementYuan = quote.prepaidManagementYuan,
            selectedAddOns = quote.addOnSummary(),
            addOnYuan = quote.addOnYuan,
            subsidyYuan = quote.subsidyYuan,
            totalYuan = if (hasDetailedPricing) quote.totalYuan else null,
            managementExpiresYear = if (hasDetailedPricing) {
                LocalDate.now().year + plan.includedManagementYears + quote.prepaidYears
            } else {
                null
            },
            renewalAnnualYuan = plan.renewalAnnualYuan,
            deceasedName = deceasedName,
            contactName = contactName,
            phone = phone,
            expectDate = expectDate,
        )
    }

    private data class InputPart(
        val deceasedName: String,
        val contactName: String,
        val phone: String,
        val expectDate: LocalDate?,
    )

    // ---------- 定位套餐（两份强类型流依次查找） ----------

    private suspend fun locatePlan(): DemoState<BurialPlan> {
        val human = collectTerminal(repository.humanPlans())
        planFrom(human)?.let { return it }
        val pet = collectTerminal(repository.petPlans())
        planFrom(pet)?.let { return it }
        return when {
            human is DemoState.Error && pet is DemoState.Error -> human
            else -> DemoState.Empty
        }
    }

    private fun planFrom(state: DemoState<List<BurialPlan>>): DemoState<BurialPlan>? =
        if (state is DemoState.Success) {
            state.value.planById(planId)?.let { DemoState.Success(it) }
        } else {
            null
        }

    private suspend fun collectTerminal(flow: Flow<DemoState<List<BurialPlan>>>): DemoState<List<BurialPlan>> {
        var last: DemoState<List<BurialPlan>>? = null
        flow.collect { last = it }
        return last ?: DemoState.Empty
    }

    override fun onCleared() {
        loadJob?.cancel()
        submitJob?.cancel()
        super.onCleared()
    }
}

/** 预约表单入口（plan-form/{planId}）。 */
@Composable
fun BurialPlanFormScreen(
    planId: String,
    navController: NavHostController,
) {
    val context = LocalContext.current
    val repository = remember(context) { BurialServiceLocator.repository(context) }
    val viewModel: BurialPlanFormViewModel = viewModel(
        factory = remember(planId, repository) {
            BurialViewModelFactory { BurialPlanFormViewModel(planId, repository) }
        },
    )

    val planState by viewModel.planState.collectAsStateWithLifecycle()
    val createdOrderId by viewModel.createdOrderId.collectAsStateWithLifecycle()

    LaunchedEffect(createdOrderId) {
        val orderId = createdOrderId ?: return@LaunchedEffect
        navController.navigate(BurialRoutes.order(orderId)) {
            popUpTo(AppRoute.PLAN_FORM.route + "/{planId}") { inclusive = true }
        }
    }

    AppScaffold(
        title = "填写预约信息",
        onBack = { navController.popBackStack() },
    ) {
        when (val current = planState) {
            DemoState.Loading -> LoadingState()
            is DemoState.Error -> ErrorRetry(message = current.message, onRetry = viewModel::reload)
            DemoState.Empty -> EmptyState(
                title = "未找到该套餐",
                description = "套餐信息暂不可用，请返回后重试。",
                actionLabel = "返回",
                onAction = { navController.popBackStack() },
            )
            is DemoState.Success -> {
                val plan = current.value
                PlanFormContent(
                    plan = plan,
                    viewModel = viewModel,
                )
            }
        }
    }
}

@Composable
private fun PlanFormContent(
    plan: BurialPlan,
    viewModel: BurialPlanFormViewModel,
) {
    val humanInput by viewModel.humanInput.collectAsStateWithLifecycle()
    val petInput by viewModel.petInput.collectAsStateWithLifecycle()
    val report by viewModel.report.collectAsStateWithLifecycle()
    val submitting by viewModel.submitting.collectAsStateWithLifecycle()
    val photoPicked by viewModel.photoPicked.collectAsStateWithLifecycle()
    val prepaidYears by viewModel.prepaidYears.collectAsStateWithLifecycle()
    val selectedAddOnIds by viewModel.selectedAddOnIds.collectAsStateWithLifecycle()
    val applySubsidy by viewModel.applySubsidy.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(12.dp))

        BurialCard {
            Column {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = plan.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${serviceDisplayName(plan.audience, plan.mode)} · ${plan.tier.label} · ${plan.priceText}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }

        when (plan.audience) {
            AudienceTrack.HUMAN -> HumanFormSection(
                input = humanInput,
                report = report,
                onUpdate = viewModel::updateHuman,
            )
            AudienceTrack.PET -> PetFormSection(
                input = petInput,
                report = report,
                photoPicked = photoPicked,
                onUpdate = viewModel::updatePet,
                onPhotoPicked = viewModel::setPhotoPicked,
            )
        }

        PlanPricingSelector(
            plan = plan,
            prepaidYears = prepaidYears,
            selectedAddOnIds = selectedAddOnIds,
            applySubsidy = applySubsidy,
            onPrepaidYearsChange = viewModel::setPrepaidYears,
            onToggleAddOn = viewModel::toggleAddOn,
            onApplySubsidyChange = viewModel::setApplySubsidy,
        )

        Spacer(Modifier.height(10.dp))
        PrimaryButton(
            text = if (submitting) "提交中…" else "提交预约申请",
            onClick = viewModel::submit,
            enabled = !submitting,
        )
        Spacer(Modifier.height(4.dp))
        TextButton(
            onClick = viewModel::resetForm,
            enabled = !submitting,
        ) {
            Text("重新填写", color = TextSecondary)
        }
        ReferenceNote(text = BURIAL_REFERENCE_TEXT)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun HumanFormSection(
    input: HumanPlanFormInput,
    report: BurialFormReport?,
    onUpdate: (transform: (HumanPlanFormInput) -> HumanPlanFormInput) -> Unit,
) {
    val today = LocalDate.now()

    val nameRequester = remember { FocusRequester() }
    val birthRequester = remember { FocusRequester() }
    val deathRequester = remember { FocusRequester() }
    val relationRequester = remember { FocusRequester() }
    val contactRequester = remember { FocusRequester() }
    val phoneRequester = remember { FocusRequester() }
    val expectRequester = remember { FocusRequester() }

    LaunchedEffect(report?.firstFocus) {
        when (report?.firstFocus) {
            BurialFormField.DECEASED_NAME -> nameRequester.requestFocus()
            BurialFormField.BIRTH_DATE -> birthRequester.requestFocus()
            BurialFormField.DEATH_DATE -> deathRequester.requestFocus()
            BurialFormField.RELATION -> relationRequester.requestFocus()
            BurialFormField.CONTACT_NAME -> contactRequester.requestFocus()
            BurialFormField.PHONE -> phoneRequester.requestFocus()
            BurialFormField.EXPECT_DATE -> expectRequester.requestFocus()
            else -> Unit
        }
    }

    BurialSectionTitle("逝者信息")
    BurialTextFormField(
        label = "逝者姓名",
        value = input.deceasedName,
        onValueChange = { v -> onUpdate { it.copy(deceasedName = v) } },
        isError = report?.errors?.containsKey(BurialFormField.DECEASED_NAME) == true,
        supportingText = report?.errors?.get(BurialFormField.DECEASED_NAME),
        focusRequester = nameRequester,
    )
    Spacer(Modifier.height(6.dp))
    BurialDateFormField(
        label = "出生日期",
        date = input.birthDate,
        errorText = report?.errors?.get(BurialFormField.BIRTH_DATE),
        selectable = { day -> !day.isAfter(today) },
        onDateSelected = { day -> onUpdate { it.copy(birthDate = day) } },
        focusRequester = birthRequester,
    )
    Spacer(Modifier.height(6.dp))
    BurialDateFormField(
        label = "离世日期",
        date = input.deathDate,
        errorText = report?.errors?.get(BurialFormField.DEATH_DATE),
        selectable = { day -> !day.isAfter(today) },
        onDateSelected = { day -> onUpdate { it.copy(deathDate = day) } },
        focusRequester = deathRequester,
    )
    Spacer(Modifier.height(6.dp))
    BurialTextFormField(
        label = "与逝者关系（如：子女）",
        value = input.relation,
        onValueChange = { v -> onUpdate { it.copy(relation = v) } },
        isError = report?.errors?.containsKey(BurialFormField.RELATION) == true,
        supportingText = report?.errors?.get(BurialFormField.RELATION),
        focusRequester = relationRequester,
    )

    ContactSection(
        contactName = input.contactName,
        phone = input.phone,
        expectDate = input.expectDate,
        report = report,
        contactRequester = contactRequester,
        phoneRequester = phoneRequester,
        expectRequester = expectRequester,
        onContactChange = { v -> onUpdate { it.copy(contactName = v) } },
        onPhoneChange = { v -> onUpdate { it.copy(phone = v) } },
        onExpectDate = { day -> onUpdate { it.copy(expectDate = day) } },
    )
    Spacer(Modifier.height(6.dp))
    BurialConsentRow(
        checked = input.consent,
        onCheckedChange = { v -> onUpdate { it.copy(consent = v) } },
    )
    if (report?.errors?.containsKey(BurialFormField.CONSENT) == true) {
        Text(
            text = report.errors.getValue(BurialFormField.CONSENT),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun PetFormSection(
    input: PetPlanFormInput,
    report: BurialFormReport?,
    photoPicked: Boolean,
    onUpdate: (transform: (PetPlanFormInput) -> PetPlanFormInput) -> Unit,
    onPhotoPicked: (Boolean) -> Unit,
) {
    val today = LocalDate.now()

    val nicknameRequester = remember { FocusRequester() }
    val deathRequester = remember { FocusRequester() }
    val contactRequester = remember { FocusRequester() }
    val phoneRequester = remember { FocusRequester() }
    val expectRequester = remember { FocusRequester() }

    LaunchedEffect(report?.firstFocus) {
        when (report?.firstFocus) {
            BurialFormField.PET_NICKNAME -> nicknameRequester.requestFocus()
            BurialFormField.DEATH_DATE -> deathRequester.requestFocus()
            BurialFormField.CONTACT_NAME -> contactRequester.requestFocus()
            BurialFormField.PHONE -> phoneRequester.requestFocus()
            BurialFormField.EXPECT_DATE -> expectRequester.requestFocus()
            else -> Unit
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> onPhotoPicked(uri != null) }

    BurialSectionTitle("宠物信息")
    BurialTextFormField(
        label = "宠物昵称",
        value = input.petNickname,
        onValueChange = { v -> onUpdate { it.copy(petNickname = v) } },
        isError = report?.errors?.containsKey(BurialFormField.PET_NICKNAME) == true,
        supportingText = report?.errors?.get(BurialFormField.PET_NICKNAME),
        focusRequester = nicknameRequester,
    )
    Spacer(Modifier.height(6.dp))
    BurialDateFormField(
        label = "离世日期",
        date = input.deathDate,
        errorText = report?.errors?.get(BurialFormField.DEATH_DATE),
        selectable = { day -> !day.isAfter(today) },
        onDateSelected = { day -> onUpdate { it.copy(deathDate = day) } },
        focusRequester = deathRequester,
    )
    Spacer(Modifier.height(6.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            modifier = Modifier.weight(1f),
        ) {
            Text(if (photoPicked) "已选择照片（仅本次会话）" else "添加宠物照片（可选）")
        }
    }

    ContactSection(
        contactName = input.contactName,
        phone = input.phone,
        expectDate = input.expectDate,
        report = report,
        contactRequester = contactRequester,
        phoneRequester = phoneRequester,
        expectRequester = expectRequester,
        onContactChange = { v -> onUpdate { it.copy(contactName = v) } },
        onPhoneChange = { v -> onUpdate { it.copy(phone = v) } },
        onExpectDate = { day -> onUpdate { it.copy(expectDate = day) } },
    )
    Spacer(Modifier.height(6.dp))
    BurialConsentRow(
        checked = input.consent,
        onCheckedChange = { v -> onUpdate { it.copy(consent = v) } },
    )
    if (report?.errors?.containsKey(BurialFormField.CONSENT) == true) {
        Text(
            text = report.errors.getValue(BurialFormField.CONSENT),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun ContactSection(
    contactName: String,
    phone: String,
    expectDate: LocalDate?,
    report: BurialFormReport?,
    contactRequester: FocusRequester,
    phoneRequester: FocusRequester,
    expectRequester: FocusRequester,
    onContactChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onExpectDate: (LocalDate) -> Unit,
) {
    BurialSectionTitle("联系人信息")
    BurialTextFormField(
        label = "联系人姓名",
        value = contactName,
        onValueChange = onContactChange,
        isError = report?.errors?.containsKey(BurialFormField.CONTACT_NAME) == true,
        supportingText = report?.errors?.get(BurialFormField.CONTACT_NAME),
        focusRequester = contactRequester,
    )
    Spacer(Modifier.height(6.dp))
    BurialTextFormField(
        label = "联系人手机号",
        value = phone,
        onValueChange = onPhoneChange,
        isError = report?.errors?.containsKey(BurialFormField.PHONE) == true,
        supportingText = report?.errors?.get(BurialFormField.PHONE),
        focusRequester = phoneRequester,
        keyboardType = KeyboardType.Phone,
    )
    Spacer(Modifier.height(6.dp))
    BurialDateFormField(
        label = "期望日期",
        date = expectDate,
        errorText = report?.errors?.get(BurialFormField.EXPECT_DATE),
        selectable = { day -> !day.isBefore(LocalDate.now()) },
        onDateSelected = onExpectDate,
        focusRequester = expectRequester,
    )
}
