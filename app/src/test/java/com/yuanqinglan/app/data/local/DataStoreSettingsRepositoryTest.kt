/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
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
 * [DataStoreSettingsRepository] 单元测试：基于真实 DataStore（临时文件）在 JVM 运行，
 * 覆盖默认值、elderMode/nickname 读写与 resetAll 恢复默认。
 */
class DataStoreSettingsRepositoryTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: DataStoreSettingsRepository
    private lateinit var testFile: File
    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        testFile = File.createTempFile("yuanqinglan_settings_test", ".preferences_pb")
        // DataStore 要求文件尚不存在（由自身创建）。
        testFile.delete()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { testFile }
        repository = DataStoreSettingsRepository(dataStore, delayMillis = 0L)
    }

    @After
    fun tearDown() {
        scope.cancel()
        testFile.delete()
    }

    @Test
    fun defaultsMatchContract() = runBlocking {
        assertFalse(repository.elderMode.first())
        assertTrue(repository.treeholeEnabled.first())
        assertEquals(DataStoreSettingsRepository.DEFAULT_NICKNAME, repository.nickname.first())
        assertNull(repository.avatarUri.first())
        assertFalse(repository.privacyAccepted.first())
    }

    @Test
    fun elderModeRoundTrip() = runBlocking {
        repository.setElderMode(true)
        assertTrue(repository.elderMode.first())

        repository.setElderMode(false)
        assertFalse(repository.elderMode.first())
    }

    @Test
    fun nicknameRoundTrip() = runBlocking {
        repository.setNickname("王阿姨")
        assertEquals("王阿姨", repository.nickname.first())

        repository.setNickname("渝安青澜用户")
        assertEquals(DataStoreSettingsRepository.DEFAULT_NICKNAME, repository.nickname.first())
    }

    @Test
    fun avatarUriRoundTripWithNullReset() = runBlocking {
        assertNull(repository.avatarUri.first())

        repository.setAvatarUri("file:///data/user/0/com.yuanqinglan.app/files/yuanqinglan/images/1.webp")
        assertEquals(
            "file:///data/user/0/com.yuanqinglan.app/files/yuanqinglan/images/1.webp",
            repository.avatarUri.first(),
        )

        repository.setAvatarUri(null)
        assertNull(repository.avatarUri.first())
    }

    @Test
    fun privacyAcceptedRoundTrip() = runBlocking {
        repository.setPrivacyAccepted(true)
        assertTrue(repository.privacyAccepted.first())

        repository.setPrivacyAccepted(false)
        assertFalse(repository.privacyAccepted.first())
    }

    @Test
    fun resetAllRestoresDefaults() = runBlocking {
        repository.setElderMode(true)
        repository.setTreeholeEnabled(false)
        repository.setNickname("测试昵称")
        repository.setAvatarUri("file:///data/user/0/com.yuanqinglan.app/files/yuanqinglan/images/a.webp")
        repository.setPrivacyAccepted(true)

        repository.resetAll()

        assertFalse(repository.elderMode.first())
        assertTrue(repository.treeholeEnabled.first())
        assertEquals(DataStoreSettingsRepository.DEFAULT_NICKNAME, repository.nickname.first())
        assertNull(repository.avatarUri.first())
        assertFalse(repository.privacyAccepted.first())
    }
}
