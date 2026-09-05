/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial.data

import com.yuanqinglan.app.core.model.AudienceTrack
import com.yuanqinglan.app.feature.burial.model.BurialMode
import com.yuanqinglan.app.feature.burial.model.BurialPlan
import com.yuanqinglan.app.feature.burial.model.HumanBurialService
import com.yuanqinglan.app.feature.burial.model.PetBurialService
import com.yuanqinglan.app.feature.burial.model.PlanTier
import com.yuanqinglan.app.feature.burial.model.modeOfServiceId
import kotlinx.serialization.Serializable

/*
 * 本地业务 JSON 的强类型 DTO。
 *
 * 人类与宠物使用完全独立的两套 DTO/文件/映射，任何轨道数据不共享列表；
 * 映射到领域模型时填充强类型轨道（AudienceTrack / BurialMode / PlanTier），
 * 未知枚举值直接抛出带上下文的错误，避免静默串轨。
 */

@Serializable
internal data class HumanServiceFile(val services: List<HumanBurialServiceDto>)

@Serializable
internal data class HumanBurialServiceDto(
    val id: String,
    val mode: String,
    val name: String,
    val subtitle: String,
    val priceRange: String,
    val image: String,
    val description: String,
    val ecoDescription: String,
    val process: List<String> = emptyList(),
    val applicable: String = "",
    val defaultPlanId: String = "",
)

@Serializable
internal data class PetServiceFile(val services: List<PetBurialServiceDto>)

@Serializable
internal data class PetBurialServiceDto(
    val id: String,
    val mode: String,
    val name: String,
    val subtitle: String,
    val priceRange: String,
    val image: String,
    val description: String,
    val parkIntro: String,
    val process: List<String> = emptyList(),
    val defaultPlanId: String = "",
)

@Serializable
internal data class BurialPlansFile(val plans: List<BurialPlanDto>)

@Serializable
internal data class BurialPlanDto(
    val id: String,
    val serviceId: String,
    val tier: String,
    val title: String,
    val priceText: String,
    val contents: List<String> = emptyList(),
)

// ---------------- DTO -> 领域模型（纯映射，轨道由调用侧显式传入） ----------------

internal fun HumanBurialServiceDto.toDomain(): HumanBurialService = HumanBurialService(
    id = id,
    mode = modeOf(mode, "人类服务 ${id ?: "<unknown>"}"),
    name = name,
    subtitle = subtitle,
    priceRange = priceRange,
    image = image,
    description = description,
    ecoDescription = ecoDescription,
    process = process,
    applicable = applicable,
    defaultPlanId = defaultPlanId,
)

internal fun PetBurialServiceDto.toDomain(): PetBurialService = PetBurialService(
    id = id,
    mode = modeOf(mode, "宠物服务 ${id ?: "<unknown>"}"),
    name = name,
    subtitle = subtitle,
    priceRange = priceRange,
    image = image,
    description = description,
    parkIntro = parkIntro,
    process = process,
    defaultPlanId = defaultPlanId,
)

internal fun BurialPlanDto.toDomain(audience: AudienceTrack): BurialPlan {
    val tier = PlanTier.fromTokenOrNull(tier)
        ?: throw IllegalArgumentException("套餐 ${id ?: "<unknown>"} 档位配置无效: $tier")
    val mode = modeOfServiceId(audience, serviceId)
        ?: throw IllegalArgumentException("套餐 ${id ?: "<unknown>"} 的 serviceId 与轨道不符: $serviceId")
    return BurialPlan(
        id = id,
        serviceId = serviceId,
        audience = audience,
        mode = mode,
        tier = tier,
        title = title,
        priceText = priceText,
        contents = contents,
    )
}

private fun modeOf(raw: String?, where: String): BurialMode =
    BurialMode.fromTokenOrNull(raw)
        ?: throw IllegalArgumentException("$where 的 mode 配置无效: $raw")
