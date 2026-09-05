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
    ;

    companion object {
        fun fromTokenOrNull(raw: String?): PlanTier? =
            entries.firstOrNull { it.token.equals(raw, ignoreCase = true) }
    }
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
)

/** 在套餐列表中按 id 查找。 */
fun List<BurialPlan>.planById(id: String): BurialPlan? =
    firstOrNull { it.id == id }

/** 取某服务的套餐；基础款在前、升级款在后展示。 */
fun List<BurialPlan>.plansOfService(serviceId: String): List<BurialPlan> =
    filter { it.serviceId == serviceId }
        .sortedWith(compareBy<BurialPlan> { if (it.tier == PlanTier.BASIC) 0 else 1 })
