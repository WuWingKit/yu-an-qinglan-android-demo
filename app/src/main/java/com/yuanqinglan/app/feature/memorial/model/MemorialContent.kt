/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.model

import kotlinx.serialization.Serializable

/**
 * 追忆模块内容层模型（叶子值类型与聚合公共接口）。
 *
 * 隔离设计：叶子类型（影像/寄语/故事/信件/日记/祭扫记录）为无轨道语义的内容值，
 * 只被同轨聚合 [HumanMemorial] / [PetMemorial] 以独立字段持有；轨道隔离的硬边界
 * 由两套完全独立的聚合类型、JSON 文件与仓库入口保证（详见 [MemorialLike] 与
 * `feature/memorial/data`）。任何跨轨操作不会共享同一份集合。
 */

/** 影像/附件来源：内置资源（drawable-nodpi）或应用私有目录 file:// Uri。 */
enum class MediaKind {
    DRAWABLE,
    IMAGE_FILE,
    AUDIO_FILE,
    VIDEO_FILE,
}

/**
 * 影像/附件引用。DRAWABLE 时 [value] 为 drawable 资源名（如 memorial_gallery_family_tea），
 * 其余为应用私有目录 file:// Uri。用户新增的影像先落私有目录（公共 FileStorage），
 * 不引用任何外部内容。
 */
@Serializable
data class MediaRef(
    val id: String,
    val kind: MediaKind,
    val value: String,
    val name: String = "",
    val sizeBytes: Long = 0L,
) {
    val isDrawable: Boolean get() = kind == MediaKind.DRAWABLE
}

/**
 * 纪念对象日期（出生/离世），支持三种精度：
 * - 仅年份：`year`，如 `MemorialDate(1996)`，展示为「1996年」；
 * - 年月：`year + month`，如 `MemorialDate(1996, 2)`，展示为「1996年2月」；
 * - 完整日期：`year + month + day`，如 `MemorialDate(1996, 2, 3)`，展示为「1996年2月3日」。
 *
 * 序列化为结构化 JSON（`{"year":1996,"month":2,"day":3}`）；缺省维度不写字段。
 * 聚合（HumanMemorial/PetMemorial）以 `MemorialDate? = null` 持有本值，「未知」即 null。
 * 本类型为纯值对象，合法性校验/自然格式化等规则集中在 `MemorialDateRules`。
 */
@Serializable
data class MemorialDate(
    val year: Int,
    val month: Int? = null,
    val day: Int? = null,
)

/** 纪念空间留言（寄语）：本地新增，默认以“我”的身份发布。 */
@Serializable
data class MemorialMessage(
    val id: String,
    val author: String,
    val text: String,
    val createdAtMillis: Long,
)

/** 生命故事节点：按 [dateMillis] 时间轴排序展示。 */
@Serializable
data class MemorialStory(
    val id: String,
    val title: String,
    val dateMillis: Long,
    val dateText: String,
    val body: String,
    val image: MediaRef? = null,
)

/** 思念日记条目：支持图片与音频附件（新增、编辑回填、删除）。 */
@Serializable
data class MemorialDiaryEntry(
    val id: String,
    val title: String,
    val body: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val images: List<MediaRef> = emptyList(),
    val audio: MediaRef? = null,
) {
    fun hasAttachment(): Boolean = images.isNotEmpty() || audio != null
}

/** 信纸样式（本地信件，仅影响展示）。 */
enum class PaperStyle(val label: String) {
    PLAIN("素白信纸"),
    GREEN_LINES("青绿格纹"),
    WARM("暖米信笺"),
    ;

    companion object {
        fun fromTokenOrNull(token: String?): PaperStyle? = entries.firstOrNull { it.name == token }
    }
}

/** 已保存的本地信件（未寄出，仅本地存储）。 */
@Serializable
data class MemorialLetter(
    val id: String,
    val memorialId: String,
    val title: String,
    val body: String,
    val paper: PaperStyle = PaperStyle.PLAIN,
    val createdAtMillis: Long,
)

/** 祭扫记录：日期、地点/方式与寄语；挂在纪念空间时间线上。 */
@Serializable
data class JisiVisitRecord(
    val id: String,
    val dateMillis: Long,
    val dateText: String,
    val place: String,
    val message: String = "",
)

/** 纪念空间聚合的只读公共形态（人类/宠物各自独立数据类实现本接口）。 */
interface MemorialLike {
    val id: String
    val name: String
    val portrait: String
    val relation: String
    val intro: String
    val createdAtMillis: Long
    val birthDate: MemorialDate?
    val deathDate: MemorialDate?
    val gallery: List<MediaRef>
    val messages: List<MemorialMessage>
    val stories: List<MemorialStory>
    val letters: List<MemorialLetter>
    val diary: List<MemorialDiaryEntry>
    val jisiRecords: List<JisiVisitRecord>

    /** 故事按时间升序（导出与时间轴使用）。 */
    fun sortedStories(): List<MemorialStory> = stories.sortedBy { it.dateMillis }

    /** 最新故事时间（无故事时为 -1）。 */
    fun latestStoryMillis(): Long = stories.maxOfOrNull { it.dateMillis } ?: -1L

    /** 日记按创建时间倒序（列表展示用）。 */
    fun sortedDiaryDesc(): List<MemorialDiaryEntry> = diary.sortedByDescending { it.createdAtMillis }

    /** 信件按创建时间倒序。 */
    fun sortedLettersDesc(): List<MemorialLetter> = letters.sortedByDescending { it.createdAtMillis }

    /** 祭扫记录按时间升序。 */
    fun sortedJisi(): List<JisiVisitRecord> = jisiRecords.sortedBy { it.dateMillis }

    /** 寄语按时间倒序。 */
    fun sortedMessagesDesc(): List<MemorialMessage> = messages.sortedByDescending { it.createdAtMillis }

    fun storyById(storyId: String): MemorialStory? = stories.firstOrNull { it.id == storyId }

    fun letterById(letterId: String): MemorialLetter? = letters.firstOrNull { it.id == letterId }

    fun diaryById(entryId: String): MemorialDiaryEntry? = diary.firstOrNull { it.id == entryId }

    fun galleryItemById(refId: String): MediaRef? = gallery.firstOrNull { it.id == refId }
}
