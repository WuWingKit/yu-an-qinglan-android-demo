/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.home.logic

import com.yuanqinglan.app.feature.home.model.BurialModeLabels
import com.yuanqinglan.app.feature.home.model.MatchOption
import com.yuanqinglan.app.feature.home.model.MatchQuestion
import com.yuanqinglan.app.feature.home.model.MatchRecommendation

/** 智能匹配问卷题目与选项（本地常量规则）。 */
object MatchCatalog {

    const val Q_REGION = "q-region"
    const val Q_BUDGET = "q-budget"
    const val Q_PREF = "q-pref"
    const val Q_MEMORIAL = "q-memorial"

    val questions: List<MatchQuestion> = listOf(
        MatchQuestion(
            id = Q_REGION,
            title = "计划安葬的地区在哪里？",
            options = listOf(
                MatchOption("region-center", "中心城区"),
                MatchOption("region-new", "主城新区"),
                MatchOption("region-ne", "渝东北区县"),
                MatchOption("region-se", "渝东南区县"),
                MatchOption("region-unsure", "暂不确定"),
            ),
        ),
        MatchQuestion(
            id = Q_BUDGET,
            title = "预算范围大致是？",
            options = listOf(
                MatchOption("budget-low", "一万元以下"),
                MatchOption("budget-mid", "一万元至三万元"),
                MatchOption("budget-high", "三万元以上"),
                MatchOption("budget-na", "暂未考虑预算"),
            ),
        ),
        MatchQuestion(
            id = Q_PREF,
            title = "更倾向哪种安葬形式？",
            options = listOf(
                MatchOption("pref-tree", "树木林地相伴"),
                MatchOption("pref-flower", "花开四季的花园"),
                MatchOption("pref-lawn", "开阔安静的草坪"),
                MatchOption("pref-traditional", "偏传统纪念形式"),
                MatchOption("pref-unsure", "还没有想好"),
            ),
        ),
        MatchQuestion(
            id = Q_MEMORIAL,
            title = "希望如何安放思念？",
            options = listOf(
                MatchOption("mem-natural", "有树木花草长期相伴"),
                MatchOption("mem-nomarker", "不保留独立标识，回归自然"),
                MatchOption("mem-collective", "参加集体纪念活动"),
                MatchOption("mem-unsure", "暂不确定"),
            ),
        ),
    )
}

/**
 * 本地规则匹配引擎：根据问卷作答给出推荐葬式、理由与下一步。
 * 规则全部为本地常量，不涉及真实政策结论；推荐内容克制、可解释，
 * 任何输入组合（含缺省/未知选项）都会返回推荐，不会抛错。
 */
object MatchEngine {

    /** @param answers 题目 id -> 选项 value。未知题目与未知选项会被忽略。 */
    fun recommend(answers: Map<String, String>): MatchRecommendation {
        val region = answers[MatchCatalog.Q_REGION]?.trim().orEmpty()
        val budget = answers[MatchCatalog.Q_BUDGET]?.trim().orEmpty()
        val pref = answers[MatchCatalog.Q_PREF]?.trim().orEmpty()
        val memorial = answers[MatchCatalog.Q_MEMORIAL]?.trim().orEmpty()

        val regionLine = regionText(region)

        // 已有明确倾向：尊重倾向，同时按纪念需求补充说明。
        when (pref) {
            "pref-tree" -> return tree(
                "您倾向树木林地相伴。树葬以纪念树作为寄托，适合安静、持续的思念。$regionLine",
                budgetLine(budget),
            )
            "pref-flower" -> return flower(
                "您倾向花开四季的花园。花葬随花境更替呈现四季景致，公共花园便于日常探访。$regionLine",
                budgetLine(budget),
            )
            "pref-lawn" -> return lawn(
                "您倾向开阔安静的草坪。草坪葬保持公共空间的完整与宁静，祭扫习惯也能自然延续。$regionLine",
                budgetLine(budget),
            )
            "pref-traditional" -> {
                val reasons = buildList {
                    add("偏传统纪念的家庭往往看重固定的祭扫时间与去处，树葬、草坪葬等节地方式同样提供安静的追思空间，只是把独立墓碑换成了公共纪念载体。")
                    if (memorial == "mem-natural") {
                        add("您希望有树木花草长期相伴，树葬的纪念树会随季节生长，适合作为家庭思念的落点。")
                    }
                    add(budgetLine(budget))
                }
                return MatchRecommendation(
                    title = "建议了解：${BurialModeLabels.TREE} 或 ${BurialModeLabels.LAWN}",
                    summary = "在不放弃传统祭扫习惯的前提下，用更节地、更自然的方式安放思念。",
                    reasons = reasons.filter { it.isNotBlank() },
                    nextActions = listOf(
                        "前往安葬服务，对比树葬与草坪葬的园区与费用说明",
                        "进入政策预审，查看所在区县的补贴参考信息",
                    ),
                    targetKey = "tree",
                )
            }
        }

        // 无明确倾向：按预算与纪念需求引导。
        return when {
            budget == "budget-low" -> lawn(
                "预算有限时，节地生态安葬通常土地占用与后续养护成本更低，是值得优先了解的方向。$regionLine",
                "具体费用构成以服务机构公示为准，页面不替代报价。",
            )
            memorial == "mem-natural" -> tree(
                "您希望有树木花草长期相伴，树葬以纪念树承载思念，适合作为首选了解方向。$regionLine",
                budgetLine(budget),
            )
            memorial == "mem-nomarker" -> lawn(
                "您倾向不保留独立标识、回归自然，草坪葬与树葬的可降解安葬方式都符合这一想法。$regionLine",
                "如希望完全不保留任何独立纪念标识，还可了解公益海葬项目（政策专区）。",
            )
            else -> MatchRecommendation(
                title = "建议先了解：${BurialModeLabels.GENERAL}",
                summary = "树葬、花葬、草坪葬各有特点，可以结合园区实景与家庭习惯逐步比较。",
                reasons = listOf(
                    "节地生态安葬把安葬与纪念融入公共林地、花园与草坪，环境安静、持续有专人养护。",
                    budgetLine(budget),
                ).filter { it.isNotBlank() },
                nextActions = listOf(
                    "前往安葬服务，查看树葬、花葬、草坪葬的详情",
                    "进入政策预审，了解所在区县补贴参考与办理提示",
                ),
                targetKey = "general",
            )
        }
    }

    private fun tree(why: String, extra: String): MatchRecommendation = MatchRecommendation(
        title = "推荐了解：${BurialModeLabels.TREE}",
        summary = "以一棵纪念树安放思念，让告别回归山林。",
        reasons = listOf(why, extra).filter { it.isNotBlank() },
        nextActions = listOf(
            "前往安葬服务查看树葬详情",
            "进入政策预审查看所在区县补贴参考",
        ),
        targetKey = "tree",
    )

    private fun flower(why: String, extra: String): MatchRecommendation = MatchRecommendation(
        title = "推荐了解：${BurialModeLabels.FLOWER}",
        summary = "让思念落在花开四季的公共花园里。",
        reasons = listOf(why, extra).filter { it.isNotBlank() },
        nextActions = listOf(
            "前往安葬服务查看花葬详情",
            "进入政策预审查看所在区县补贴参考",
        ),
        targetKey = "flower",
    )

    private fun lawn(why: String, extra: String): MatchRecommendation = MatchRecommendation(
        title = "推荐了解：${BurialModeLabels.LAWN}",
        summary = "开阔草坪保持公共空间的宁静，也让祭扫自然延续。",
        reasons = listOf(why, extra).filter { it.isNotBlank() },
        nextActions = listOf(
            "前往安葬服务查看草坪葬详情",
            "进入政策预审查看所在区县补贴参考",
        ),
        targetKey = "lawn",
    )

    private fun regionText(region: String): String = when (region) {
        "region-center" -> "中心城区配套完善、交通便利，探访与参加集体纪念活动都较方便。"
        "region-new" -> "主城新区生态空间充足，不少园区依托浅丘与滨水环境建设。"
        "region-ne", "region-se" -> "区县园区通常依托本地山林与生态资源，建议就近实地了解。"
        else -> ""
    }

    private fun budgetLine(budget: String): String = when (budget) {
        "budget-low" -> "预算有限时可优先关注节地生态安葬项目，具体费用以服务机构公示为准。"
        "budget-mid" -> "节地生态安葬的费用构成通常较清晰，签约前请逐项确认服务与养护内容。"
        "budget-high" -> "无论预算如何，都建议先确认园区资质、服务内容与后续管理，再做决定。"
        else -> ""
    }
}
