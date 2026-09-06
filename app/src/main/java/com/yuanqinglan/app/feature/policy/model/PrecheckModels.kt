/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.policy.model

/** 政策预审表单字段（用于错误定位）。 */
enum class PrecheckField {
    COUNTY,
    APPLICANT,
    RELATION,
    MODE,
    PHONE,
}

/** 单个字段校验错误。 */
data class PrecheckFieldError(
    val field: PrecheckField,
    val message: String,
)

/** 预审表单的可选身份/关系/葬式配置项（本地规则）。 */
data class SelectOption(
    val value: String,
    val label: String,
    val hint: String? = null,
)

/** 表单选项常量（本地规则）。 */
object PrecheckOptions {
    /** 申请身份类型。 */
    val applicantTypes: List<SelectOption> = listOf(
        SelectOption("spouse-children", "逝者的配偶、子女等直系亲属"),
        SelectOption("other-relative", "逝者的其他亲属"),
        SelectOption("authorized", "受亲属委托办理的人员"),
    )

    /** 与逝者的关系。 */
    val relationTypes: List<SelectOption> = listOf(
        SelectOption("spouse", "配偶"),
        SelectOption("child", "子女"),
        SelectOption("parent", "父母"),
        SelectOption("sibling", "兄弟姐妹"),
        SelectOption("grandchild", "孙辈"),
        SelectOption("other", "其他亲属或委托人"),
    )

    /** 计划安葬方式。 */
    val burialModes: List<SelectOption> = listOf(
        SelectOption("tree", "树葬"),
        SelectOption("flower", "花葬"),
        SelectOption("lawn", "草坪葬"),
        SelectOption("sea", "公益海葬"),
        SelectOption("traditional", "传统安葬"),
    )
}

/** 预审表单内容（全部本地状态，不收集真实个人身份信息）。 */
data class PrecheckForm(
    val countyId: String = "",
    val applicantType: String = "",
    val relationType: String = "",
    val burialMode: String = "",
    /** 选填联系电话（仅本机暂存，用于格式校验演示，不提交）。 */
    val contactPhone: String = "",
    val remark: String = "",
)

/** 补贴项目拆分行。 */
data class SubsidyLine(
    val title: String,
    /** 参考金额（元）；0 表示公益项目不适用金额。 */
    val amountYuan: Int,
    val description: String,
)

/** 预审测算结果。 */
data class SubsidyEstimate(
    /** 是否匹配到补贴项目；传统安葬等场景为 false 并给出提示。 */
    val hasMatch: Boolean,
    val lines: List<SubsidyLine>,
    val totalYuan: Int,
    val notes: List<String>,
)
