/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** 进程级单例：单文件 DataStore 名为 settings。 */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private object SettingsKeys {
    val ELDER_MODE = booleanPreferencesKey("elder_mode")
    val TREEHOLE_ENABLED = booleanPreferencesKey("treehole_enabled")
    val NICKNAME = stringPreferencesKey("nickname")
    val AVATAR_URI = stringPreferencesKey("avatar_uri")
    val PRIVACY_ACCEPTED = booleanPreferencesKey("privacy_accepted")
}

/**
 * [SettingsRepository] 的 DataStore 实现。所有写操作注入可配置的固定短延迟
 * （默认 400ms），使加载/成功/空/失败/重试等界面状态可稳定复现。
 */
class DataStoreSettingsRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val delayMillis: Long = DEFAULT_SIMULATED_DELAY_MILLIS,
) : SettingsRepository {

    /** 通过 Android Context 获取默认单文件 DataStore 的入口。 */
    constructor(context: Context) : this(context.settingsDataStore)

    override val elderMode: Flow<Boolean> = dataStore.data
        .settingsCatch()
        .map { it[SettingsKeys.ELDER_MODE] ?: DEFAULT_ELDER_MODE }

    override val treeholeEnabled: Flow<Boolean> = dataStore.data
        .settingsCatch()
        .map { it[SettingsKeys.TREEHOLE_ENABLED] ?: DEFAULT_TREEHOLE_ENABLED }

    override val nickname: Flow<String> = dataStore.data
        .settingsCatch()
        .map { it[SettingsKeys.NICKNAME] ?: DEFAULT_NICKNAME }

    override val avatarUri: Flow<String?> = dataStore.data
        .settingsCatch()
        .map { it[SettingsKeys.AVATAR_URI] }

    override val privacyAccepted: Flow<Boolean> = dataStore.data
        .settingsCatch()
        .map { it[SettingsKeys.PRIVACY_ACCEPTED] ?: DEFAULT_PRIVACY_ACCEPTED }

    override suspend fun setElderMode(enabled: Boolean) {
        simulatePersistenceDelay()
        dataStore.edit { it[SettingsKeys.ELDER_MODE] = enabled }
    }

    override suspend fun setTreeholeEnabled(enabled: Boolean) {
        simulatePersistenceDelay()
        dataStore.edit { it[SettingsKeys.TREEHOLE_ENABLED] = enabled }
    }

    override suspend fun setNickname(nickname: String) {
        simulatePersistenceDelay()
        dataStore.edit { it[SettingsKeys.NICKNAME] = nickname }
    }

    override suspend fun setAvatarUri(uri: String?) {
        simulatePersistenceDelay()
        dataStore.edit { preferences ->
            if (uri == null) {
                preferences.remove(SettingsKeys.AVATAR_URI)
            } else {
                preferences[SettingsKeys.AVATAR_URI] = uri
            }
        }
    }

    override suspend fun setPrivacyAccepted(accepted: Boolean) {
        simulatePersistenceDelay()
        dataStore.edit { it[SettingsKeys.PRIVACY_ACCEPTED] = accepted }
    }

    override suspend fun resetAll() {
        simulatePersistenceDelay()
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    private suspend fun simulatePersistenceDelay() {
        if (delayMillis > 0L) delay(delayMillis)
    }

    private fun Flow<Preferences>.settingsCatch(): Flow<Preferences> = catch { throwable ->
        if (throwable is IOException) {
            emit(emptyPreferences())
        } else {
            throw throwable
        }
    }

    companion object {
        /** 写操作默认注入的模拟持久化延迟（毫秒），让各状态可复现。 */
        const val DEFAULT_SIMULATED_DELAY_MILLIS = 400L
        const val DEFAULT_ELDER_MODE = false
        const val DEFAULT_TREEHOLE_ENABLED = true
        const val DEFAULT_NICKNAME = "渝安青澜用户"
        const val DEFAULT_PRIVACY_ACCEPTED = false
    }
}
