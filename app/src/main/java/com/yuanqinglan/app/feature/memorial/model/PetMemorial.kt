/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.model

import kotlinx.serialization.Serializable

/**
 * 宠物纪念空间聚合（独立数据类）。
 *
 * 与 [HumanMemorial] 完全独立：宠物聚合来自 `assets/demo/memorial/memorials_pet.json`
 * 并经独立的 [PetMemorial 仓库入口] 管理，标识以 `pm-` 开头。宠物纪念与人类纪念
 * 数据永不共享同一列表或同一集合。
 *
 * [portrait] 为 drawable 资源名（宠物肖像 memorial_pet_portrait）。宠物纪念内容
 * （相册/寄语/故事/信件/日记/祭扫记录）与人类纪念完全同构但不互通。
 */
@Serializable
data class PetMemorial(
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
) : MemorialLike {
    companion object {
        /** 宠物纪念肖像资源名（示意肖像，不作真实宠物档案宣传）。 */
        const val PORTRAIT_DEFAULT = "memorial_pet_portrait"
    }
}

/** 宠物纪念空间创建草稿（强类型，仅可创建 PetMemorial）。 */
data class PetMemorialDraft(
    val name: String,
    val relation: String,
    val intro: String,
    val portrait: String = PetMemorial.PORTRAIT_DEFAULT,
) {
    companion object {
        const val DEFAULT_PORTRAIT = PetMemorial.PORTRAIT_DEFAULT
    }
}
