/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.profile

import com.yuanqinglan.app.feature.profile.data.ProfileMediaImporter
import com.yuanqinglan.app.feature.profile.model.ProfileMaterialKind
import com.yuanqinglan.app.feature.profile.testutil.FakeMediaImporter
import com.yuanqinglan.app.feature.profile.testutil.FakeProfileLocalStore
import com.yuanqinglan.app.feature.profile.testutil.FakeSettingsRepository
import com.yuanqinglan.app.feature.profile.ui.ElderModeViewModel
import com.yuanqinglan.app.feature.profile.ui.FeedbackViewModel
import com.yuanqinglan.app.feature.profile.ui.MaterialViewModel
import com.yuanqinglan.app.feature.profile.ui.MeViewModel
import com.yuanqinglan.app.feature.profile.ui.PasswordEditViewModel
import com.yuanqinglan.app.feature.profile.ui.PhoneEditViewModel
import com.yuanqinglan.app.feature.profile.ui.PrivacyViewModel
import com.yuanqinglan.app.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** me/elder/privacy 设置链路 ViewModel 测试。 */
class ProfileSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun elderMode_togglePersistsToSharedRepo() = runBlocking {
        val settings = FakeSettingsRepository()
        val vm = ElderModeViewModel(settings)

        assertFalse(settings.elderMode.value)
        vm.setElderMode(true)
        assertTrue(settings.elderMode.value)
        vm.setElderMode(false)
        assertFalse(settings.elderMode.value)
    }

    @Test
    fun me_treeholeTogglePersists() = runBlocking {
        val settings = FakeSettingsRepository()
        val vm = MeViewModel(settings, FakeMediaImporter())

        assertTrue(settings.treeholeEnabled.value)
        vm.setTreeholeEnabled(false)
        assertFalse(settings.treeholeEnabled.value)
        vm.setTreeholeEnabled(true)
        assertTrue(settings.treeholeEnabled.value)
    }

    @Test
    fun me_saveNickname_valid_trimsAndPersists() = runBlocking {
        val settings = FakeSettingsRepository()
        val vm = MeViewModel(settings, FakeMediaImporter())

        val ok = vm.saveNickname("  王阿姨  ")
        assertTrue(ok)
        assertEquals("王阿姨", settings.nickname.value)
    }

    @Test
    fun me_saveNickname_tooLong_rejected() = runBlocking {
        val settings = FakeSettingsRepository()
        val vm = MeViewModel(settings, FakeMediaImporter())

        val ok = vm.saveNickname("一二三四五六七八九十甲乙丙")
        assertFalse(ok)
        assertEquals("渝安青澜用户", settings.nickname.value)
    }

    @Test
    fun me_importAvatar_updatesUriAndDeletesOld() = runBlocking {
        val settings = FakeSettingsRepository()
        val importer = FakeMediaImporter()
        settings.avatarUri.value = "file:///old/avatar.jpg"
        val vm = MeViewModel(settings, importer)

        vm.importAvatar("content:///picker/1")
        assertEquals("file:///data/user/0/com.yuanqinglan.app/files/yuanqinglan/images/test.jpg", settings.avatarUri.value)
        assertTrue(importer.deletedUris.contains("file:///old/avatar.jpg"))
    }

    @Test
    fun me_importAvatar_failure_keepsOld() = runBlocking {
        val settings = FakeSettingsRepository()
        val importer = FakeMediaImporter().apply { importImageFail = true }
        settings.avatarUri.value = "file:///old/avatar.jpg"
        val vm = MeViewModel(settings, importer)

        vm.importAvatar("content:///picker/1")
        assertEquals("file:///old/avatar.jpg", settings.avatarUri.value)
    }

    @Test
    fun me_resetAll_restoresDefaultsAndRemovesAvatarFile() = runBlocking {
        val settings = FakeSettingsRepository().apply {
            elderMode.value = true
            treeholeEnabled.value = false
            nickname.value = "测试"
            avatarUri.value = "file:///avatar.jpg"
            privacyAccepted.value = true
        }
        val importer = FakeMediaImporter()
        val vm = MeViewModel(settings, importer)

        vm.resetAll()

        assertFalse(settings.elderMode.value)
        assertTrue(settings.treeholeEnabled.value)
        assertEquals("渝安青澜用户", settings.nickname.value)
        assertNull(settings.avatarUri.value)
        assertFalse(settings.privacyAccepted.value)
        assertTrue(importer.deletedUris.contains("file:///avatar.jpg"))
    }

    @Test
    fun privacy_togglesPersist() = runBlocking {
        val settings = FakeSettingsRepository()
        val local = FakeProfileLocalStore()
        val vm = PrivacyViewModel(settings, local)

        vm.setAllowBrowsePrefs(false)
        vm.setTreeholeAnonymous(false)
        assertFalse(local.allowBrowsePrefs.first())
        assertFalse(local.treeholeAnonymous.first())

        vm.setPrivacyAccepted(true)
        assertTrue(settings.privacyAccepted.value)
    }
}

/** pwd-edit / phone-edit / feedback / materials 表单校验 VM 测试。 */
class ProfileFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun passwordEdit_wrongOld_showsError() = runBlocking {
        val local = FakeProfileLocalStore()
        val vm = PasswordEditViewModel(local)

        vm.onOldChange("wrong")
        vm.onNewChange("abc123")
        vm.onConfirmChange("abc123")
        vm.submit()

        assertEquals("原密码不正确", vm.errors.value["old"])
        assertFalse(vm.success.value)
    }

    @Test
    fun passwordEdit_valid_savesNewPassword() = runBlocking {
        val local = FakeProfileLocalStore()
        val vm = PasswordEditViewModel(local)

        vm.onOldChange("12345678")
        vm.onNewChange("abc123")
        vm.onConfirmChange("abc123")
        vm.submit()

        assertTrue(vm.errors.value.isEmpty())
        assertTrue(vm.success.value)
        assertTrue(local.verifyPassword("abc123"))
    }

    @Test
    fun phoneEdit_firstBinding_valid_submits() = runBlocking {
        val local = FakeProfileLocalStore()
        val vm = PhoneEditViewModel(local, codeGenerator = { "123456" })

        vm.onNewChange("13900139000")
        vm.requestCode()
        vm.onCodeChange("123456")
        vm.submit()

        assertTrue(vm.errors.value.isEmpty())
        assertTrue(vm.success.value)
        assertEquals("13900139000", local.boundPhone.first())
    }

    @Test
    fun phoneEdit_wrongCode_showsError() = runBlocking {
        val local = FakeProfileLocalStore()
        val vm = PhoneEditViewModel(local, codeGenerator = { "123456" })

        vm.onNewChange("13900139000")
        vm.requestCode()
        vm.onCodeChange("999999")
        vm.submit()

        assertEquals("验证码不正确", vm.errors.value["code"])
        assertFalse(vm.success.value)
    }

    @Test
    fun phoneEdit_boundOldMismatch_showsError() = runBlocking {
        val local = FakeProfileLocalStore().apply { setBoundPhone("13800138000") }
        val vm = PhoneEditViewModel(local, codeGenerator = { "123456" })

        vm.onOldChange("13700137000")
        vm.onNewChange("13900139000")
        vm.requestCode()
        vm.onCodeChange("123456")
        vm.submit()

        assertEquals("原手机号与本机预留号码不一致", vm.errors.value["old"])
    }

    @Test
    fun feedback_bodyRequiredAndMax() = runBlocking {
        val local = FakeProfileLocalStore()
        val vm = FeedbackViewModel(local, FakeMediaImporter())

        vm.submit()
        assertEquals("请填写反馈内容", vm.bodyError.value)
        assertFalse(vm.received.value)

        vm.onBodyChange("好".repeat(501))
        assertEquals(500, vm.body.value.length)

        vm.onBodyChange(" 希望改进功能 ")
        vm.submit()
        assertTrue(vm.received.value)
        assertEquals("功能建议", local.feedbackRecords.first().firstOrNull()?.typeLabel)
    }

    @Test
    fun feedback_attachmentLimitApplied() = runBlocking {
        val local = FakeProfileLocalStore()
        val importer = FakeMediaImporter()
        val vm = FeedbackViewModel(local, importer)

        vm.attachImages(listOf("content://a", "content://b", "content://c", "content://d"))
        assertEquals(3, vm.attachments.value.size)
    }

    @Test
    fun materials_importAndDeleteFlow() = runBlocking {
        val local = FakeProfileLocalStore()
        val importer = FakeMediaImporter()
        val vm = MaterialViewModel(local, importer)

        vm.importImage("content:///img")
        val entry = local.materials.first().single()
        assertEquals(ProfileMaterialKind.IMAGE, entry.kind)

        vm.deleteMaterial(entry)
        assertTrue(local.materials.first().isEmpty())
        assertTrue(importer.deletedUris.contains(entry.uri))
    }
}
