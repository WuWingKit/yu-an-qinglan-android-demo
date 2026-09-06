/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yuanqinglan.app.core.designsystem.currentTouchTargetSize
import com.yuanqinglan.app.feature.memorial.model.MemorialDate
import com.yuanqinglan.app.feature.memorial.model.MemorialDateRules
import com.yuanqinglan.app.feature.memorial.model.MemorialDateRules.formatMemorialDate
import com.yuanqinglan.app.feature.memorial.model.MemorialDateRules.toLocalDateOrNull
import com.yuanqinglan.app.feature.memorial.model.MemorialDateRules.toMemorialDate

/**
 * 出生/离世日期录入控件（新建与编辑共用）：
 * - 「选择日期」打开完整日期选择器；
 * - 「仅填年份」切换为年份录入（如 1996，校验 1900-2100）；
 * - 「清除」恢复未知；
 * - 当前值始终自然格式化（未知/仅年份/年月/完整日期），不显示 null/0/占位；
 * - 全部操作有可读语义，按钮热区不低于当前模式（老年 52dp / 普通 48dp）。
 */
@Composable
fun MemorialDateField(
    label: String,
    value: MemorialDate?,
    error: String?,
    onChange: (MemorialDate?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val touchTarget = currentTouchTargetSize()
    var showPicker by rememberSaveable(label) { mutableStateOf(false) }
    var yearMode by rememberSaveable(label) { mutableStateOf(value != null && value.month == null) }
    var yearText by rememberSaveable(label) { mutableStateOf(value?.year?.toString() ?: "") }

    val yearError = yearText.trim().let { raw ->
        if (raw.isEmpty()) {
            null
        } else {
            val parsed = raw.toIntOrNull()
            when {
                parsed == null -> "年份需为数字"
                parsed !in MemorialDateRules.YEAR_RANGE ->
                    "请填写 ${MemorialDateRules.YEAR_RANGE.first}-${MemorialDateRules.YEAR_RANGE.last} 之间的年份"
                else -> null
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = formatMemorialDate(value),
            style = MaterialTheme.typography.bodyLarge,
            color = if (value == null) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        error?.let { message ->
            Spacer(Modifier.height(2.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { showPicker = true },
                modifier = Modifier.defaultMinSize(minHeight = touchTarget),
            ) {
                Text("选择日期")
            }
            TextButton(onClick = { yearMode = true }) {
                Text("仅填年份")
            }
            if (value != null) {
                TextButton(
                    onClick = {
                        onChange(null)
                        yearMode = false
                        yearText = ""
                    },
                ) {
                    Text("清除")
                }
            }
        }
        if (yearMode) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = yearText,
                    onValueChange = { yearText = it.filter(Char::isDigit).take(4) },
                    modifier = Modifier.weight(1f),
                    label = { Text("年份") },
                    singleLine = true,
                    isError = yearError != null,
                    supportingText = yearError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                )
                Spacer(Modifier.width(8.dp))
                TextButton(
                    enabled = yearError == null && yearText.isNotBlank(),
                    onClick = {
                        val year = yearText.toInt()
                        onChange(MemorialDate(year = year))
                        yearMode = false
                    },
                ) {
                    Text("设为年份")
                }
            }
        }
    }

    if (showPicker) {
        MemorialDatePickerDialog(
            title = "选择$label",
            initialDate = value?.toLocalDateOrNull(),
            onConfirm = { date ->
                onChange(date.toMemorialDate())
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}
