/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.policy

import com.yuanqinglan.app.feature.policy.logic.PrecheckValidator
import com.yuanqinglan.app.feature.policy.model.PrecheckField
import com.yuanqinglan.app.feature.policy.model.PrecheckForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 政策预审表单本地校验测试（必填 / 格式 / 错误提示）。 */
class PrecheckValidatorTest {

    private val knownCounties = setOf("cq-yuzhong", "cq-yubei")

    private fun validForm() = PrecheckForm(
        countyId = "cq-yuzhong",
        applicantType = "spouse-children",
        relationType = "spouse",
        burialMode = "tree",
    )

    @Test
    fun `fully filled valid form passes`() {
        assertTrue(PrecheckValidator.validate(validForm(), knownCounties).isEmpty())
    }

    @Test
    fun `empty form reports all required fields with clear messages`() {
        val errors = PrecheckValidator.validate(PrecheckForm(), knownCounties)
        val fields = errors.map { it.field }.toSet()
        assertEquals(
            setOf(PrecheckField.COUNTY, PrecheckField.APPLICANT, PrecheckField.RELATION, PrecheckField.MODE),
            fields,
        )
        assertTrue(errors.any { it.message.contains("区县") })
        assertTrue(errors.any { it.message.contains("身份") })
        assertTrue(errors.any { it.message.contains("关系") })
        assertTrue(errors.any { it.message.contains("安葬方式") })
    }

    @Test
    fun `county not in list is rejected`() {
        val form = validForm().copy(countyId = "not-in-list")
        val errors = PrecheckValidator.validate(form, knownCounties)
        assertEquals(listOf(PrecheckField.COUNTY), errors.map { it.field })
    }

    @Test
    fun `blank phone passes because it is optional`() {
        assertTrue(PrecheckValidator.validate(validForm().copy(contactPhone = "  "), knownCounties).isEmpty())
    }

    @Test
    fun `invalid phone formats produce format error`() {
        listOf("12345", "123456789012", "abcdefghijk", "110", "1234567890a").forEach { bad ->
            val errors = PrecheckValidator.validate(validForm().copy(contactPhone = bad), knownCounties)
            val phoneError = errors.firstOrNull { it.field == PrecheckField.PHONE }
            assertTrue("手机号 [$bad] 应报格式错误", phoneError != null)
            assertTrue(phoneError!!.message.contains("11 位手机号"))
        }
    }

    @Test
    fun `valid mainland mobile passes format check`() {
        val errors = PrecheckValidator.validate(validForm().copy(contactPhone = "13800138000"), knownCounties)
        assertTrue(errors.none { it.field == PrecheckField.PHONE })
    }
}
