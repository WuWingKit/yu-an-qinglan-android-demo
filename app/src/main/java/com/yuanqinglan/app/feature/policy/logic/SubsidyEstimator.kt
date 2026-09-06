/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.policy.logic

import com.yuanqinglan.app.feature.policy.model.SubsidyEstimate
import com.yuanqinglan.app.feature.policy.model.SubsidyLine

/**
 * 补贴参考测算（本地规则）。
 *
 * 规则说明：页面只按"是否节地生态安葬 / 是否公益海葬"输出参考口径，
 * 金额为内置参考数值，不构成任何机构承诺；结果页必须叠加合规说明。
 */
object SubsidyEstimator {

    /** 参考标准：节地生态安葬补贴（元/例）。 */
    private const val EcoBurialReferenceYuan = 1000

    fun estimate(mode: String, countyName: String): SubsidyEstimate {
        val countyText = countyName.ifBlank { "所在区县" }
        return when (mode) {
            "tree", "flower", "lawn" -> SubsidyEstimate(
                hasMatch = true,
                lines = listOf(
                    SubsidyLine(
                        title = "节地生态安葬补贴（参考）",
                        amountYuan = EcoBurialReferenceYuan,
                        description = "面向选择树葬、花葬、草坪葬等节地生态安葬方式并符合当地申请条件的对象；" +
                            "金额为内置参考口径，实际以$countyText 民政部门当年公布与核定为准。",
                    ),
                ),
                totalYuan = EcoBurialReferenceYuan,
                notes = listOf(
                    "补贴申领需安葬地园区在当年补贴范围之内，并按要求备齐身份、火化与安葬证明等材料。",
                    "本测算不构成办理承诺，请以$countyText 民政部门答复为准。",
                ),
            )
            "sea" -> SubsidyEstimate(
                hasMatch = false,
                lines = listOf(
                    SubsidyLine(
                        title = "公益海葬（公益项目）",
                        amountYuan = 0,
                        description = "公益海葬由民政部门或授权机构按年度统一组织，不设补贴金额，" +
                            "参与批次与费用安排以组织单位当年度公告为准。",
                    ),
                ),
                totalYuan = 0,
                notes = listOf(
                    "公益海葬登记以官方渠道为准，谨防以海葬名义索取个人信息或转账。",
                    "本测算不构成参与确认，请以$countyText 民政部门公告为准。",
                ),
            )
            "traditional" -> SubsidyEstimate(
                hasMatch = false,
                lines = emptyList(),
                totalYuan = 0,
                notes = listOf(
                    "传统安葬方式不属于本页节地生态安葬补贴参考范围。",
                    "如需了解传统安葬的服务内容与费用，请咨询殡仪服务机构并核对书面协议。",
                ),
            )
            else -> SubsidyEstimate(
                hasMatch = false,
                lines = emptyList(),
                totalYuan = 0,
                notes = listOf(
                    "暂未收录该安葬方式的参考信息，请以$countyText 民政部门答复为准。",
                ),
            )
        }
    }
}
