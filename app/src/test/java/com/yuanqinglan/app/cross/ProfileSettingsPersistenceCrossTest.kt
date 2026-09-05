/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.cross

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.yuanqinglan.app.data.local.DataStoreSettingsRepository
import com.yuanqinglan.app.feature.profile.logic.ProfileRules
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 跨模块关键设置链路测试：公共设置仓库（DataStore）由多个实例/多次会话共享，
 * elderMode/nickname/treeholeEnabled/privacyAccepted 的持久化与 resetAll 复位，
 * 供个人中心页与首页右上角开关共用同一数据源。
 */
class ProfileSettingsPersistenceCrossTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var testFile: File
    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        testFile = File.createTempFile("yuanqinglan_cross_settings", ".preferences_pb")
        testFile.delete()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { testFile }
    }

    @After
    fun tearDown() {
        scope.cancel()
        testFile.delete()
    }

    @Test
    fun elderModePersistsAcrossRepositoryInstances_andResetAllRestores() = runBlocking {
        val first = DataStoreSettingsRepository(dataStore, delayMillis = 0L)
        first.setElderMode(true)
        first.setTreeholeEnabled(false)
        first.setPrivacyAccepted(true)

        // 新的仓库实例读取同一 DataStore 文件（等价于进程内再次进入页面）
        val second = DataStoreSettingsRepository(dataStore, delayMillis = 0L)
        assertTrue(second.elderMode.first())
        assertFalse(second.treeholeEnabled.first())
        assertTrue(second.privacyAccepted.first())

        second.resetAll()
        assertFalse(second.elderMode.first())
        assertTrue(second.treeholeEnabled.first())
        assertFalse(second.privacyAccepted.first())
        assertEquals(DataStoreSettingsRepository.DEFAULT_NICKNAME, second.nickname.first())
        assertNull(second.avatarUri.first())
    }

    @Test
    fun nicknameBoundary_rulesGatePersistedValue() {
        // 个人中心保存前先用规则校验 1/12/13 边界，超出不进入仓库
        assertNull(ProfileRules.nicknameError("澜"))
        assertNull(ProfileRules.nicknameError("一二三四五六七八九十甲乙"))
        assertTrue(ProfileRules.nicknameError("一二三四五六七八九十甲乙丙") != null)
        assertTrue(ProfileRules.nicknameError("") != null)
    }

    @Test
    fun nicknameRoundTripThroughSharedRepo() = runBlocking {
        val repository = DataStoreSettingsRepository(dataStore, delayMillis = 0L)
        repository.setNickname("王阿姨")
        assertEquals("王阿姨", repository.nickname.first())
        repository.resetAll()
        assertEquals(DataStoreSettingsRepository.DEFAULT_NICKNAME, repository.nickname.first())
    }
}
