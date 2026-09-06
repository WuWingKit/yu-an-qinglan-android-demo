/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.policy

import com.yuanqinglan.app.feature.policy.data.PolicyCatalogSource
import com.yuanqinglan.app.feature.policy.model.County
import com.yuanqinglan.app.feature.policy.model.PolicyArticle
import com.yuanqinglan.app.feature.policy.model.PolicyLevel
import com.yuanqinglan.app.feature.policy.model.SeaGuide
import com.yuanqinglan.app.testutil.MainDispatcherRule
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** 预审 ViewModel 状态机：失败态可重试；校验失败停在表单；成功后进入结果页并记录区县名。 */
class PrecheckViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sampleCounty = County(
        id = "cq-yuzhong",
        name = "渝中区",
        zone = "中心城区",
        brief = "简介",
        policySummary = "政策摘要",
        processTips = "办理提示",
    )

    private inner class FakePolicyCatalogSource(
        var failCounties: Boolean = false,
    ) : PolicyCatalogSource {
        override suspend fun loadPolicies(): List<PolicyArticle> =
            listOf(PolicyArticle("pol-1", PolicyLevel.CITY, "标题", "摘要"))

        override suspend fun loadCounties(): List<County> {
            if (failCounties) throw IOException("counties boom")
            return listOf(sampleCounty)
        }

        override suspend fun loadSeaGuide(): SeaGuide = SeaGuide(title = "公益海葬指引")
    }

    @Test
    fun `submit with invalid form stays in form with errors`() {
        val vm = PrecheckViewModel(source = FakePolicyCatalogSource(), prefillCountyId = "")
        assertTrue(vm.countiesState.value is com.yuanqinglan.app.core.model.DemoState.Success)

        val ok = vm.submit()
        assertTrue(!ok)
        assertEquals(PrecheckPhase.FORM, vm.phase.value)
        assertTrue(vm.errors.value.isNotEmpty())
        assertNull(vm.estimate.value)
    }

    @Test
    fun `valid form enters result phase with county name and estimate`() {
        val vm = PrecheckViewModel(source = FakePolicyCatalogSource(), prefillCountyId = "cq-yuzhong")
        vm.updateApplicantType("spouse-children")
        vm.updateRelationType("spouse")
        vm.updateBurialMode("lawn")

        val ok = vm.submit()
        assertTrue(ok)
        assertEquals(PrecheckPhase.RESULT, vm.phase.value)
        assertEquals("渝中区", vm.resultCountyName.value)
        assertTrue(vm.estimate.value?.hasMatch == true)
        assertEquals(1000, vm.estimate.value?.totalYuan)
    }

    @Test
    fun `restart returns to form keeping prefill county`() {
        val vm = PrecheckViewModel(source = FakePolicyCatalogSource(), prefillCountyId = "cq-yuzhong")
        vm.updateApplicantType("spouse-children")
        vm.updateRelationType("spouse")
        vm.updateBurialMode("tree")
        vm.submit()
        assertEquals(PrecheckPhase.RESULT, vm.phase.value)

        vm.restart()
        assertEquals(PrecheckPhase.FORM, vm.phase.value)
        assertEquals("cq-yuzhong", vm.form.value.countyId)
        assertNull(vm.estimate.value)
        assertTrue(vm.errors.value.isEmpty())
        assertEquals("", vm.form.value.applicantType)
    }

    @Test
    fun `county load failure is retryable and form becomes usable after retry`() {
        val source = FakePolicyCatalogSource(failCounties = true)
        val vm = PrecheckViewModel(source = source, prefillCountyId = "")
        assertTrue(vm.countiesState.value is com.yuanqinglan.app.core.model.DemoState.Error)

        source.failCounties = false
        vm.reloadCounties()
        assertTrue(vm.countiesState.value is com.yuanqinglan.app.core.model.DemoState.Success)
    }

    @Test
    fun `unknown prefill county id is rejected by validation`() {
        val vm = PrecheckViewModel(source = FakePolicyCatalogSource(), prefillCountyId = "cq-unknown")
        vm.updateApplicantType("spouse-children")
        vm.updateRelationType("spouse")
        vm.updateBurialMode("tree")
        val ok = vm.submit()
        assertTrue(!ok)
        assertTrue(vm.errorMessage(com.yuanqinglan.app.feature.policy.model.PrecheckField.COUNTY) != null)
        assertEquals(PrecheckPhase.FORM, vm.phase.value)
    }

    @Test
    fun `phone format error blocks submit until fixed`() {
        val vm = PrecheckViewModel(source = FakePolicyCatalogSource(), prefillCountyId = "cq-yuzhong")
        vm.updateApplicantType("spouse-children")
        vm.updateRelationType("spouse")
        vm.updateBurialMode("tree")
        vm.updateContactPhone("123")

        assertTrue(!vm.submit())
        assertEquals(PrecheckPhase.FORM, vm.phase.value)

        vm.updateContactPhone("13800138000")
        assertTrue(vm.submit())
        assertEquals(PrecheckPhase.RESULT, vm.phase.value)
    }
}
