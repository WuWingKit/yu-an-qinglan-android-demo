/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.profile.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.FormTextField
import com.yuanqinglan.app.core.ui.NoticeBanner
import com.yuanqinglan.app.core.ui.NoticeTone
import com.yuanqinglan.app.core.ui.PrimaryButton
import com.yuanqinglan.app.core.ui.ReferenceNote
import com.yuanqinglan.app.core.ui.SecondaryButton
import com.yuanqinglan.app.feature.profile.data.DataStoreProfileLocalStore
import com.yuanqinglan.app.feature.profile.data.ProfileLocalStore
import com.yuanqinglan.app.feature.profile.logic.ProfileRules
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 更换手机号页 ViewModel：本机绑定号码与本地验证码校验。 */
class PhoneEditViewModel(
    private val localStore: ProfileLocalStore,
    private val codeGenerator: () -> String = {
        Random.nextInt(100_000, 1_000_000).toString()
    },
) : ViewModel() {

    private val _oldPhone = MutableStateFlow("")
    val oldPhone: StateFlow<String> = _oldPhone.asStateFlow()

    private val _newPhone = MutableStateFlow("")
    val newPhone: StateFlow<String> = _newPhone.asStateFlow()

    private val _code = MutableStateFlow("")
    val code: StateFlow<String> = _code.asStateFlow()

    private val _generatedCode = MutableStateFlow("")
    val generatedCode: StateFlow<String> = _generatedCode.asStateFlow()

    private val _errors = MutableStateFlow<Map<String, String>>(emptyMap())
    val errors: StateFlow<Map<String, String>> = _errors.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success.asStateFlow()

    val boundPhone: StateFlow<String?> = localStore.boundPhone.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), null,
    )

    fun onOldChange(value: String) {
        _oldPhone.value = value
        _errors.value = emptyMap()
    }

    fun onNewChange(value: String) {
        _newPhone.value = value
        _errors.value = emptyMap()
    }

    fun onCodeChange(value: String) {
        _code.value = value
        _errors.value = emptyMap()
    }

    /** 生成本地验证码（仅本机校验，不发送短信）。 */
    fun requestCode() {
        _generatedCode.value = codeGenerator()
    }

    fun submit() {
        if (_saving.value) return
        viewModelScope.launch {
            _saving.value = true
            val currentBound = localStore.boundPhone.first()
            val errors = ProfileRules.phoneEditErrors(
                oldPhone = _oldPhone.value,
                newPhone = _newPhone.value,
                code = _code.value,
                generatedCode = _generatedCode.value,
                boundPhone = currentBound,
            )
            if (errors.isNotEmpty()) {
                _errors.value = errors
                _saving.value = false
                return@launch
            }
            localStore.setBoundPhone(_newPhone.value.trim())
            _success.value = true
            _saving.value = false
        }
    }

    fun consumeSuccess() {
        _success.value = false
        _generatedCode.value = ""
        _code.value = ""
        _oldPhone.value = ""
        _newPhone.value = ""
    }
}

/** 更换手机号页。 */
@Composable
fun PhoneEditScreen(
    onBack: () -> Unit,
    vm: PhoneEditViewModel? = null,
) {
    val context = LocalContext.current
    val localStore = remember(context) { DataStoreProfileLocalStore(context) }
    val effectiveViewModel: PhoneEditViewModel = vm ?: viewModel(
        factory = remember {
            ProfileViewModelFactory { PhoneEditViewModel(localStore) }
        },
    )

    val oldPhone by effectiveViewModel.oldPhone.collectAsStateWithLifecycle()
    val newPhone by effectiveViewModel.newPhone.collectAsStateWithLifecycle()
    val code by effectiveViewModel.code.collectAsStateWithLifecycle()
    val generatedCode by effectiveViewModel.generatedCode.collectAsStateWithLifecycle()
    val errors by effectiveViewModel.errors.collectAsStateWithLifecycle()
    val saving by effectiveViewModel.saving.collectAsStateWithLifecycle()
    val success by effectiveViewModel.success.collectAsStateWithLifecycle()
    val boundPhone by effectiveViewModel.boundPhone.collectAsStateWithLifecycle()

    AppScaffold(title = "更换手机号", onBack = onBack) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
        ) {
            Spacer(Modifier.size(10.dp))
            NoticeBanner(
                text = if (boundPhone == null) {
                    "本机尚未绑定手机号，可直接完成首次绑定；号码仅保存在本机，不自动对外提交。"
                } else {
                    "更换后仅更新本机预留号码；号码保存在本机，不自动对外提交。"
                },
                tone = NoticeTone.COMPLIANCE,
            )

            Spacer(Modifier.size(14.dp))
            if (boundPhone != null) {
                FormTextField(
                    label = "原手机号",
                    value = oldPhone,
                    onValueChange = effectiveViewModel::onOldChange,
                    isError = errors["old"] != null,
                    supportingText = errors["old"] ?: "请输入本机预留的原手机号",
                )
            }

            FormTextField(
                label = "新手机号",
                value = newPhone,
                onValueChange = effectiveViewModel::onNewChange,
                isError = errors["new"] != null,
                supportingText = errors["new"] ?: "请输入有效的 11 位手机号",
            )

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                FormTextField(
                    label = "验证码",
                    value = code,
                    onValueChange = effectiveViewModel::onCodeChange,
                    isError = errors["code"] != null,
                    supportingText = errors["code"] ?: "请输入 6 位验证码",
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = effectiveViewModel::requestCode) {
                    Text("获取验证码", color = MaterialTheme.colorScheme.primary)
                }
            }

            if (generatedCode.isNotBlank()) {
                Spacer(Modifier.size(6.dp))
                NoticeBanner(
                    text = "本机验证码：$generatedCode（仅本机校验，不会发送短信）。",
                    tone = NoticeTone.INFO,
                )
            }

            Spacer(Modifier.size(14.dp))
            PrimaryButton(
                text = if (saving) "提交中…" else "确认更换",
                onClick = effectiveViewModel::submit,
                enabled = !saving,
            )
            Spacer(Modifier.size(10.dp))
            ReferenceNote(text = "验证码仅用于本机校验，手机号与验证码均不对外传输。")
            ProfileBottomSpace()
        }
    }

    if (success) {
        SuccessDialog(
            title = "更换成功",
            message = "本机预留手机号已更新。",
            onDone = {
                effectiveViewModel.consumeSuccess()
                onBack()
            },
        )
    }
}
