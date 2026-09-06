/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yuanqinglan.app.core.designsystem.QingLanGreen
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.feature.burial.model.BurialPlan
import com.yuanqinglan.app.feature.burial.model.PlanQuote
import com.yuanqinglan.app.feature.burial.model.quote
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun PlanPricingDetails(plan: BurialPlan) {
    if (plan.priceYuan == null) return
    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
    Text("费用与管理", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
    Spacer(Modifier.height(6.dp))
    PricingLine("已含管理费", "前 ${plan.includedManagementYears} 年")
    PricingLine("续期标准", "第 ${plan.renewalStartYear} 年起 ${yuan(plan.renewalAnnualYuan)} / 年")
    if (plan.managementPrepay.isNotEmpty()) {
        PricingLine(
            "管理费预付",
            plan.managementPrepay.joinToString(" · ") { "${it.years} 年 ${yuan(it.priceYuan)}" },
        )
    }
    if (plan.addOns.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        Text("可选增值服务", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        plan.addOns.forEach { PricingLine(it.label, "+${yuan(it.priceYuan)}") }
    }
    if (plan.excludedFees.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        Text("不含费用", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        plan.excludedFees.forEach {
            Text("• $it", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
    if (plan.subsidyNote.isNotBlank()) {
        Spacer(Modifier.height(10.dp))
        Text("重庆补贴", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        Text(plan.subsidyNote, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
internal fun PlanPricingSelector(
    plan: BurialPlan,
    prepaidYears: Int,
    selectedAddOnIds: Set<String>,
    applySubsidy: Boolean,
    onPrepaidYearsChange: (Int) -> Unit,
    onToggleAddOn: (String) -> Unit,
    onApplySubsidyChange: (Boolean) -> Unit,
) {
    if (plan.priceYuan == null) return
    BurialSectionTitle("费用确认")
    Text("预付管理费", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = prepaidYears == 0,
            onClick = { onPrepaidYearsChange(0) },
            label = { Text("暂不预付") },
        )
        plan.managementPrepay.forEach { option ->
            FilterChip(
                selected = prepaidYears == option.years,
                onClick = { onPrepaidYearsChange(option.years) },
                label = { Text("${option.years} 年 +${yuan(option.priceYuan)}") },
            )
        }
    }
    if (plan.addOns.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text("增值服务", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        plan.addOns.forEach { option ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = option.id in selectedAddOnIds,
                    onCheckedChange = { onToggleAddOn(option.id) },
                )
                Text(
                    text = "${option.label}  +${yuan(option.priceYuan)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                )
            }
        }
    }
    if (plan.subsidyYuan > 0) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = applySubsidy, onCheckedChange = onApplySubsidyChange)
            Column {
                Text("按最高重庆补贴试算 -${yuan(plan.subsidyYuan)}", color = TextPrimary)
                Text("需重庆户籍及火化证明，最终以民政审核为准", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
    val quote = plan.quote(prepaidYears, selectedAddOnIds, applySubsidy)
    BurialCard {
        Column {
            PricingLine("套餐价", yuan(quote.planPriceYuan))
            PricingLine("预付管理费", yuan(quote.prepaidManagementYuan))
            PricingLine("增值服务", yuan(quote.addOnYuan))
            PricingLine("政府补贴", if (quote.subsidyYuan > 0) "-${yuan(quote.subsidyYuan)}" else yuan(0))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("预计实付", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(yuan(quote.totalYuan), style = MaterialTheme.typography.titleLarge, color = QingLanGreen)
            }
            Text("实付 = 套餐价 + 预付管理费 + 增值服务 - 政府补贴", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
private fun PricingLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(0.38f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = TextPrimary, modifier = Modifier.weight(0.62f))
    }
    Spacer(Modifier.height(4.dp))
}

internal fun yuan(value: Int): String =
    "¥${NumberFormat.getIntegerInstance(Locale.CHINA).format(value)}"

internal fun PlanQuote.addOnSummary(): List<String> =
    selectedAddOns.map { "${it.label} +${yuan(it.priceYuan)}" }
