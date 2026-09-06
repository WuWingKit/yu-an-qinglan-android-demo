/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.ui

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** 追忆模块统一日期选择对话框（本地时区语义，输出 LocalDate）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemorialDatePickerDialog(
    title: String,
    initialDate: LocalDate? = null,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialMillis = initialDate
        ?.atStartOfDay(ZoneId.systemDefault())
        ?.toInstant()
        ?.toEpochMilli()
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selected = pickerState.selectedDateMillis
                    if (selected != null) {
                        // DatePicker 返回 UTC 零点，还原为本地日。
                        val local = Instant.ofEpochMilli(selected)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        onConfirm(local)
                    }
                },
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    ) {
        DatePicker(state = pickerState, showModeToggle = true)
    }
}

/** LocalDate → 展示文本（如 1996年2月3日）。 */
fun dateTextOf(date: LocalDate): String =
    "${date.year}年${date.monthValue}月${date.dayOfMonth}日"

/** LocalDate → 本地零点毫秒（排序/时间轴用）。 */
fun localDateToMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
