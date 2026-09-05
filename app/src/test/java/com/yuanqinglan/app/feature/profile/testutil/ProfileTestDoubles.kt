/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.profile.testutil

import com.yuanqinglan.app.data.local.SettingsRepository
import com.yuanqinglan.app.feature.profile.data.ProfileLocalStore
import com.yuanqinglan.app.feature.profile.data.ProfileMediaImporter
import com.yuanqinglan.app.feature.profile.model.FeedbackRecord
import com.yuanqinglan.app.feature.profile.model.LocalMaterialEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** 测试用公共设置仓库假实现（内存态）。 */
class FakeSettingsRepository : SettingsRepository {
    override val elderMode = MutableStateFlow(false)
    override val treeholeEnabled = MutableStateFlow(true)
    override val nickname = MutableStateFlow("渝安青澜用户")
    override val avatarUri = MutableStateFlow<String?>(null)
    override val privacyAccepted = MutableStateFlow(false)

    override suspend fun setElderMode(enabled: Boolean) {
        elderMode.value = enabled
    }

    override suspend fun setTreeholeEnabled(enabled: Boolean) {
        treeholeEnabled.value = enabled
    }

    override suspend fun setNickname(nickname: String) {
        this.nickname.value = nickname
    }

    override suspend fun setAvatarUri(uri: String?) {
        avatarUri.value = uri
    }

    override suspend fun setPrivacyAccepted(accepted: Boolean) {
        privacyAccepted.value = accepted
    }

    override suspend fun resetAll() {
        elderMode.value = false
        treeholeEnabled.value = true
        nickname.value = "渝安青澜用户"
        avatarUri.value = null
        privacyAccepted.value = false
    }
}

/** 测试用个人中心本地仓库假实现（内存态，默认值与 DataStore 实现一致）。 */
class FakeProfileLocalStore : ProfileLocalStore {
    private val _allowBrowsePrefs = MutableStateFlow(false)
    private val _treeholeAnonymous = MutableStateFlow(true)
    private val _boundPhone = MutableStateFlow<String?>(null)
    private var passwordHash: String? = null
    private val _materials = MutableStateFlow<List<LocalMaterialEntry>>(emptyList())
    private val _feedback = MutableStateFlow<List<FeedbackRecord>>(emptyList())

    override val allowBrowsePrefs: Flow<Boolean> = _allowBrowsePrefs
    override val treeholeAnonymous: Flow<Boolean> = _treeholeAnonymous
    override val boundPhone: Flow<String?> = _boundPhone

    override suspend fun setAllowBrowsePrefs(enabled: Boolean) {
        _allowBrowsePrefs.value = enabled
    }

    override suspend fun setTreeholeAnonymous(enabled: Boolean) {
        _treeholeAnonymous.value = enabled
    }

    override suspend fun setBoundPhone(phone: String?) {
        _boundPhone.value = phone
    }

    override suspend fun verifyPassword(candidate: String): Boolean {
        val effective = passwordHash ?: sha("12345678")
        return sha(candidate) == effective
    }

    override suspend fun setNewPassword(newPassword: String) {
        passwordHash = sha(newPassword)
    }

    override suspend fun resetLocalPassword() {
        passwordHash = null
    }

    override val materials: Flow<List<LocalMaterialEntry>> = _materials

    override suspend fun addMaterial(entry: LocalMaterialEntry) {
        _materials.value = _materials.value + entry
    }

    override suspend fun removeMaterial(id: String) {
        _materials.value = _materials.value.filterNot { it.id == id }
    }

    override val feedbackRecords: Flow<List<FeedbackRecord>> = _feedback

    override suspend fun addFeedback(record: FeedbackRecord) {
        _feedback.value = (_feedback.value + record).takeLast(20)
    }

    private fun sha(value: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

/** 测试用文件搬运假实现：可预置导入结果。 */
class FakeMediaImporter : ProfileMediaImporter {
    var nextImageUri: String? = "file:///data/user/0/com.yuanqinglan.app/files/yuanqinglan/images/test.jpg"
    var nextAudioUri: String? = "file:///data/user/0/com.yuanqinglan.app/files/yuanqinglan/audio/test.m4a"
    var importImageFail = false
    var importAudioFail = false
    val deletedUris = mutableListOf<String>()

    override suspend fun importImageToPrivate(sourceUri: String): String? =
        if (importImageFail) null else nextImageUri

    override suspend fun importAudioToPrivate(sourceUri: String): String? =
        if (importAudioFail) null else nextAudioUri

    override suspend fun deletePrivateFile(uriString: String?): Boolean {
        if (uriString != null) deletedUris.add(uriString)
        return uriString != null
    }
}
