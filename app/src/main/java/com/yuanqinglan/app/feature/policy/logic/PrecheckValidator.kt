/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.policy.logic

import com.yuanqinglan.app.feature.policy.model.PrecheckField
import com.yuanqinglan.app.feature.policy.model.PrecheckFieldError
import com.yuanqinglan.app.feature.policy.model.PrecheckForm

/**
 * 政策预审表单本地校验：
 * - 必填项：区县、申请身份、与逝者关系、计划安葬方式；
 * - 格式项：联系电话（选填）需为 11 位手机号。
 * 校验只做本地提示，不提交、不暗示办理结果。
 */
object PrecheckValidator {

    /** 中国大陆手机号（宽松校验：1 开头、共 11 位数字）。 */
    private val phonePattern = Regex("^1\\d{10}$")

    fun validate(
        form: PrecheckForm,
        knownCountyIds: Set<String>,
    ): List<PrecheckFieldError> = buildList {
        if (form.countyId.isBlank() || form.countyId !in knownCountyIds) {
            add(PrecheckFieldError(PrecheckField.COUNTY, "请选择所在区县"))
        }
        if (form.applicantType.isBlank()) {
            add(PrecheckFieldError(PrecheckField.APPLICANT, "请选择申请身份类型"))
        }
        if (form.relationType.isBlank()) {
            add(PrecheckFieldError(PrecheckField.RELATION, "请选择与逝者的关系"))
        }
        if (form.burialMode.isBlank()) {
            add(PrecheckFieldError(PrecheckField.MODE, "请选择计划安葬方式"))
        }
        val phone = form.contactPhone.trim()
        if (phone.isNotEmpty() && !phonePattern.matches(phone)) {
            add(PrecheckFieldError(PrecheckField.PHONE, "联系电话需为 11 位手机号（可不填写）"))
        }
    }
}
