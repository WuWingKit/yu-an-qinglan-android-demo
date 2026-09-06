/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.model

import kotlinx.serialization.Serializable

/**
 * 人类纪念空间聚合（独立数据类）。
 *
 * 与 [PetMemorial] 完全独立：人类聚合来自 `assets/demo/memorial/memorials_human.json`
 * 并经独立的 [HumanMemorial 仓库入口] 管理，标识以 `hm-` 开头。任何轨道切换
 * 都不会把宠物数据写进本类型或反向混用。
 *
 * [portrait] 为 drawable 资源名；内置外公与母亲使用各自独立肖像，用户新增的
 * 相册/附件以 file:// 私有目录 Uri 形式保存于 [MediaRef]。
 */
@Serializable
data class HumanMemorial(
    override val id: String,
    override val name: String,
    override val portrait: String = PORTRAIT_DEFAULT,
    override val relation: String = "",
    override val intro: String = "",
    override val createdAtMillis: Long = 0L,
    override val gallery: List<MediaRef> = emptyList(),
    override val messages: List<MemorialMessage> = emptyList(),
    override val stories: List<MemorialStory> = emptyList(),
    override val letters: List<MemorialLetter> = emptyList(),
    override val diary: List<MemorialDiaryEntry> = emptyList(),
    override val jisiRecords: List<JisiVisitRecord> = emptyList(),
    override val birthDate: MemorialDate? = null,
    override val deathDate: MemorialDate? = null,
) : MemorialLike {
    companion object {
        /** 人类纪念肖像资源名（示意肖像，不作真实人物宣传）。 */
        const val PORTRAIT_DEFAULT = "memorial_human_portrait"
        const val PORTRAIT_MOTHER = "memorial_mother_portrait"
    }
}

/** 人类纪念空间创建草稿（强类型，仅可创建 HumanMemorial）。 */
data class HumanMemorialDraft(
    val name: String,
    val relation: String,
    val intro: String,
    val portrait: String = HumanMemorial.PORTRAIT_DEFAULT,
    val birthDate: MemorialDate? = null,
    val deathDate: MemorialDate? = null,
) {
    companion object {
        const val DEFAULT_PORTRAIT = HumanMemorial.PORTRAIT_DEFAULT
    }
}
