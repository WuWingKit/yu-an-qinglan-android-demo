/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial.model

/**
 * 宠物生态安葬服务（宠物树葬 / 花葬 / 草坪葬）。
 *
 * 与 [HumanBurialService] 强类型隔离：宠物服务拥有独立字段语义
 * （园区介绍 parkIntro 面向独立宠物园区），且服务首屏固定承载
 * [PetCompliance] 四条合规说明：无害化处理前置、无民政补贴、
 * 独立园区、人宠场地物理隔离。
 */
data class PetBurialService(
    val id: String,
    val mode: BurialMode,
    val name: String,
    val subtitle: String,
    val priceRange: String,
    /** 图片资源名（如 burial_pet_tree），由 UI 层映射为 drawable。 */
    val image: String,
    val description: String,
    /** 独立宠物园区环境介绍（pet-park 页使用）。 */
    val parkIntro: String,
    val process: List<String>,
    val defaultPlanId: String,
)

/** 在宠物服务强类型列表中按 id 查找；找不到返回 null（由调用方转空态）。 */
fun List<PetBurialService>.petById(id: String): PetBurialService? =
    firstOrNull { it.id == id }

/** 在宠物服务强类型列表中按模式查找；找不到返回 null（由调用方转空态）。 */
fun List<PetBurialService>.petByMode(mode: BurialMode): PetBurialService? =
    firstOrNull { it.mode == mode }

/**
 * 宠物服务固定合规说明（首屏固定展示，不来自业务 JSON，避免漏配）。
 * 对外口径克制：明确无民政补贴、独立园区与物理隔离。
 */
object PetCompliance {
    val notices: List<String> = listOf(
        "无害化处理前置：宠物安葬前须按相关规定完成无害化处理。",
        "无民政补贴：宠物安葬不属于民政生态安葬补贴范围。",
        "独立园区：宠物服务使用独立园区，与人类安葬区分区管理。",
        "场地隔离：人宠安葬场地物理隔离，互不共用安葬区域。",
    )

    /** 拼为单条多行文案供 NoticeBanner 展示。 */
    val bannerText: String
        get() = notices.joinToString("\n")
}
