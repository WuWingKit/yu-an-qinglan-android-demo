/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.home.model

import kotlinx.serialization.Serializable

/**
 * 智能匹配问卷数据模型。
 * 题目与选项的规则写在本地常量（[com.yuanqinglan.app.feature.home.logic.MatchCatalog]）中，
 * 页面作答后由匹配引擎按本地规则给出推荐结果。
 */

/** 单个问题的一个选项。 */
@Serializable
data class MatchOption(
    val value: String,
    val label: String,
    val hint: String? = null,
)

/** 问卷中的一道单选题。 */
@Serializable
data class MatchQuestion(
    val id: String,
    val title: String,
    val options: List<MatchOption>,
)

/** 匹配推荐结果。 */
data class MatchRecommendation(
    /** 推荐标题，例如"建议了解：树葬"。 */
    val title: String,
    /** 一句话总结推荐依据。 */
    val summary: String,
    /** 推荐理由列表。 */
    val reasons: List<String>,
    /** 下一步可做的事。 */
    val nextActions: List<String>,
    /** 匹配的目标模式：tree / flower / lawn / sea / general。 */
    val targetKey: String,
)

/** 常见安葬模式显示名。 */
object BurialModeLabels {
    const val TREE = "树葬"
    const val FLOWER = "花葬"
    const val LAWN = "草坪葬"
    const val SEA = "公益海葬"
    const val GENERAL = "节地生态安葬"
}
