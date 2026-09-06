/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import java.time.LocalDate

/**
 * 文本输入行（直接使用 M3 OutlinedTextField，以支持校验聚焦/键盘类型；
 * 视觉与设计基线保持一致）。
 */
@Composable
fun BurialTextFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    supportingText: String?,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
) {
    val baseModifier = modifier
        .fillMaxWidth()
        .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = isError,
        supportingText = supportingText?.let { text -> { Text(text) } },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(AppDimensions.CompactRadius),
        modifier = baseModifier,
    )
}

/**
 * 只读日期输入行：点击整行弹出 [BurialDatePickerDialog]，
 * [selectable] 限制可选日期范围（如期望日期不早于今天）。
 */
@Composable
fun BurialDateFormField(
    label: String,
    date: LocalDate?,
    errorText: String?,
    selectable: (LocalDate) -> Boolean,
    onDateSelected: (LocalDate) -> Unit,
    focusRequester: FocusRequester? = null,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val display = date?.let { "%d 年 %d 月 %d 日".format(it.year, it.monthValue, it.dayOfMonth) }
        ?: "请选择日期"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showPicker = true },
    ) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            isError = errorText != null,
            supportingText = errorText?.let { text -> { Text(text) } },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Outlined.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            shape = RoundedCornerShape(AppDimensions.CompactRadius),
            modifier = Modifier
                .fillMaxWidth()
                .let { if (focusRequester != null) it.focusRequester(focusRequester) else it },
        )
    }
    if (showPicker) {
        BurialDatePickerDialog(
            title = label,
            initialDate = date,
            selectable = selectable,
            onConfirm = { day ->
                onDateSelected(day)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

/** 服务说明确认行。 */
@Composable
fun BurialConsentRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column(modifier = Modifier.padding(top = 10.dp)) {
            Text(
                text = "我已了解并同意：以上信息仅保存在本机，用于查看办理进度参考；" +
                    "相关信息仅供参考，具体费用与办理结果以服务机构最终公布为准。",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )
            Text(
                text = "本页不会向任何机构自动提交您的个人信息。",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
