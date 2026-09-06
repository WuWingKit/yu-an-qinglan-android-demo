/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.policy.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 政策条目的级别/类别标签。 */
@Serializable
enum class PolicyLevel(val label: String) {
    @SerialName("国家级")
    NATIONAL("国家级"),

    @SerialName("市级")
    CITY("市级"),

    @SerialName("办理提示")
    TIP("办理提示"),

    @SerialName("注意事项")
    NOTICE("注意事项"),
}

/** 政策补贴/办理提示条目。 */
@Serializable
data class PolicyArticle(
    val id: String,
    val level: PolicyLevel,
    val title: String,
    val summary: String,
)

/** 区县信息（38 区县）。 */
@Serializable
data class County(
    val id: String,
    val name: String,
    /** 所属片区：中心城区 / 主城新区 / 渝东北 / 渝东南。 */
    val zone: String,
    val brief: String,
    val policySummary: String,
    val processTips: String,
)

/** 公益海葬指引中的一个流程步骤。 */
@Serializable
data class SeaFlowStep(
    val title: String,
    val detail: String,
)

/** 公益海葬指引整页内容。 */
@Serializable
data class SeaGuide(
    val title: String,
    val introParagraphs: List<String> = emptyList(),
    val flowTitle: String = "一般流程",
    val flowSteps: List<SeaFlowStep> = emptyList(),
    val applyTitle: String = "报名与登记方式",
    val applyParagraph: String = "",
    val notices: List<String> = emptyList(),
    val complianceNote: String = "",
)
