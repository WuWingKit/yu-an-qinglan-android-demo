/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.data

import android.content.Context
import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.data.local.DemoAssetLoader
import com.yuanqinglan.app.feature.memorial.model.HumanMemorial
import com.yuanqinglan.app.feature.memorial.model.HumanMemorialDraft
import com.yuanqinglan.app.feature.memorial.model.JisiVisitRecord
import com.yuanqinglan.app.feature.memorial.model.MediaRef
import com.yuanqinglan.app.feature.memorial.model.MemorialDate
import com.yuanqinglan.app.feature.memorial.model.MemorialDiaryEntry
import com.yuanqinglan.app.feature.memorial.model.MemorialLetter
import com.yuanqinglan.app.feature.memorial.model.MemorialLike
import com.yuanqinglan.app.feature.memorial.model.MemorialMessage
import com.yuanqinglan.app.feature.memorial.model.MemorialStory
import com.yuanqinglan.app.feature.memorial.model.MemorialTrack
import com.yuanqinglan.app.feature.memorial.model.PetMemorial
import com.yuanqinglan.app.feature.memorial.model.PetMemorialDraft
import kotlinx.coroutines.flow.Flow

/**
 * 追忆模块进程级入口：聚合人类/宠物两套独立存储 + 按 ID 路由的内容操作。
 *
 * 路由只依据纪念空间 ID 前缀（hm-/pm-）选择对应轨存储；内容写入一律回到
 * 所属轨的独立存储，绝不写入另一轨。读取侧对外提供只读公共形态（MemorialLike），
 * 用于故事/日记/信件等以 ID 为入口的页面渲染；这些页面不持有跨轨集合。
 */
class MemorialRepository(context: Context) {

    private val loader = DemoAssetLoader(context.applicationContext)
    private val snapshotIo: MemorialSnapshotIo = PrivateFileSnapshotIo(context.applicationContext)

    /** 人类纪念空间存储（独立入口）。 */
    val human: HumanMemorialStore = HumanMemorialStore(
        seedProvider = { loader.load(HUMAN_ASSET, HumanMemorialsFile.serializer()).memorials },
        snapshotIo = snapshotIo,
    )

    /** 宠物纪念空间存储（独立入口）。 */
    val pet: PetMemorialStore = PetMemorialStore(
        seedProvider = { loader.load(PET_ASSET, PetMemorialsFile.serializer()).memorials },
        snapshotIo = snapshotIo,
    )

    // ---------------- 轨道类型化读取 ----------------

    fun humanSpaces(): Flow<DemoState<List<HumanMemorial>>> = human.spaces()

    fun petSpaces(): Flow<DemoState<List<PetMemorial>>> = pet.spaces()

    fun observeHumanSpace(memorialId: String): Flow<DemoState<HumanMemorial>> = human.observe(memorialId)

    fun observePetSpace(memorialId: String): Flow<DemoState<PetMemorial>> = pet.observe(memorialId)

    /** 按 ID 观察任意轨空间（渲染层只读公共形态，不跨轨集合）。 */
    fun observeSpace(memorialId: String): Flow<DemoState<MemorialLike>> {
        val track = MemorialTrack.ofId(memorialId)
        return if (track == MemorialTrack.HUMAN) human.observe(memorialId) else pet.observe(memorialId)
    }

    suspend fun space(memorialId: String): MemorialLike? {
        val track = MemorialTrack.ofId(memorialId)
        return if (track == MemorialTrack.HUMAN) human.space(memorialId) else pet.space(memorialId)
    }

    /** 按全局 letterId 检索信件（先人类轨后宠物轨；ID 前缀决定所属，绝不跨轨命中）。 */
    suspend fun letterById(letterId: String): MemorialLetter? =
        human.allLetters().firstOrNull { it.id == letterId }
            ?: pet.allLetters().firstOrNull { it.id == letterId }

    /** 指定轨的祭扫记录时间线（含所属空间名；供祭扫时光页展示）。 */
    suspend fun jisiTimeline(track: MemorialTrack): List<JisiTimelineItem> = when (track) {
        MemorialTrack.HUMAN -> human.allJisiWithOwner().map { (space, record) ->
            JisiTimelineItem(space.id, space.name, record)
        }
        MemorialTrack.PET -> pet.allJisiWithOwner().map { (space, record) ->
            JisiTimelineItem(space.id, space.name, record)
        }
    }

    // ---------------- 创建/编辑/删除（轨道类型化） ----------------

    suspend fun createHuman(draft: HumanMemorialDraft): HumanMemorial = human.create(draft)

    suspend fun createPet(draft: PetMemorialDraft): PetMemorial = pet.create(draft)

    suspend fun updateHumanMeta(
        memorialId: String,
        name: String,
        relation: String,
        intro: String,
        birthDate: MemorialDate? = null,
        deathDate: MemorialDate? = null,
    ): HumanMemorial? = human.updateMeta(memorialId, name, relation, intro, birthDate, deathDate)

    suspend fun updatePetMeta(
        memorialId: String,
        name: String,
        relation: String,
        intro: String,
        birthDate: MemorialDate? = null,
        deathDate: MemorialDate? = null,
    ): PetMemorial? = pet.updateMeta(memorialId, name, relation, intro, birthDate, deathDate)

    suspend fun deleteHuman(memorialId: String): Boolean = human.delete(memorialId)

    suspend fun deletePet(memorialId: String): Boolean = pet.delete(memorialId)

    // ---------------- 内容写入（按 ID 路由到所属轨存储） ----------------

    suspend fun addGalleryMedia(memorialId: String, ref: MediaRef): Boolean = route(memorialId) {
        it.addGalleryMedia(memorialId, ref)
    }

    suspend fun removeGalleryMedia(memorialId: String, refId: String): Boolean = route(memorialId) {
        it.removeGalleryMedia(memorialId, refId)
    }

    suspend fun removeGalleryMediaBatch(memorialId: String, refIds: Set<String>): Int = route(memorialId) {
        it.removeGalleryMediaBatch(memorialId, refIds)
    }

    suspend fun addMessage(memorialId: String, message: MemorialMessage): Boolean = route(memorialId) {
        it.addMessage(memorialId, message)
    }

    suspend fun addStory(memorialId: String, story: MemorialStory): Boolean = route(memorialId) {
        it.addStory(memorialId, story)
    }

    suspend fun addLetter(memorialId: String, letter: MemorialLetter): Boolean = route(memorialId) {
        it.addLetter(memorialId, letter)
    }

    suspend fun removeLetter(memorialId: String, letterId: String): Boolean = route(memorialId) {
        it.removeLetter(memorialId, letterId)
    }

    suspend fun addDiaryEntry(memorialId: String, entry: MemorialDiaryEntry): Boolean = route(memorialId) {
        it.addDiaryEntry(memorialId, entry)
    }

    suspend fun updateDiaryEntry(memorialId: String, entry: MemorialDiaryEntry): Boolean = route(memorialId) {
        it.updateDiaryEntry(memorialId, entry)
    }

    suspend fun removeDiaryEntry(memorialId: String, entryId: String): Boolean = route(memorialId) {
        it.removeDiaryEntry(memorialId, entryId)
    }

    suspend fun addJisiRecord(memorialId: String, record: JisiVisitRecord): Boolean = route(memorialId) {
        it.addJisiRecord(memorialId, record)
    }

    private suspend fun <R> route(
        memorialId: String,
        block: suspend (MemorialTrackStore<out MemorialLike>) -> R,
    ): R {
        val track = MemorialTrack.ofId(memorialId)
        return if (track == MemorialTrack.HUMAN) {
            block(human)
        } else {
            block(pet)
        }
    }

    companion object {
        private const val HUMAN_ASSET = "memorial/memorials_human.json"
        private const val PET_ASSET = "memorial/memorials_pet.json"
    }
}

/** 祭扫时光时间轴条目（所属空间 + 单条记录）。 */
data class JisiTimelineItem(
    val memorialId: String,
    val memorialName: String,
    val record: JisiVisitRecord,
)

/** 追忆模块服务定位器（懒加载单例）。 */
object MemorialServiceLocator {

    @Volatile
    private var repository: MemorialRepository? = null

    @Volatile
    private var daijiCollective: DaijiCollectiveStore? = null

    fun repository(context: Context): MemorialRepository {
        val appContext = context.applicationContext
        return repository ?: synchronized(this) {
            repository ?: MemorialRepository(appContext).also { repository = it }
        }
    }

    /** 代祭与共祭的独立本地存储（与纪念空间仓库分开构造、分开状态）。 */
    fun daijiCollective(context: Context): DaijiCollectiveStore {
        val appContext = context.applicationContext
        return daijiCollective ?: synchronized(this) {
            daijiCollective ?: DaijiCollectiveStore(DemoAssetLoader(appContext)).also {
                daijiCollective = it
            }
        }
    }
}
