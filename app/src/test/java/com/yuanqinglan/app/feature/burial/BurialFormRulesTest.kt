/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial

import com.yuanqinglan.app.feature.burial.model.BurialFormField
import com.yuanqinglan.app.feature.burial.model.BurialFormRules
import com.yuanqinglan.app.feature.burial.model.HumanPlanFormInput
import com.yuanqinglan.app.feature.burial.model.PetPlanFormInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** 预约表单本地校验：必填、手机号格式、日期合理性、确认勾选。 */
class BurialFormRulesTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 5)

    private fun validHuman() = HumanPlanFormInput(
        deceasedName = "李明",
        birthDate = LocalDate.of(1950, 3, 1),
        deathDate = LocalDate.of(2026, 8, 20),
        relation = "子女",
        contactName = "李华",
        phone = "13800138000",
        expectDate = LocalDate.of(2026, 9, 20),
        consent = true,
    )

    private fun validPet() = PetPlanFormInput(
        petNickname = "团团",
        deathDate = LocalDate.of(2026, 8, 20),
        contactName = "王芳",
        phone = "13900139000",
        expectDate = LocalDate.of(2026, 9, 25),
        consent = true,
    )

    @Test
    fun `人类表单填写完整时通过`() {
        val report = BurialFormRules.validateHuman(validHuman(), today)
        assertTrue(report.isValid)
        assertNull(report.firstFocus)
    }

    @Test
    fun `宠物表单填写完整时通过`() {
        val report = BurialFormRules.validatePet(validPet(), today)
        assertTrue(report.isValid)
        assertNull(report.firstFocus)
    }

    @Test
    fun `人类必填缺失逐项报错并给出首错聚焦`() {
        val report = BurialFormRules.validateHuman(HumanPlanFormInput(), today)
        assertFalse(report.isValid)
        assertEquals(
            setOf(
                BurialFormField.DECEASED_NAME,
                BurialFormField.BIRTH_DATE,
                BurialFormField.DEATH_DATE,
                BurialFormField.RELATION,
                BurialFormField.CONTACT_NAME,
                BurialFormField.PHONE,
                BurialFormField.EXPECT_DATE,
                BurialFormField.CONSENT,
            ),
            report.errors.keys,
        )
        assertEquals(BurialFormField.DECEASED_NAME, report.firstFocus)
    }

    @Test
    fun `宠物必填缺失逐项报错`() {
        val report = BurialFormRules.validatePet(PetPlanFormInput(), today)
        assertFalse(report.isValid)
        assertEquals(BurialFormField.PET_NICKNAME, report.firstFocus)
        assertTrue(report.errors.containsKey(BurialFormField.DEATH_DATE))
        assertTrue(report.errors.containsKey(BurialFormField.PHONE))
    }

    @Test
    fun `手机号格式校验`() {
        // 空手机号 → 必填错误
        var report = BurialFormRules.validateHuman(validHuman().copy(phone = ""), today)
        assertEquals("请填写联系人手机号", report.errors[BurialFormField.PHONE])

        // 长度/开头非法
        report = BurialFormRules.validateHuman(validHuman().copy(phone = "12345"), today)
        assertEquals("请输入有效的 11 位手机号", report.errors[BurialFormField.PHONE])

        report = BurialFormRules.validateHuman(validHuman().copy(phone = "23800138000"), today)
        assertEquals("请输入有效的 11 位手机号", report.errors[BurialFormField.PHONE])

        // 合法手机号
        report = BurialFormRules.validateHuman(validHuman().copy(phone = "13712345678"), today)
        assertNull(report.errors[BurialFormField.PHONE])
    }

    @Test
    fun `日期合理性校验`() {
        // 离世晚于今天
        var report = BurialFormRules.validateHuman(
            validHuman().copy(deathDate = LocalDate.of(2026, 10, 1)),
            today,
        )
        assertEquals("离世日期不能晚于今天", report.errors[BurialFormField.DEATH_DATE])

        // 出生晚于离世
        report = BurialFormRules.validateHuman(
            validHuman().copy(
                birthDate = LocalDate.of(2026, 8, 21),
                deathDate = LocalDate.of(2026, 8, 20),
            ),
            today,
        )
        assertEquals("离世日期不能早于出生日期", report.errors[BurialFormField.DEATH_DATE])

        // 出生晚于今天
        report = BurialFormRules.validateHuman(
            validHuman().copy(birthDate = LocalDate.of(2027, 1, 1)),
            today,
        )
        assertEquals("出生日期不能晚于今天", report.errors[BurialFormField.BIRTH_DATE])

        // 期望日期早于今天
        report = BurialFormRules.validateHuman(
            validHuman().copy(expectDate = today.minusDays(1)),
            today,
        )
        assertEquals("期望日期不能早于今天", report.errors[BurialFormField.EXPECT_DATE])

        // 宠物离世日期晚于今天
        val petReport = BurialFormRules.validatePet(
            validPet().copy(deathDate = today.plusDays(1)),
            today,
        )
        assertEquals("离世日期不能晚于今天", petReport.errors[BurialFormField.DEATH_DATE])
    }

    @Test
    fun `未勾选服务确认时拦截`() {
        val report = BurialFormRules.validateHuman(validHuman().copy(consent = false), today)
        assertFalse(report.isValid)
        assertEquals(
            "请先阅读并确认服务说明",
            report.errors[BurialFormField.CONSENT],
        )
    }

    @Test
    fun `手机号正则表达式覆盖主流号段`() {
        val pattern = BurialFormRules.PHONE_PATTERN.toRegex()
        assertTrue(pattern.matches("13512345678"))
        assertTrue(pattern.matches("19912345678"))
        assertFalse(pattern.matches("10012345678"))
        assertFalse(pattern.matches("1381234567"))
        assertFalse(pattern.matches("138123456789"))
        assertFalse(pattern.matches("1381234567a"))
    }
}
