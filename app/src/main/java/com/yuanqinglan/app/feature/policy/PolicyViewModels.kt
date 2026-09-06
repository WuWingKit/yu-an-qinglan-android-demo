/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.policy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.feature.policy.data.PolicyCatalogSource
import com.yuanqinglan.app.feature.policy.data.loadPolicyListState
import com.yuanqinglan.app.feature.policy.logic.CountyIndex
import com.yuanqinglan.app.feature.policy.logic.PrecheckValidator
import com.yuanqinglan.app.feature.policy.logic.SubsidyEstimator
import com.yuanqinglan.app.feature.policy.model.County
import com.yuanqinglan.app.feature.policy.model.PolicyArticle
import com.yuanqinglan.app.feature.policy.model.PrecheckFieldError
import com.yuanqinglan.app.feature.policy.model.PrecheckForm
import com.yuanqinglan.app.feature.policy.model.SeaGuide
import com.yuanqinglan.app.feature.policy.model.SubsidyEstimate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 政策页的两种浏览模式：政策列表 / 区县查询列表。 */
enum class PolicyPageMode {
    POLICIES,
    COUNTIES,
}

/** 政策补贴 + 区县查询页 ViewModel。 */
class PolicyViewModel(
    source: PolicyCatalogSource,
) : ViewModel() {

    private val _policiesState = MutableStateFlow<DemoState<List<PolicyArticle>>>(DemoState.Loading)
    val policiesState: StateFlow<DemoState<List<PolicyArticle>>> = _policiesState.asStateFlow()

    private val _countiesState = MutableStateFlow<DemoState<List<County>>>(DemoState.Loading)
    val countiesState: StateFlow<DemoState<List<County>>> = _countiesState.asStateFlow()

    private val _mode = MutableStateFlow(PolicyPageMode.POLICIES)
    val mode: StateFlow<PolicyPageMode> = _mode.asStateFlow()

    private val _countyQuery = MutableStateFlow("")
    val countyQuery: StateFlow<String> = _countyQuery.asStateFlow()

    private val catalogSource = source

    init {
        reloadPolicies()
        reloadCounties()
    }

    fun openCounties() {
        _mode.value = PolicyPageMode.COUNTIES
    }

    fun backToPolicies() {
        _mode.value = PolicyPageMode.POLICIES
    }

    fun updateCountyQuery(query: String) {
        _countyQuery.value = query
    }

    /** 当前查询下的区县列表（搜索为空时返回全部 38 个）。 */
    fun filteredCounties(): List<County> {
        val counties = (_countiesState.value as? DemoState.Success)?.value.orEmpty()
        return CountyIndex.search(counties, _countyQuery.value)
    }

    fun reloadPolicies() {
        viewModelScope.launch {
            _policiesState.value = DemoState.Loading
            _policiesState.value = loadPolicyListState(
                { catalogSource.loadPolicies() },
                "政策信息加载失败，请稍后重试。",
            )
        }
    }

    fun reloadCounties() {
        viewModelScope.launch {
            _countiesState.value = DemoState.Loading
            _countiesState.value = loadPolicyListState(
                { catalogSource.loadCounties() },
                "区县信息加载失败，请稍后重试。",
            )
        }
    }
}

/** 区县详情 ViewModel：按 id 加载；id 无效给出明确错误。 */
class CountyDetailViewModel(
    source: PolicyCatalogSource,
    private val countyId: String,
) : ViewModel() {

    private val _countyState = MutableStateFlow<DemoState<County>>(DemoState.Loading)
    val countyState: StateFlow<DemoState<County>> = _countyState.asStateFlow()

    private val catalogSource = source

    init {
        load(countyId)
    }

    fun load(id: String) {
        viewModelScope.launch {
            _countyState.value = DemoState.Loading
            _countyState.value = try {
                val counties = catalogSource.loadCounties()
                val county = CountyIndex.findById(counties, id)
                if (county == null) {
                    DemoState.Error("未找到该区县信息，请返回重试。")
                } else {
                    DemoState.Success(county)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DemoState.Error("区县信息加载失败，请稍后重试。")
            }
        }
    }
}

/** 公益海葬指引 ViewModel。 */
class SeaDetailViewModel(
    source: PolicyCatalogSource,
) : ViewModel() {

    private val _guideState = MutableStateFlow<DemoState<SeaGuide>>(DemoState.Loading)
    val guideState: StateFlow<DemoState<SeaGuide>> = _guideState.asStateFlow()

    private val catalogSource = source

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            _guideState.value = DemoState.Loading
            _guideState.value = try {
                DemoState.Success(catalogSource.loadSeaGuide())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DemoState.Error("指引加载失败，请稍后重试。")
            }
        }
    }
}

/** 政策预审页的两种阶段：填写表单 / 查看测算结果。 */
enum class PrecheckPhase {
    FORM,
    RESULT,
}

/**
 * 政策预审 ViewModel：表单（本地校验）→ 参考测算结果 → 可"重新填写"。
 * 全程本地状态，不自动提交任何个人信息。
 */
class PrecheckViewModel(
    source: PolicyCatalogSource,
    private val prefillCountyId: String,
) : ViewModel() {

    private val _countiesState = MutableStateFlow<DemoState<List<County>>>(DemoState.Loading)
    val countiesState: StateFlow<DemoState<List<County>>> = _countiesState.asStateFlow()

    private val _form = MutableStateFlow(
        PrecheckForm(countyId = prefillCountyId.takeIf { it.isNotBlank() }.orEmpty()),
    )
    val form: StateFlow<PrecheckForm> = _form.asStateFlow()

    private val _errors = MutableStateFlow<List<PrecheckFieldError>>(emptyList())
    val errors: StateFlow<List<PrecheckFieldError>> = _errors.asStateFlow()

    private val _phase = MutableStateFlow(PrecheckPhase.FORM)
    val phase: StateFlow<PrecheckPhase> = _phase.asStateFlow()

    private val _estimate = MutableStateFlow<SubsidyEstimate?>(null)
    val estimate: StateFlow<SubsidyEstimate?> = _estimate.asStateFlow()

    /** 提交时定格的结果区县名称（结果页展示用）。 */
    private val _resultCountyName = MutableStateFlow("")
    val resultCountyName: StateFlow<String> = _resultCountyName.asStateFlow()

    private val catalogSource = source

    init {
        reloadCounties()
    }

    fun reloadCounties() {
        viewModelScope.launch {
            _countiesState.value = DemoState.Loading
            _countiesState.value = loadPolicyListState(
                { catalogSource.loadCounties() },
                "区县信息加载失败，请稍后重试。",
            )
            // 区县列表刷新后重新校验一次预填项。
            validateSilently()
        }
    }

    fun updateCounty(countyId: String) {
        _form.update { it.copy(countyId = countyId) }
    }

    fun updateApplicantType(value: String) {
        _form.update { it.copy(applicantType = value) }
    }

    fun updateRelationType(value: String) {
        _form.update { it.copy(relationType = value) }
    }

    fun updateBurialMode(value: String) {
        _form.update { it.copy(burialMode = value) }
    }

    fun updateContactPhone(value: String) {
        _form.update { it.copy(contactPhone = value) }
    }

    fun updateRemark(value: String) {
        _form.update { it.copy(remark = value) }
    }

    /** 校验通过则进入结果阶段并完成测算；否则保留错误提示。返回是否通过。 */
    fun submit(): Boolean {
        val errors = PrecheckValidator.validate(_form.value, countyIds())
        _errors.value = errors
        if (errors.isNotEmpty()) return false
        val form = _form.value
        val countyName = counties().firstOrNull { it.id == form.countyId }?.name
            ?: CountyIndex.cleanTitle(form.countyId)
        _resultCountyName.value = countyName
        _estimate.value = SubsidyEstimator.estimate(form.burialMode, countyName)
        _phase.value = PrecheckPhase.RESULT
        return true
    }

    /** 重新填写：回到表单并清空（保留预选区县参数以方便再次测算）。 */
    fun restart() {
        _form.value = PrecheckForm(countyId = prefillCountyId.takeIf { it.isNotBlank() }.orEmpty())
        _errors.value = emptyList()
        _estimate.value = null
        _resultCountyName.value = ""
        _phase.value = PrecheckPhase.FORM
    }

    fun errorMessage(field: com.yuanqinglan.app.feature.policy.model.PrecheckField): String? =
        _errors.value.firstOrNull { it.field == field }?.message

    private fun counties(): List<County> =
        (_countiesState.value as? DemoState.Success)?.value.orEmpty()

    private fun countyIds(): Set<String> = counties().mapTo(mutableSetOf()) { it.id }

    private fun validateSilently() {
        _errors.value = PrecheckValidator.validate(_form.value, countyIds())
    }
}
