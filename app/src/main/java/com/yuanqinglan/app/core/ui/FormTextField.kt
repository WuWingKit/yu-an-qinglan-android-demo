/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.core.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yuanqinglan.app.core.designsystem.AppDimensions

/**
 * 表单输入框：Material 3 OutlinedTextField，圆角 10dp。
 * [supportingText] 在传入时显示；错误态（[isError] = true）时 supportingText 使用警示色。
 */
@Composable
fun FormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean? = null,
    supportingText: String? = null,
    modifier: Modifier = Modifier,
) {
    val errorColor = MaterialTheme.colorScheme.error
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(text = label) },
        isError = isError == true,
        supportingText = if (supportingText != null) {
            {
                Text(
                    text = supportingText,
                    color = if (isError == true) errorColor else secondaryColor,
                )
            }
        } else {
            null
        },
        singleLine = true,
        shape = RoundedCornerShape(AppDimensions.CompactRadius),
    )
}
