/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.yuanqinglan.app.feature.profile.data.DataStoreProfileLocalStore
import com.yuanqinglan.app.feature.profile.logic.ProfileRules
import com.yuanqinglan.app.feature.profile.model.FeedbackRecord
import com.yuanqinglan.app.feature.profile.model.FeedbackType
import com.yuanqinglan.app.feature.profile.model.LocalMaterialEntry
import com.yuanqinglan.app.feature.profile.model.ProfileMaterialKind
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

/** [DataStoreProfileLocalStore] 持久化测试：隐私开关/密码/手机号/素材/反馈。 */
class DataStoreProfileLocalStoreTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var store: DataStoreProfileLocalStore
    private lateinit var testFile: File
    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        testFile = File.createTempFile("yuanqinglan_profile_test", ".preferences_pb")
        testFile.delete()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { testFile }
        store = DataStoreProfileLocalStore(dataStore, delayMillis = 0L)
    }

    @After
    fun tearDown() {
        scope.cancel()
        testFile.delete()
    }

    @Test
    fun privacyToggles_defaultsAndRoundTrip() = runBlocking {
        assertFalse(store.allowBrowsePrefs.first())
        assertTrue(store.treeholeAnonymous.first())

        store.setAllowBrowsePrefs(true)
        store.setTreeholeAnonymous(false)
        assertTrue(store.allowBrowsePrefs.first())
        assertFalse(store.treeholeAnonymous.first())

        store.setAllowBrowsePrefs(false)
        assertFalse(store.allowBrowsePrefs.first())
    }

    @Test
    fun boundPhone_roundTripAndClear() = runBlocking {
        assertNull(store.boundPhone.first())

        store.setBoundPhone("13900139000")
        assertEquals("13900139000", store.boundPhone.first())

        store.setBoundPhone(null)
        assertNull(store.boundPhone.first())
    }

    @Test
    fun password_initialPresetVerifies_thenChangeAndReset() = runBlocking {
        // 未设置过：初始预设通过
        assertTrue(store.verifyPassword(ProfileRules.DEFAULT_LOCAL_PASSWORD))
        assertFalse(store.verifyPassword("wrong"))

        store.setNewPassword("abc123")
        assertTrue(store.verifyPassword("abc123"))
        assertFalse(store.verifyPassword(ProfileRules.DEFAULT_LOCAL_PASSWORD))

        store.resetLocalPassword()
        assertTrue(store.verifyPassword(ProfileRules.DEFAULT_LOCAL_PASSWORD))
    }

    @Test
    fun materials_addAndRemove_persisted() = runBlocking {
        val entry = LocalMaterialEntry(
            id = "m1",
            kind = ProfileMaterialKind.IMAGE,
            uri = "file:///data/user/0/com.yuanqinglan.app/files/yuanqinglan/images/m1.jpg",
            name = "本地图片 1",
            createdAtMillis = 1234L,
        )
        store.addMaterial(entry)
        assertEquals(listOf(entry), store.materials.first())

        store.removeMaterial("m1")
        assertTrue(store.materials.first().isEmpty())
    }

    @Test
    fun feedback_addKeepsLatestAndCapsAt20() = runBlocking {
        for (i in 1..25) {
            store.addFeedback(
                FeedbackRecord(
                    id = "f$i",
                    typeLabel = FeedbackType.FUNCTION.label,
                    body = "反馈 $i",
                    submittedAtMillis = i.toLong(),
                ),
            )
        }
        val records = store.feedbackRecords.first()
        assertEquals(20, records.size)
        assertEquals("f25", records.last().id)
        assertEquals("f6", records.first().id)
    }
}
