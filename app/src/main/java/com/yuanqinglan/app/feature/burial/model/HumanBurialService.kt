/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial.model

/**
 * 人类生态安葬服务（树葬 / 花葬 / 草坪葬）。
 *
 * 与 [PetBurialService] 完全独立：人类服务来自独立 JSON 文件与独立数据流，
 * 不使用可空字段或字符串过滤复用宠物列表。字段按人类办理语义建模：
 * 流程（process）、适用对象（applicable）、生态说明等均与宠物服务不同。
 */
data class HumanBurialService(
    val id: String,
    val mode: BurialMode,
    val name: String,
    val subtitle: String,
    val priceRange: String,
    /** 图片资源名（如 burial_tree_grove），由 UI 层映射为 drawable。 */
    val image: String,
    val description: String,
    val ecoDescription: String,
    val process: List<String>,
    val applicable: String,
    /** 人类服务默认套餐（基础款）ID，用于“立即预约”入口。 */
    val defaultPlanId: String,
)

/** 在人类服务强类型列表中按 id 查找；找不到返回 null（由调用方转空态）。 */
fun List<HumanBurialService>.humanById(id: String): HumanBurialService? =
    firstOrNull { it.id == id }
