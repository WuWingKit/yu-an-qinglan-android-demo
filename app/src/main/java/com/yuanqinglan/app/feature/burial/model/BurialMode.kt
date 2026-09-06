/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial.model

import com.yuanqinglan.app.core.model.AudienceTrack

/**
 * 葬式模式。人类与宠物服务共用受控枚举值，但各自以强类型服务模型承载，
 * 导航参数（pet-tree / pet-park 的 ?mode=）一律经 [parseRouteMode] 解析，
 * 非法或缺失值回退 [BurialMode.TREE]，避免把错误配置带入详情页。
 */
enum class BurialMode(val token: String, val label: String) {
    TREE("TREE", "树葬"),
    FLOWER("FLOWER", "花葬"),
    LAWN("LAWN", "草坪葬"),
    ;

    companion object {
        fun fromTokenOrNull(raw: String?): BurialMode? =
            entries.firstOrNull { it.token.equals(raw, ignoreCase = true) }

        /** 路由参数解析：受控枚举 + 回退树葬。 */
        fun parseRouteMode(raw: String?): BurialMode = fromTokenOrNull(raw) ?: TREE
    }
}

/**
 * 由轨道 + 模式推导稳定的服务标识（强类型推导，杜绝跨轨字符串过滤）。
 * 人类标识遵循契约路由键：tree / flower / grass（草坪葬对应 grass）；
 * 宠物标识：pet-tree / pet-flower / pet-lawn。
 */
fun serviceIdFor(track: AudienceTrack, mode: BurialMode): String = when (track) {
    AudienceTrack.HUMAN -> when (mode) {
        BurialMode.TREE -> "tree"
        BurialMode.FLOWER -> "flower"
        BurialMode.LAWN -> "grass"
    }
    AudienceTrack.PET -> "pet-${mode.token.lowercase()}"
}

/** 由轨道 + 服务标识反推模式；标识不匹配返回 null（调用方转为错误/回退）。 */
fun modeOfServiceId(track: AudienceTrack, serviceId: String): BurialMode? = when (track) {
    AudienceTrack.HUMAN -> when (serviceId.lowercase()) {
        "tree" -> BurialMode.TREE
        "flower" -> BurialMode.FLOWER
        "grass" -> BurialMode.LAWN
        else -> null
    }
    AudienceTrack.PET -> BurialMode.fromTokenOrNull(serviceId.removePrefix("pet-"))
}

/** 对外展示的服务名（人类：树葬…；宠物：宠物树葬…）。 */
fun serviceDisplayName(track: AudienceTrack, mode: BurialMode): String = when (track) {
    AudienceTrack.HUMAN -> mode.label
    AudienceTrack.PET -> "宠物${mode.label}"
}
