/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial.model

import com.yuanqinglan.app.core.model.AudienceTrack

/** 套餐档位。 */
enum class PlanTier(val token: String, val label: String) {
    BASIC("basic", "基础款"),
    UPGRADE("upgrade", "升级款"),
    SIMPLE("simple", "简·归"),
    SINCERE("sincere", "诚·念"),
    PREMIUM("premium", "至·念"),
    ;

    companion object {
        fun fromTokenOrNull(raw: String?): PlanTier? =
            entries.firstOrNull { it.token.equals(raw, ignoreCase = true) }
    }
}

/** 套餐可选费用项。 */
data class PlanPriceOption(
    val id: String,
    val label: String,
    val priceYuan: Int,
    val years: Int = 0,
)

/** 套餐费用试算结果。 */
data class PlanQuote(
    val planPriceYuan: Int,
    val prepaidYears: Int,
    val prepaidManagementYuan: Int,
    val selectedAddOns: List<PlanPriceOption>,
    val addOnYuan: Int,
    val subsidyYuan: Int,
) {
    val totalYuan: Int = (planPriceYuan + prepaidManagementYuan + addOnYuan - subsidyYuan)
        .coerceAtLeast(0)
}

/**
 * 安葬套餐。带强类型 [AudienceTrack] 与所属服务 ID：
 * - 人类套餐 serviceId ∈ {tree, flower, grass}；
 * - 宠物套餐 serviceId ∈ {pet-tree, pet-flower, pet-lawn}；
 * 套餐 ID 全量唯一，plan-form 可直接按 ID 反查套餐并推导轨道，
 * 关键字段不跨轨道串值。mode 由 audience+serviceId 强类型推导并在建单时校验。
 */
data class BurialPlan(
    val id: String,
    val serviceId: String,
    val audience: AudienceTrack,
    val mode: BurialMode,
    val tier: PlanTier,
    val title: String,
    val priceText: String,
    val contents: List<String>,
    val priceYuan: Int? = null,
    val includedManagementYears: Int = 0,
    val renewalStartYear: Int = 0,
    val renewalAnnualYuan: Int = 0,
    val highlight: String = "",
    val subsidyYuan: Int = 0,
    val subsidyNote: String = "",
    val managementPrepay: List<PlanPriceOption> = emptyList(),
    val addOns: List<PlanPriceOption> = emptyList(),
    val excludedFees: List<String> = emptyList(),
    val purchaseLimit: String = "",
)

/** 根据选择计算套餐实付金额；找不到的选项会被忽略。 */
fun BurialPlan.quote(
    prepaidYears: Int,
    selectedAddOnIds: Set<String>,
    applySubsidy: Boolean,
): PlanQuote {
    val prepay = managementPrepay.firstOrNull { it.years == prepaidYears }
    val selected = addOns.filter { it.id in selectedAddOnIds }
    return PlanQuote(
        planPriceYuan = priceYuan ?: 0,
        prepaidYears = prepay?.years ?: 0,
        prepaidManagementYuan = prepay?.priceYuan ?: 0,
        selectedAddOns = selected,
        addOnYuan = selected.sumOf { it.priceYuan },
        subsidyYuan = if (applySubsidy) subsidyYuan else 0,
    )
}

/** 在套餐列表中按 id 查找。 */
fun List<BurialPlan>.planById(id: String): BurialPlan? =
    firstOrNull { it.id == id }

/** 取某服务的套餐；按档位从基础到高阶展示。 */
fun List<BurialPlan>.plansOfService(serviceId: String): List<BurialPlan> =
    filter { it.serviceId == serviceId }
        .sortedBy {
            when (it.tier) {
                PlanTier.BASIC, PlanTier.SIMPLE -> 0
                PlanTier.UPGRADE, PlanTier.SINCERE -> 1
                PlanTier.PREMIUM -> 2
            }
        }
