/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.home.data

import com.yuanqinglan.app.data.local.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 进程内 SettingsRepository 实现（内存态，不落盘）。
 *
 * 用途说明：主要供 feature 单元测试使用；在 foundation 的 AppContainer /
 * DataStore 实现合入前，也可作为页面默认接缝保证可运行。合入后生产页面
 * 一律经 AppContainer.settings 获取公共实现，使老年模式统一持久化并全局生效。
 */
class InMemorySettingsRepository : SettingsRepository {
    private val _elderMode = MutableStateFlow(false)
    private val _treeholeEnabled = MutableStateFlow(true)
    private val _nickname = MutableStateFlow("渝安青澜用户")
    private val _avatarUri = MutableStateFlow<String?>(null)
    private val _privacyAccepted = MutableStateFlow(false)

    override val elderMode: Flow<Boolean> = _elderMode
    override val treeholeEnabled: Flow<Boolean> = _treeholeEnabled
    override val nickname: Flow<String> = _nickname
    override val avatarUri: Flow<String?> = _avatarUri
    override val privacyAccepted: Flow<Boolean> = _privacyAccepted

    override suspend fun setElderMode(enabled: Boolean) {
        _elderMode.value = enabled
    }

    override suspend fun setTreeholeEnabled(enabled: Boolean) {
        _treeholeEnabled.value = enabled
    }

    override suspend fun setNickname(nickname: String) {
        _nickname.value = nickname
    }

    override suspend fun setAvatarUri(uri: String?) {
        _avatarUri.value = uri
    }

    override suspend fun setPrivacyAccepted(accepted: Boolean) {
        _privacyAccepted.value = accepted
    }

    override suspend fun resetAll() {
        _elderMode.value = false
        _treeholeEnabled.value = true
        _nickname.value = "渝安青澜用户"
        _avatarUri.value = null
        _privacyAccepted.value = false
    }
}
