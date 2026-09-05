/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.policy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.QingLanGreenDark
import com.yuanqinglan.app.core.designsystem.QingLanGreenSoft
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.designsystem.Warning
import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.ErrorRetry
import com.yuanqinglan.app.core.ui.FormTextField
import com.yuanqinglan.app.core.ui.InfoRow
import com.yuanqinglan.app.core.ui.LoadingState
import com.yuanqinglan.app.core.ui.NoticeBanner
import com.yuanqinglan.app.core.ui.NoticeTone
import com.yuanqinglan.app.core.ui.PrimaryButton
import com.yuanqinglan.app.core.ui.SecondaryButton
import com.yuanqinglan.app.data.local.AppContainer
import com.yuanqinglan.app.feature.policy.PrecheckPhase
import com.yuanqinglan.app.feature.policy.PrecheckViewModel
import com.yuanqinglan.app.feature.policy.data.AssetPolicyCatalogSource
import com.yuanqinglan.app.feature.policy.logic.CountyIndex
import com.yuanqinglan.app.feature.policy.model.County
import com.yuanqinglan.app.feature.policy.model.PrecheckField
import com.yuanqinglan.app.feature.policy.model.PrecheckForm
import com.yuanqinglan.app.feature.policy.model.PrecheckOptions
import com.yuanqinglan.app.feature.policy.model.SelectOption
import com.yuanqinglan.app.feature.policy.model.SubsidyEstimate

/**
 * 政策预审页（presult）：表单（本地校验）⇄ 参考测算结果。
 * 不自动提交个人信息，结果页明确"本地参考测算"语义。
 */
@Composable
fun PrecheckRoute(
    countyIdArg: String,
    onBack: () -> Unit,
    onOpenSeaDetail: () -> Unit,
) {
    val vm: PrecheckViewModel = viewModel(key = "precheck-$countyIdArg") {
        PrecheckViewModel(
            source = AssetPolicyCatalogSource(AppContainer.demoAssets),
            prefillCountyId = countyIdArg,
        )
    }
    val countiesState by vm.countiesState.collectAsStateWithLifecycle()
    val form by vm.form.collectAsStateWithLifecycle()
    val errors by vm.errors.collectAsStateWithLifecycle()
    val phase by vm.phase.collectAsStateWithLifecycle()
    val currentEstimate = vm.estimate.collectAsStateWithLifecycle().value
    val resultCountyName by vm.resultCountyName.collectAsStateWithLifecycle()

    AppScaffold(
        title = if (phase == PrecheckPhase.FORM) "政策预审" else "测算结果",
        onBack = onBack,
    ) {
        if (phase == PrecheckPhase.RESULT && currentEstimate != null) {
            PrecheckResultPane(
                form = form,
                countyName = resultCountyName,
                estimate = currentEstimate,
                onRestart = vm::restart,
                onOpenSeaDetail = onOpenSeaDetail,
            )
            return@AppScaffold
        }

        // 表单阶段
        when (val state = countiesState) {
            DemoState.Loading -> LoadingState()
            is DemoState.Error -> Box(modifier = Modifier.fillMaxSize()) {
                ErrorRetry(message = state.message, onRetry = vm::reloadCounties)
            }
            DemoState.Empty -> Box(modifier = Modifier.fillMaxSize()) {
                ErrorRetry(message = "区县数据为空，暂时无法预审。", onRetry = vm::reloadCounties)
            }
            is DemoState.Success -> PrecheckFormPane(
                counties = state.value,
                form = form,
                errorOf = vm::errorMessage,
                onCounty = vm::updateCounty,
                onApplicant = vm::updateApplicantType,
                onRelation = vm::updateRelationType,
                onMode = vm::updateBurialMode,
                onPhone = vm::updateContactPhone,
                onRemark = vm::updateRemark,
                onSubmit = { vm.submit() },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrecheckFormPane(
    counties: List<County>,
    form: PrecheckForm,
    errorOf: (PrecheckField) -> String?,
    onCounty: (String) -> Unit,
    onApplicant: (String) -> Unit,
    onRelation: (String) -> Unit,
    onMode: (String) -> Unit,
    onPhone: (String) -> Unit,
    onRemark: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = 6.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            NoticeBanner(
                text = "预审依据本地内置参考规则即时测算，仅作流程说明；不会向任何机构提交信息，也不代表办理结果。",
                tone = NoticeTone.COMPLIANCE,
            )
        }
        item {
            FieldCard(
                title = "所在区县",
                required = true,
                errorText = errorOf(PrecheckField.COUNTY),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    counties.forEach { county ->
                        ChoiceChip(
                            label = county.name,
                            selected = form.countyId == county.id,
                            onClick = { onCounty(county.id) },
                        )
                    }
                }
            }
        }
        item {
            FieldCard(
                title = "申请身份类型",
                required = true,
                errorText = errorOf(PrecheckField.APPLICANT),
            ) {
                ChoiceFlow(PrecheckOptions.applicantTypes, form.applicantType, onApplicant)
            }
        }
        item {
            FieldCard(
                title = "与逝者的关系",
                required = true,
                errorText = errorOf(PrecheckField.RELATION),
            ) {
                ChoiceFlow(PrecheckOptions.relationTypes, form.relationType, onRelation)
            }
        }
        item {
            FieldCard(
                title = "计划安葬方式",
                required = true,
                errorText = errorOf(PrecheckField.MODE),
            ) {
                ChoiceFlow(PrecheckOptions.burialModes, form.burialMode, onMode)
            }
        }
        item {
            FieldCard(title = "联系电话", required = false, errorText = errorOf(PrecheckField.PHONE)) {
                FormTextField(
                    label = "联系电话（选填）",
                    value = form.contactPhone,
                    onValueChange = onPhone,
                    isError = errorOf(PrecheckField.PHONE) != null,
                    supportingText = errorOf(PrecheckField.PHONE)
                        ?: "仅在本机做格式校验，不会提交或保存。",
                )
            }
        }
        item {
            FieldCard(title = "备注", required = false, errorText = null) {
                FormTextField(
                    label = "备注（选填）",
                    value = form.remark,
                    onValueChange = onRemark,
                )
            }
        }
        item {
            PrimaryButton(text = "开始本地测算", onClick = onSubmit, modifier = Modifier.padding(top = 2.dp))
        }
        item {
            Text(
                text = "请如实按本人情况选择；测算结果仅作参考，办理资格以民政部门核定为准。",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceFlow(
    options: List<SelectOption>,
    selectedValue: String,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            ChoiceChip(
                label = option.label,
                selected = selectedValue == option.value,
                onClick = { onSelect(option.value) },
            )
        }
    }
}

@Composable
private fun FieldCard(
    title: String,
    required: Boolean,
    errorText: String?,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                if (required) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "必填",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            content()
            if (errorText != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Warning,
                )
            }
        }
    }
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    val borderColor = if (selected) Color.Transparent else Color(0xFFD8D2C2)
    Surface(
        modifier = Modifier.clickable(
            role = Role.Button,
            onClickLabel = if (selected) "已选：$label" else "选择$label",
            onClick = onClick,
        ),
        shape = shape,
        color = if (selected) QingLanGreenSoft else Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .border(1.dp, borderColor, shape)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) QingLanGreenDark else TextPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PrecheckResultPane(
    form: PrecheckForm,
    countyName: String,
    estimate: SubsidyEstimate,
    onRestart: () -> Unit,
    onOpenSeaDetail: () -> Unit,
) {
    val modeLabel = PrecheckOptions.burialModes.firstOrNull { it.value == form.burialMode }?.label ?: ""
    val applicantLabel = PrecheckOptions.applicantTypes.firstOrNull { it.value == form.applicantType }?.label ?: ""
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = 8.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            NoticeBanner(
                text = "以下为本地规则参考测算，非官方核定结果，不构成办理承诺。",
                tone = NoticeTone.WARNING,
            )
        }
        item {
            Card(
                shape = RoundedCornerShape(AppDimensions.CardRadius),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
                    Text(
                        text = "预审信息",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(6.dp))
                    InfoRow(label = "区县", value = CountyIndex.cleanTitle(countyName))
                    InfoRow(label = "安葬方式", value = modeLabel.ifBlank { form.burialMode })
                    InfoRow(label = "申请身份", value = applicantLabel)
                }
            }
        }
        if (estimate.lines.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(AppDimensions.CardRadius),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
                        Text(
                            text = if (estimate.hasMatch) "补贴项目拆分（参考）" else "项目说明（参考）",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        estimate.lines.forEach { line ->
                            Column {
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.Top) {
                                    Text(
                                        text = line.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = if (line.amountYuan > 0) "${line.amountYuan} 元（参考）" else "公益项目",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = QingLanGreenDark,
                                        textAlign = TextAlign.End,
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = line.description,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "参考合计",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = "${estimate.totalYuan} 元",
                                style = MaterialTheme.typography.titleLarge,
                                color = QingLanGreenDark,
                            )
                        }
                    }
                }
            }
        }
        if (estimate.notes.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("需要留意", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    estimate.notes.forEach { note ->
                        Text(
                            text = "·  $note",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(AppDimensions.CardRadius),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
                    Text(
                        text = "下一步建议",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "1. 电话联系所选区县民政部门，确认当年度补贴名额与受理窗口。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                    Text(
                        text = "2. 到候选园区实地了解，确认园区是否在补贴范围及服务内容。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                    Text(
                        text = "3. 如需线下咨询，可提前预约园区或民政窗口的现场接待。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                    if (form.burialMode == "sea") {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "如选择公益海葬，可查看流程与报名方式。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable(role = Role.Button, onClickLabel = "查看公益海葬指引", onClick = onOpenSeaDetail)
                                .padding(vertical = 2.dp),
                        )
                    }
                }
            }
        }
        item {
            NoticeBanner(
                text = POLICY_COMPLIANCE_SENTENCE,
                tone = NoticeTone.COMPLIANCE,
            )
        }
        item {
            SecondaryButton(text = "重新填写", onClick = onRestart)
        }
    }
}
