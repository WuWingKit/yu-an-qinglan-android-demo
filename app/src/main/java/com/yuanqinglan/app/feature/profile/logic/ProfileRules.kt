/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.profile.logic

import java.security.MessageDigest

/**
 * 个人中心纯逻辑规则：昵称边界、密码/手机号校验、反馈表单规则与本地摘要。
 *
 * 全部为无副作用纯函数，便于单元测试覆盖边界（1/12/13、6/20 位等）。
 * 校验错误统一返回中文提示；通过时返回 null。
 */
object ProfileRules {

    /** 昵称长度边界（按字符数，汉字/字母/数字均计 1 字）。 */
    const val NICKNAME_MIN = 1
    const val NICKNAME_MAX = 12

    /** 本地新密码边界与强度要求（本机流程不依赖真实账号体系）。 */
    const val PASSWORD_MIN = 6
    const val PASSWORD_MAX = 20

    /** 大陆手机号规则（与安葬预约表单保持一致）。 */
    const val PHONE_PATTERN = "^1[3-9]\\d{9}$"

    /** 本地初始密码（仅用于本机无账号体系的流程；修改后按摘要存本机）。 */
    const val DEFAULT_LOCAL_PASSWORD = "12345678"

    // ---------------- 昵称 ----------------

    fun nicknameError(input: String): String? {
        val value = input.trim()
        if (value.isEmpty()) return "昵称不能为空"
        val length = value.codePointCount(0, value.length)
        return when {
            length < NICKNAME_MIN -> "昵称不能为空"
            length > NICKNAME_MAX -> "昵称最多 $NICKNAME_MAX 个字"
            else -> null
        }
    }

    // ---------------- 密码 ----------------

    /** 新密码强度：长度 + 至少含一个字母与一个数字。 */
    fun newPasswordError(candidate: String): String? {
        val length = candidate.length
        if (length < PASSWORD_MIN || length > PASSWORD_MAX) {
            return "密码长度需为 $PASSWORD_MIN-$PASSWORD_MAX 位"
        }
        if (!candidate.any { it.isLetter() } || !candidate.any { it.isDigit() }) {
            return "密码需同时包含字母和数字"
        }
        return null
    }

    fun confirmPasswordError(newPassword: String, confirm: String): String? =
        if (newPassword != confirm) "两次输入的密码不一致" else null

    /**
     * 修改密码整体校验。返回字段错误映射；全部通过返回空映射。
     * [currentPasswordMatches] 由存储层按当前本地摘要判断（不在这里存状态）。
     */
    fun passwordEditErrors(
        oldPassword: String,
        newPassword: String,
        confirm: String,
        currentPasswordMatches: Boolean,
    ): Map<String, String> {
        val errors = linkedMapOf<String, String>()
        if (!currentPasswordMatches) {
            errors["old"] = "原密码不正确"
        }
        newPasswordError(newPassword)?.let { errors["new"] = it }
        confirmPasswordError(newPassword, confirm)?.let { errors["confirm"] = it }
        if (newPassword == oldPassword && oldPassword.isNotEmpty()) {
            errors["new"] = "新密码不能与原密码相同"
        }
        return errors
    }

    /** SHA-256 摘要（hex），用于本地保存密码，避免明文落盘。 */
    fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    // ---------------- 手机号 ----------------

    fun phoneFormatError(value: String): String? =
        if (Regex(PHONE_PATTERN).matches(value.trim())) null else "请输入有效的 11 位手机号"

    /**
     * 更换手机号整体校验。
     * [boundPhone] 为本机已绑定号码。已绑定时必须填写并一致；
     * 未绑定时（首次绑定）不要求原号，仅校验新号与验证码。
     */
    fun phoneEditErrors(
        oldPhone: String,
        newPhone: String,
        code: String,
        generatedCode: String,
        boundPhone: String?,
    ): Map<String, String> {
        val errors = linkedMapOf<String, String>()
        if (boundPhone != null) {
            if (oldPhone.isBlank()) {
                errors["old"] = "请输入原手机号"
            } else if (oldPhone.trim() != boundPhone) {
                errors["old"] = "原手机号与本机预留号码不一致"
            } else {
                phoneFormatError(oldPhone)?.let { errors["old"] = it }
            }
        }
        if (newPhone.isBlank()) {
            errors["new"] = "请输入新手机号"
        } else {
            phoneFormatError(newPhone)?.let { errors["new"] = it }
        }
        if (boundPhone != null && oldPhone.trim() == newPhone.trim() && newPhone.isNotBlank()) {
            errors["new"] = "新手机号不能与原手机号相同"
        }
        if (generatedCode.isBlank()) {
            errors["code"] = "请先获取验证码"
        } else if (code.isBlank()) {
            errors["code"] = "请输入验证码"
        } else if (code.trim() != generatedCode) {
            errors["code"] = "验证码不正确"
        }
        return errors
    }

    // ---------------- 意见反馈 ----------------

    const val FEEDBACK_BODY_MIN = 1
    const val FEEDBACK_BODY_MAX = 500
    const val FEEDBACK_ATTACHMENT_MAX = 3

    fun feedbackBodyError(body: String): String? {
        val value = body.trim()
        return when {
            value.isEmpty() -> "请填写反馈内容"
            value.length > FEEDBACK_BODY_MAX -> "反馈内容不能超过 $FEEDBACK_BODY_MAX 字"
            else -> null
        }
    }
}
