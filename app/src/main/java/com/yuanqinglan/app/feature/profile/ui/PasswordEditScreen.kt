/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.profile.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.NoticeBanner
import com.yuanqinglan.app.core.ui.NoticeTone
import com.yuanqinglan.app.core.ui.PrimaryButton
import com.yuanqinglan.app.core.ui.ReferenceNote
import com.yuanqinglan.app.core.ui.SecondaryButton
import com.yuanqinglan.app.feature.profile.data.DataStoreProfileLocalStore
import com.yuanqinglan.app.feature.profile.data.ProfileLocalStore
import com.yuanqinglan.app.feature.profile.logic.ProfileRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 修改密码页 ViewModel：本机密码摘要校验与更新。 */
class PasswordEditViewModel(
    private val localStore: ProfileLocalStore,
) : ViewModel() {

    private val _oldPassword = MutableStateFlow("")
    val oldPassword: StateFlow<String> = _oldPassword.asStateFlow()

    private val _newPassword = MutableStateFlow("")
    val newPassword: StateFlow<String> = _newPassword.asStateFlow()

    private val _confirm = MutableStateFlow("")
    val confirm: StateFlow<String> = _confirm.asStateFlow()

    private val _errors = MutableStateFlow<Map<String, String>>(emptyMap())
    val errors: StateFlow<Map<String, String>> = _errors.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success.asStateFlow()

    fun onOldChange(value: String) {
        _oldPassword.value = value
        _errors.value = emptyMap()
    }

    fun onNewChange(value: String) {
        _newPassword.value = value
        _errors.value = emptyMap()
    }

    fun onConfirmChange(value: String) {
        _confirm.value = value
        _errors.value = emptyMap()
    }

    fun submit() {
        if (_saving.value) return
        viewModelScope.launch {
            _saving.value = true
            val oldMatches = localStore.verifyPassword(_oldPassword.value)
            val errors = ProfileRules.passwordEditErrors(
                oldPassword = _oldPassword.value,
                newPassword = _newPassword.value,
                confirm = _confirm.value,
                currentPasswordMatches = oldMatches,
            )
            if (errors.isNotEmpty()) {
                _errors.value = errors
                _saving.value = false
                return@launch
            }
            localStore.setNewPassword(_newPassword.value)
            _oldPassword.value = ""
            _newPassword.value = ""
            _confirm.value = ""
            _success.value = true
            _saving.value = false
        }
    }

    fun consumeSuccess() {
        _success.value = false
    }
}

/** 修改密码页。 */
@Composable
fun PasswordEditScreen(
    onBack: () -> Unit,
    vm: PasswordEditViewModel? = null,
) {
    val context = LocalContext.current
    val localStore = remember(context) { DataStoreProfileLocalStore(context) }
    val effectiveViewModel: PasswordEditViewModel = vm ?: viewModel(
        factory = remember {
            ProfileViewModelFactory { PasswordEditViewModel(localStore) }
        },
    )

    val oldPassword by effectiveViewModel.oldPassword.collectAsStateWithLifecycle()
    val newPassword by effectiveViewModel.newPassword.collectAsStateWithLifecycle()
    val confirm by effectiveViewModel.confirm.collectAsStateWithLifecycle()
    val errors by effectiveViewModel.errors.collectAsStateWithLifecycle()
    val saving by effectiveViewModel.saving.collectAsStateWithLifecycle()
    val success by effectiveViewModel.success.collectAsStateWithLifecycle()

    AppScaffold(title = "修改密码", onBack = onBack) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
        ) {
            Spacer(Modifier.size(10.dp))
            NoticeBanner(
                text = "密码仅保存在本机，用于本机校验，不对外传输。如从未修改，初始密码为本机预设密码。",
                tone = NoticeTone.COMPLIANCE,
            )

            Spacer(Modifier.size(14.dp))
            PasswordField(
                label = "原密码",
                value = oldPassword,
                onValueChange = effectiveViewModel::onOldChange,
                error = errors["old"],
                supportingText = "如未修改过，请输入初始密码 12345678",
            )
            PasswordField(
                label = "新密码",
                value = newPassword,
                onValueChange = effectiveViewModel::onNewChange,
                error = errors["new"],
                supportingText = "6-20 位，需同时包含字母和数字",
            )
            PasswordField(
                label = "确认新密码",
                value = confirm,
                onValueChange = effectiveViewModel::onConfirmChange,
                error = errors["confirm"],
                supportingText = "再次输入新密码",
            )

            Spacer(Modifier.size(14.dp))
            PrimaryButton(
                text = if (saving) "保存中…" else "确认修改",
                onClick = effectiveViewModel::submit,
                enabled = !saving,
            )
            Spacer(Modifier.size(10.dp))
            ReferenceNote(text = "修改成功后，之后请使用新密码在本机校验。")
            ProfileBottomSpace()
        }
    }

    if (success) {
        SuccessDialog(
            title = "密码修改成功",
            message = "新的本机密码已保存。",
            onDone = {
                effectiveViewModel.consumeSuccess()
                onBack()
            },
        )
    }
}

@Composable
fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String? = null,
    supportingText: String,
) {
    val errorColor = MaterialTheme.colorScheme.error
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        isError = error != null,
        supportingText = {
            Text(
                text = error ?: supportingText,
                color = if (error != null) errorColor else secondaryColor,
            )
        },
    )
}
