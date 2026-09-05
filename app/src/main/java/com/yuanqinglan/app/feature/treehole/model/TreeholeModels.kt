/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.treehole.model

import kotlinx.serialization.Serializable

/**
 * 树洞模块内容模型。
 *
 * 人间（[HumanLetter]）与生灵（[PetLetter]）是两套独立内容池：独立聚合类型、
 * 独立 JSON 文件（human-letters.json / pet-letters.json）、独立仓库入口与本地存储。
 * 信纸样式、分类与附件不可跨池混用。树洞不提供点赞计数、热度、粉丝、私信或
 * 楼中楼评论，卡片不展示地域、不建立社交关系。
 */

/** 树洞池（人间/生灵）。 */
enum class TreeholePoolType(val poolLabel: String, val idPrefix: String) {
    HUMAN_POOL("人间树洞", "tlh-"),
    PET_POOL("生灵树洞", "tlp-"),
    ;
}

/** 信件审核状态：本地状态机。发布后进入 [REVIEWING]，不会立刻进入公共拾信池。 */
enum class TreeholeLetterState {
    REVIEWING,
    PUBLISHED,
}

/** 信纸样式（本地展示；两种内容池各自允许其池内样式选择）。 */
enum class TreeholePaperStyle(val label: String) {
    PLAIN("素白"),
    GREEN("青绿格纹"),
    WARM("暖米信笺"),
    ;

    companion object {
        fun fromTokenOrNull(token: String?): TreeholePaperStyle? =
            entries.firstOrNull { it.name == token }
    }
}

/** 树洞信件附件（file:// 私有目录），类型限定图片/音频。 */
@Serializable
data class TreeholeAttachment(
    val id: String,
    val kind: TreeholeAttachmentKind,
    val uri: String,
    val name: String = "",
    val sizeBytes: Long = 0L,
)

enum class TreeholeAttachmentKind {
    IMAGE,
    AUDIO,
}

/**
 * 附件大小上限（本地校验）：图片 ≤ 10MB、音频 ≤ 5MB，超限给出明确提示。
 */
object TreeholeAttachmentLimits {
    const val MAX_IMAGE_BYTES: Long = 10L * 1024L * 1024L
    const val MAX_AUDIO_BYTES: Long = 5L * 1024L * 1024L

    fun imageErrorIfAny(sizeBytes: Long): String? =
        if (sizeBytes > MAX_IMAGE_BYTES) {
            "图片不能超过 10MB，当前文件已超限"
        } else {
            null
        }

    fun audioErrorIfAny(sizeBytes: Long): String? =
        if (sizeBytes > MAX_AUDIO_BYTES) {
            "音频不能超过 5MB，当前文件已超限"
        } else {
            null
        }
}

/** 人间树洞信件（独立内容池；与生灵信件永不共享列表）。 */
@Serializable
data class HumanLetter(
    override val id: String,
    override val title: String,
    override val body: String,
    override val category: String,
    override val paper: TreeholePaperStyle = TreeholePaperStyle.PLAIN,
    override val image: TreeholeAttachment? = null,
    override val audio: TreeholeAttachment? = null,
    override val createdAtMillis: Long,
    override val state: TreeholeLetterState = TreeholeLetterState.PUBLISHED,
) : TreeholeLetterLike

/** 生灵树洞信件（独立内容池；与人间信件永不共享列表）。 */
@Serializable
data class PetLetter(
    override val id: String,
    override val title: String,
    override val body: String,
    override val category: String,
    override val paper: TreeholePaperStyle = TreeholePaperStyle.PLAIN,
    override val image: TreeholeAttachment? = null,
    override val audio: TreeholeAttachment? = null,
    override val createdAtMillis: Long,
    override val state: TreeholeLetterState = TreeholeLetterState.PUBLISHED,
) : TreeholeLetterLike

/** 树洞信件的只读公共形态（两种内容池各自实现；池实例强类型区分）。 */
interface TreeholeLetterLike {
    val id: String
    val title: String
    val body: String
    val category: String
    val paper: TreeholePaperStyle
    val image: TreeholeAttachment?
    val audio: TreeholeAttachment?
    val createdAtMillis: Long
    val state: TreeholeLetterState
}

/** 人间池分类（可写信选择；不跨池）。 */
val HUMAN_POOL_CATEGORIES: List<String> = listOf("思念", "倾诉", "祝福", "遗憾", "感恩")

/** 生灵池分类（不跨池）。 */
val PET_POOL_CATEGORIES: List<String> = listOf("想念", "告别", "谢谢你", "日常")

fun TreeholePoolType.categories(): List<String> = when (this) {
    TreeholePoolType.HUMAN_POOL -> HUMAN_POOL_CATEGORIES
    TreeholePoolType.PET_POOL -> PET_POOL_CATEGORIES
}

/** 轻回应：仅允许这三种关怀表达，不显示任何计数。 */
enum class KindResponse(val label: String, val description: String) {
    LIGHT("点灯", "为你点亮一盏灯"),
    LEAF("叶片", "寄去一片新叶"),
    FLOWER("花朵", "送上一朵小花"),
}
