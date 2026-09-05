/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.profile

import com.yuanqinglan.app.feature.profile.logic.ProfileRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 个人中心纯逻辑规则测试：昵称边界、密码/手机号/反馈校验。 */
class ProfileRulesTest {

    // ---------------- 昵称边界 1 / 12 / 13 ----------------

    @Test
    fun nickname_blank_isRejected() {
        assertEquals("昵称不能为空", ProfileRules.nicknameError(""))
        assertEquals("昵称不能为空", ProfileRules.nicknameError("   "))
    }

    @Test
    fun nickname_singleChar_ok() {
        assertNull(ProfileRules.nicknameError("澜"))
        assertNull(ProfileRules.nicknameError(" 澜 "))
    }

    @Test
    fun nickname_12Chars_ok() {
        assertNull(ProfileRules.nicknameError("一二三四五六七八九十甲乙"))
    }

    @Test
    fun nickname_13Chars_rejected() {
        assertTrue(ProfileRules.nicknameError("一二三四五六七八九十甲乙丙")!!.contains("12"))
    }

    // ---------------- 密码 ----------------

    @Test
    fun newPassword_tooShort_rejected() {
        assertEquals("密码长度需为 6-20 位", ProfileRules.newPasswordError("Ab1"))
    }

    @Test
    fun newPassword_missingLetter_rejected() {
        assertEquals("密码需同时包含字母和数字", ProfileRules.newPasswordError("123456"))
    }

    @Test
    fun newPassword_missingDigit_rejected() {
        assertEquals("密码需同时包含字母和数字", ProfileRules.newPasswordError("abcdef"))
    }

    @Test
    fun newPassword_valid_ok() {
        assertNull(ProfileRules.newPasswordError("abc123"))
    }

    @Test
    fun newPassword_tooLong_rejected() {
        assertEquals("密码长度需为 6-20 位", ProfileRules.newPasswordError("a1".repeat(11)))
    }

    @Test
    fun confirmMismatch_rejected() {
        assertEquals("两次输入的密码不一致", ProfileRules.confirmPasswordError("abc123", "abc124"))
        assertNull(ProfileRules.confirmPasswordError("abc123", "abc123"))
    }

    @Test
    fun passwordEdit_wrongOld_fails() {
        val errors = ProfileRules.passwordEditErrors(
            oldPassword = "wrong",
            newPassword = "newpass1",
            confirm = "newpass1",
            currentPasswordMatches = false,
        )
        assertEquals("原密码不正确", errors["old"])
    }

    @Test
    fun passwordEdit_sameAsOld_fails() {
        val errors = ProfileRules.passwordEditErrors(
            oldPassword = "abc123",
            newPassword = "abc123",
            confirm = "abc123",
            currentPasswordMatches = true,
        )
        assertEquals("新密码不能与原密码相同", errors["new"])
    }

    @Test
    fun passwordEdit_allValid_ok() {
        val errors = ProfileRules.passwordEditErrors(
            oldPassword = "abc123",
            newPassword = "newpass1",
            confirm = "newpass1",
            currentPasswordMatches = true,
        )
        assertTrue(errors.isEmpty())
    }

    @Test
    fun sha256_deterministic() {
        val first = ProfileRules.sha256Hex("abc123")
        val second = ProfileRules.sha256Hex("abc123")
        assertEquals(first, second)
        assertEquals(64, first.length)
    }

    // ---------------- 手机号 ----------------

    @Test
    fun phoneFormat_valid11() {
        assertNull(ProfileRules.phoneFormatError("13800138000"))
    }

    @Test
    fun phoneFormat_invalid_rejected() {
        assertEquals("请输入有效的 11 位手机号", ProfileRules.phoneFormatError("12345"))
        assertEquals("请输入有效的 11 位手机号", ProfileRules.phoneFormatError("23800138000"))
        assertEquals("请输入有效的 11 位手机号", ProfileRules.phoneFormatError("1380013800a"))
    }

    @Test
    fun phoneEdit_firstBinding_skipsOld() {
        val errors = ProfileRules.phoneEditErrors(
            oldPhone = "",
            newPhone = "13900139000",
            code = "123456",
            generatedCode = "123456",
            boundPhone = null,
        )
        assertTrue(errors.isEmpty())
    }

    @Test
    fun phoneEdit_oldMismatch_whenBound_fails() {
        val errors = ProfileRules.phoneEditErrors(
            oldPhone = "13800138000",
            newPhone = "13900139000",
            code = "123456",
            generatedCode = "123456",
            boundPhone = "13700137000",
        )
        assertEquals("原手机号与本机预留号码不一致", errors["old"])
    }

    @Test
    fun phoneEdit_newSameAsOld_fails() {
        val errors = ProfileRules.phoneEditErrors(
            oldPhone = "13800138000",
            newPhone = "13800138000",
            code = "123456",
            generatedCode = "123456",
            boundPhone = "13800138000",
        )
        assertEquals("新手机号不能与原手机号相同", errors["new"])
    }

    @Test
    fun phoneEdit_wrongCode_fails() {
        val errors = ProfileRules.phoneEditErrors(
            oldPhone = "13800138000",
            newPhone = "13900139000",
            code = "654321",
            generatedCode = "123456",
            boundPhone = "13800138000",
        )
        assertEquals("验证码不正确", errors["code"])
    }

    @Test
    fun phoneEdit_codeNotRequested_fails() {
        val errors = ProfileRules.phoneEditErrors(
            oldPhone = "13800138000",
            newPhone = "13900139000",
            code = "",
            generatedCode = "",
            boundPhone = "13800138000",
        )
        assertEquals("请先获取验证码", errors["code"])
    }

    // ---------------- 意见反馈 ----------------

    @Test
    fun feedbackBody_blank_rejected() {
        assertEquals("请填写反馈内容", ProfileRules.feedbackBodyError(""))
    }

    @Test
    fun feedbackBody_over500_rejected() {
        assertEquals("反馈内容不能超过 500 字", ProfileRules.feedbackBodyError("好".repeat(501)))
    }

    @Test
    fun feedbackBody_valid_ok() {
        assertNull(ProfileRules.feedbackBodyError("希望增加更多内容。"))
    }
}
