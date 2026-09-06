/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial.model

import java.time.LocalDate

/** 表单字段（供校验报告与首错聚焦使用）。 */
enum class BurialFormField {
    DECEASED_NAME,
    BIRTH_DATE,
    DEATH_DATE,
    RELATION,
    CONTACT_NAME,
    PHONE,
    EXPECT_DATE,
    CONSENT,
    PET_NICKNAME,
}

/** 人类预约表单的纯数据字段。 */
data class HumanPlanFormInput(
    val deceasedName: String = "",
    val birthDate: LocalDate? = null,
    val deathDate: LocalDate? = null,
    val relation: String = "",
    val contactName: String = "",
    val phone: String = "",
    val expectDate: LocalDate? = null,
    val consent: Boolean = false,
)

/** 宠物预约表单的纯数据字段（无出生日期、无关系，昵称代替姓名）。 */
data class PetPlanFormInput(
    val petNickname: String = "",
    val deathDate: LocalDate? = null,
    val contactName: String = "",
    val phone: String = "",
    val expectDate: LocalDate? = null,
    val consent: Boolean = false,
)

/** 校验结果：字段级错误 + 首个需聚焦字段。 */
data class BurialFormReport(
    val errors: Map<BurialFormField, String>,
    val firstFocus: BurialFormField?,
) {
    val isValid: Boolean get() = errors.isEmpty()
}

/**
 * 预约表单本地校验规则（纯函数，便于单元测试）。
 * 规则：必填、手机号格式（11 位大陆手机号）、日期合理性（离世不晚于今天、
 * 出生不晚于离世、期望日期不早于今天）、服务确认勾选。
 */
object BurialFormRules {
    const val PHONE_PATTERN = "^1[3-9]\\d{9}$"

    private fun isBlank(v: String): Boolean = v.trim().isEmpty()

    fun validateHuman(input: HumanPlanFormInput, today: LocalDate): BurialFormReport {
        val errors = linkedMapOf<BurialFormField, String>()

        if (isBlank(input.deceasedName)) errors[BurialFormField.DECEASED_NAME] = "请填写逝者姓名"
        if (input.birthDate == null) {
            errors[BurialFormField.BIRTH_DATE] = "请选择出生日期"
        } else if (input.birthDate.isAfter(today)) {
            errors[BurialFormField.BIRTH_DATE] = "出生日期不能晚于今天"
        }
        if (input.deathDate == null) {
            errors[BurialFormField.DEATH_DATE] = "请选择离世日期"
        } else {
            if (input.deathDate.isAfter(today)) {
                errors[BurialFormField.DEATH_DATE] = "离世日期不能晚于今天"
            } else if (input.birthDate != null && input.deathDate.isBefore(input.birthDate)) {
                errors[BurialFormField.DEATH_DATE] = "离世日期不能早于出生日期"
            }
        }
        if (isBlank(input.relation)) errors[BurialFormField.RELATION] = "请填写与逝者的关系"

        fillContactAndConsent(input.contactName, input.phone, input.expectDate, input.consent, today, errors)
        return BurialFormReport(errors, errors.keys.firstOrNull())
    }

    fun validatePet(input: PetPlanFormInput, today: LocalDate): BurialFormReport {
        val errors = linkedMapOf<BurialFormField, String>()

        if (isBlank(input.petNickname)) errors[BurialFormField.PET_NICKNAME] = "请填写宠物昵称"
        if (input.deathDate == null) {
            errors[BurialFormField.DEATH_DATE] = "请选择离世日期"
        } else if (input.deathDate.isAfter(today)) {
            errors[BurialFormField.DEATH_DATE] = "离世日期不能晚于今天"
        }

        fillContactAndConsent(input.contactName, input.phone, input.expectDate, input.consent, today, errors)
        return BurialFormReport(errors, errors.keys.firstOrNull())
    }

    private fun fillContactAndConsent(
        contactName: String,
        phone: String,
        expectDate: LocalDate?,
        consent: Boolean,
        today: LocalDate,
        errors: LinkedHashMap<BurialFormField, String>,
    ) {
        if (isBlank(contactName)) errors[BurialFormField.CONTACT_NAME] = "请填写联系人姓名"
        if (isBlank(phone)) {
            errors[BurialFormField.PHONE] = "请填写联系人手机号"
        } else if (!Regex(PHONE_PATTERN).matches(phone.trim())) {
            errors[BurialFormField.PHONE] = "请输入有效的 11 位手机号"
        }
        if (expectDate == null) {
            errors[BurialFormField.EXPECT_DATE] = "请选择期望日期"
        } else if (expectDate.isBefore(today)) {
            errors[BurialFormField.EXPECT_DATE] = "期望日期不能早于今天"
        }
        if (!consent) errors[BurialFormField.CONSENT] = "请先阅读并确认服务说明"
    }
}
