/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.profile.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yuanqinglan.app.feature.profile.logic.ProfileRules
import com.yuanqinglan.app.feature.profile.model.FeedbackRecord
import com.yuanqinglan.app.feature.profile.model.LocalMaterialEntry
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 个人中心本地偏好/记录仓库契约（profile 独占的 DataStore，不影响公共设置仓库）。
 *
 * 存放公共 [com.yuanqinglan.app.data.local.SettingsRepository] 未覆盖、但仅个人中心
 * 使用的隐私开关、本机绑定手机号、本地密码摘要、素材索引与反馈记录。
 * 头像/昵称/老年模式/树洞开关等公共项一律走公共设置仓库，保证全局唯一来源。
 */
interface ProfileLocalStore {
    /** 是否允许保存浏览偏好。 */
    val allowBrowsePrefs: Flow<Boolean>
    suspend fun setAllowBrowsePrefs(enabled: Boolean)

    /** 树洞内容是否匿名展示。 */
    val treeholeAnonymous: Flow<Boolean>
    suspend fun setTreeholeAnonymous(enabled: Boolean)

    /** 本机绑定的手机号（可空，空表示尚未绑定）。 */
    val boundPhone: Flow<String?>
    suspend fun setBoundPhone(phone: String?)

    /** 校验原密码是否正确（未设置过时以本地初始预设校验）。 */
    suspend fun verifyPassword(candidate: String): Boolean

    /** 保存新本地密码（摘要落盘）。 */
    suspend fun setNewPassword(newPassword: String)

    /** 重置本地密码为初始预设（需要二次确认）。 */
    suspend fun resetLocalPassword()

    /** 本地素材索引（图片/音频/AI 生成内容统一索引，删除即销毁）。 */
    val materials: Flow<List<LocalMaterialEntry>>
    suspend fun addMaterial(entry: LocalMaterialEntry)
    suspend fun removeMaterial(id: String)

    /** 已提交的本机反馈记录。 */
    val feedbackRecords: Flow<List<FeedbackRecord>>
    suspend fun addFeedback(record: FeedbackRecord)
}

private val Context.profileLocalDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "profile_local",
)

private object ProfileLocalKeys {
    val ALLOW_BROWSE_PREFS = booleanPreferencesKey("allow_browse_prefs")
    val TREEHOLE_ANONYMOUS = booleanPreferencesKey("treehole_anonymous")
    val BOUND_PHONE = stringPreferencesKey("bound_phone")
    val PASSWORD_HASH = stringPreferencesKey("password_hash")
    val MATERIALS = stringPreferencesKey("materials")
    val FEEDBACK = stringPreferencesKey("feedback")
}

/** DataStore 默认实现；写操作注入短延迟使界面状态可复现。 */
class DataStoreProfileLocalStore internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val delayMillis: Long = DEFAULT_SIMULATED_DELAY_MILLIS,
) : ProfileLocalStore {

    constructor(context: Context) : this(context.profileLocalDataStore)

    private val json = Json { ignoreUnknownKeys = true }

    override val allowBrowsePrefs: Flow<Boolean> = dataStore.data
        .localCatch()
        .map { it[ProfileLocalKeys.ALLOW_BROWSE_PREFS] ?: DEFAULT_ALLOW_BROWSE_PREFS }

    override val treeholeAnonymous: Flow<Boolean> = dataStore.data
        .localCatch()
        .map { it[ProfileLocalKeys.TREEHOLE_ANONYMOUS] ?: DEFAULT_TREEHOLE_ANONYMOUS }

    override val boundPhone: Flow<String?> = dataStore.data
        .localCatch()
        .map { it[ProfileLocalKeys.BOUND_PHONE] }

    override suspend fun setAllowBrowsePrefs(enabled: Boolean) {
        simulateDelay()
        dataStore.edit { it[ProfileLocalKeys.ALLOW_BROWSE_PREFS] = enabled }
    }

    override suspend fun setTreeholeAnonymous(enabled: Boolean) {
        simulateDelay()
        dataStore.edit { it[ProfileLocalKeys.TREEHOLE_ANONYMOUS] = enabled }
    }

    override suspend fun setBoundPhone(phone: String?) {
        simulateDelay()
        dataStore.edit { prefs ->
            if (phone == null) {
                prefs.remove(ProfileLocalKeys.BOUND_PHONE)
            } else {
                prefs[ProfileLocalKeys.BOUND_PHONE] = phone
            }
        }
    }

    override suspend fun verifyPassword(candidate: String): Boolean {
        val hash = dataStore.data.localCatch().first()[ProfileLocalKeys.PASSWORD_HASH]
        val effective = hash ?: ProfileRules.sha256Hex(ProfileRules.DEFAULT_LOCAL_PASSWORD)
        return ProfileRules.sha256Hex(candidate) == effective
    }

    override suspend fun setNewPassword(newPassword: String) {
        simulateDelay()
        val hash = ProfileRules.sha256Hex(newPassword)
        dataStore.edit { it[ProfileLocalKeys.PASSWORD_HASH] = hash }
    }

    override suspend fun resetLocalPassword() {
        simulateDelay()
        dataStore.edit { it.remove(ProfileLocalKeys.PASSWORD_HASH) }
    }

    override val materials: Flow<List<LocalMaterialEntry>> = dataStore.data
        .localCatch()
        .map { decodeList(it[ProfileLocalKeys.MATERIALS], ListSerializer(LocalMaterialEntry.serializer())) }

    override suspend fun addMaterial(entry: LocalMaterialEntry) {
        simulateDelay()
        dataStore.edit { prefs ->
            val existing = decodeList(
                prefs[ProfileLocalKeys.MATERIALS],
                ListSerializer(LocalMaterialEntry.serializer()),
            )
            prefs[ProfileLocalKeys.MATERIALS] =
                json.encodeToString(ListSerializer(LocalMaterialEntry.serializer()), existing + entry)
        }
    }

    override suspend fun removeMaterial(id: String) {
        simulateDelay()
        dataStore.edit { prefs ->
            val existing = decodeList(
                prefs[ProfileLocalKeys.MATERIALS],
                ListSerializer(LocalMaterialEntry.serializer()),
            )
            prefs[ProfileLocalKeys.MATERIALS] = json.encodeToString(
                ListSerializer(LocalMaterialEntry.serializer()),
                existing.filterNot { it.id == id },
            )
        }
    }

    override val feedbackRecords: Flow<List<FeedbackRecord>> = dataStore.data
        .localCatch()
        .map { decodeList(it[ProfileLocalKeys.FEEDBACK], ListSerializer(FeedbackRecord.serializer())) }

    override suspend fun addFeedback(record: FeedbackRecord) {
        simulateDelay()
        dataStore.edit { prefs ->
            val existing = decodeList(
                prefs[ProfileLocalKeys.FEEDBACK],
                ListSerializer(FeedbackRecord.serializer()),
            )
            val kept = (existing + record).takeLast(MAX_FEEDBACK_RECORDS)
            prefs[ProfileLocalKeys.FEEDBACK] = json.encodeToString(
                ListSerializer(FeedbackRecord.serializer()),
                kept,
            )
        }
    }

    private suspend fun simulateDelay() {
        if (delayMillis > 0L) delay(delayMillis)
    }

    private fun Flow<Preferences>.localCatch(): Flow<Preferences> = catch { throwable ->
        if (throwable is IOException) {
            emit(emptyPreferences())
        } else {
            throw throwable
        }
    }

    private fun <T> decodeList(raw: String?, serializer: KSerializer<List<T>>): List<T> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(serializer, raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        const val DEFAULT_SIMULATED_DELAY_MILLIS = 250L
        const val MAX_FEEDBACK_RECORDS = 20

        /** 隐私默认值：默认不保存浏览偏好（最小化采集）；树洞默认匿名展示。 */
        const val DEFAULT_ALLOW_BROWSE_PREFS = false
        const val DEFAULT_TREEHOLE_ANONYMOUS = true
    }
}
