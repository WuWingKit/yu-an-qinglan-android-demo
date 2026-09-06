/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.data

import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.data.local.AppJson
import com.yuanqinglan.app.feature.memorial.model.HumanMemorial
import com.yuanqinglan.app.feature.memorial.model.HumanMemorialDraft
import com.yuanqinglan.app.feature.memorial.model.JisiVisitRecord
import com.yuanqinglan.app.feature.memorial.model.MediaRef
import com.yuanqinglan.app.feature.memorial.model.MemorialDiaryEntry
import com.yuanqinglan.app.feature.memorial.model.MemorialFormRules
import com.yuanqinglan.app.feature.memorial.model.MemorialLetter
import com.yuanqinglan.app.feature.memorial.model.MemorialLike
import com.yuanqinglan.app.feature.memorial.model.MemorialMessage
import com.yuanqinglan.app.feature.memorial.model.MemorialStory
import com.yuanqinglan.app.feature.memorial.model.MemorialTrack
import com.yuanqinglan.app.feature.memorial.model.PetMemorial
import com.yuanqinglan.app.feature.memorial.model.PetMemorialDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString

/**
 * 追忆轨内存储基类（人类/宠物各自实例化，聚合类型互斥）。
 *
 * - 首次访问时经 [seedProvider] 载入内置纪念内容；若存在私有快照则以快照恢复；
 * - 每次变更（新建/编辑/删除/内容写入）同步落快照（尽力而为，重启后可选保留）；
 * - 全部读改写操作在同一把互斥锁内执行，避免并发点击造成状态丢失；
 * - 不可变 [DemoState] 状态流：加载/成功/空/失败均可复现。
 */
internal class ContentParts(
    val gallery: List<MediaRef>,
    val messages: List<MemorialMessage>,
    val stories: List<MemorialStory>,
    val letters: List<MemorialLetter>,
    val diary: List<MemorialDiaryEntry>,
    val jisiRecords: List<JisiVisitRecord>,
)

abstract class MemorialTrackStore<T : MemorialLike>(
    private val seedProvider: suspend () -> List<T>,
    private val snapshotIo: MemorialSnapshotIo?,
    private val snapshotName: String,
    private val listSerializer: KSerializer<List<T>>,
    private val normalizeLoaded: (T) -> T = { it },
) {

    private val mutex = Mutex()
    private var initialized = false

    private val _state = MutableStateFlow<DemoState<Map<String, T>>>(DemoState.Loading)
    val state: StateFlow<DemoState<Map<String, T>>> = _state.asStateFlow()

    /** 替换内容的不可变钩子（由具体轨实现 copy；本类型仅模块内可见）。 */
    internal abstract fun withParts(base: T, parts: ContentParts): T

    /** 替换名称/关系/简介的不可变钩子。 */
    protected abstract fun withMeta(base: T, name: String, relation: String, intro: String): T

    /** 纪念空间列表（按创建时间倒序）。收集时先触发初始化（种子/快照载入）。 */
    fun spaces(): Flow<DemoState<List<T>>> = flow {
        ensureInitialized()
        emitAll(
            state.map { current ->
                when (current) {
                    DemoState.Loading -> DemoState.Loading
                    is DemoState.Error -> current
                    is DemoState.Success -> DemoState.Success(
                        current.value.values.sortedByDescending { it.createdAtMillis },
                    )
                    DemoState.Empty -> DemoState.Success(emptyList())
                }
            }.distinctUntilChanged(),
        )
    }

    /** 按 ID 观察单个纪念空间。收集时先触发初始化。 */
    fun observe(memorialId: String): Flow<DemoState<T>> = flow {
        ensureInitialized()
        emitAll(
            state.map { current ->
                when (current) {
                    DemoState.Loading -> DemoState.Loading
                    is DemoState.Error -> current
                    is DemoState.Success -> current.value[memorialId]?.let { DemoState.Success(it) }
                        ?: DemoState.Empty
                    DemoState.Empty -> DemoState.Empty
                }
            }.distinctUntilChanged(),
        )
    }

    /** 取单个纪念空间（未找到返回 null）。 */
    suspend fun space(memorialId: String): T? = locked {
        successMap()[memorialId]
    }

    /** 本轨全部已保存信件（供 letter-view 按全局 letterId 检索；跨空间收集同轨信件）。 */
    suspend fun allLetters(): List<MemorialLetter> = locked {
        successMap().values.flatMap { it.letters }
    }

    /** 本轨全部祭扫记录聚合（供祭扫时光时间轴渲染）。 */
    suspend fun allJisiWithOwner(): List<Pair<T, JisiVisitRecord>> = locked {
        successMap().values.flatMap { space ->
            space.jisiRecords.map { space to it }
        }
    }

    /** 恢复内置内容（丢弃快照并重载种子）。 */
    suspend fun restoreBuiltIn() {
        locked {
            snapshotIo?.let { runCatching { it.delete(snapshotName) } }
            initialized = false
            loadLocked()
        }
    }

    // ---------------- 内容写入（全部在锁内执行，成功即持久化） ----------------

    suspend fun addGalleryMedia(memorialId: String, ref: MediaRef): Boolean =
        updateLocked(memorialId) { base ->
            withParts(
                base,
                ContentParts(base.gallery + ref, base.messages, base.stories, base.letters, base.diary, base.jisiRecords),
            )
        }

    /** 按相册项稳定 ID 删除（单张删除）。 */
    suspend fun removeGalleryMedia(memorialId: String, refId: String): Boolean =
        updateLocked(memorialId) { base ->
            if (base.gallery.none { it.id == refId }) {
                null
            } else {
                withParts(
                    base,
                    ContentParts(
                        base.gallery.filterNot { it.id == refId },
                        base.messages,
                        base.stories,
                        base.letters,
                        base.diary,
                        base.jisiRecords,
                    ),
                )
            }
        }

    /** 多选删除：一次删除多个相册项（不存在项忽略），返回实际删除数。 */
    suspend fun removeGalleryMediaBatch(memorialId: String, refIds: Set<String>): Int {
        val ids = refIds.filter { it.isNotBlank() }.toSet()
        if (ids.isEmpty()) return 0
        return locked {
            val map = successMap()
            val base = map[memorialId] ?: return@locked 0
            val removed = base.gallery.count { it.id in ids }
            if (removed == 0) {
                return@locked 0
            }
            val updated = withParts(
                base,
                ContentParts(
                    base.gallery.filterNot { it.id in ids },
                    base.messages,
                    base.stories,
                    base.letters,
                    base.diary,
                    base.jisiRecords,
                ),
            )
            commitLocked(map + (memorialId to updated))
            removed
        }
    }

    suspend fun addMessage(memorialId: String, message: MemorialMessage): Boolean =
        updateLocked(memorialId) { base ->
            withParts(
                base,
                ContentParts(base.gallery, base.messages + message, base.stories, base.letters, base.diary, base.jisiRecords),
            )
        }

    suspend fun addStory(memorialId: String, story: MemorialStory): Boolean =
        updateLocked(memorialId) { base ->
            withParts(
                base,
                ContentParts(base.gallery, base.messages, base.stories + story, base.letters, base.diary, base.jisiRecords),
            )
        }

    suspend fun addLetter(memorialId: String, letter: MemorialLetter): Boolean =
        updateLocked(memorialId) { base ->
            withParts(
                base,
                ContentParts(base.gallery, base.messages, base.stories, base.letters + letter, base.diary, base.jisiRecords),
            )
        }

    suspend fun removeLetter(memorialId: String, letterId: String): Boolean =
        updateLocked(memorialId) { base ->
            if (base.letters.none { it.id == letterId }) {
                null
            } else {
                withParts(
                    base,
                    ContentParts(
                        base.gallery,
                        base.messages,
                        base.stories,
                        base.letters.filterNot { it.id == letterId },
                        base.diary,
                        base.jisiRecords,
                    ),
                )
            }
        }

    /** 新增日记条目；图片/音频附件引用须先落私有目录再写入。 */
    suspend fun addDiaryEntry(memorialId: String, entry: MemorialDiaryEntry): Boolean =
        updateLocked(memorialId) { base ->
            withParts(
                base,
                ContentParts(base.gallery, base.messages, base.stories, base.letters, base.diary + entry, base.jisiRecords),
            )
        }

    /** 编辑回填：按条目 ID 替换整条（含附件变更）。 */
    suspend fun updateDiaryEntry(memorialId: String, entry: MemorialDiaryEntry): Boolean =
        updateLocked(memorialId) { base ->
            if (base.diary.none { it.id == entry.id }) {
                null
            } else {
                withParts(
                    base,
                    ContentParts(
                        base.gallery,
                        base.messages,
                        base.stories,
                        base.letters,
                        base.diary.map { if (it.id == entry.id) entry else it },
                        base.jisiRecords,
                    ),
                )
            }
        }

    suspend fun removeDiaryEntry(memorialId: String, entryId: String): Boolean =
        updateLocked(memorialId) { base ->
            if (base.diary.none { it.id == entryId }) {
                null
            } else {
                withParts(
                    base,
                    ContentParts(
                        base.gallery,
                        base.messages,
                        base.stories,
                        base.letters,
                        base.diary.filterNot { it.id == entryId },
                        base.jisiRecords,
                    ),
                )
            }
        }

    suspend fun addJisiRecord(memorialId: String, record: JisiVisitRecord): Boolean =
        updateLocked(memorialId) { base ->
            withParts(
                base,
                ContentParts(
                    base.gallery,
                    base.messages,
                    base.stories,
                    base.letters,
                    base.diary,
                    base.jisiRecords + record,
                ),
            )
        }

    // ---------------- 私有辅助 ----------------

    /** 互斥入口：确保初始化后在锁内执行读改写。 */
    protected suspend fun <R> locked(block: suspend () -> R): R = mutex.withLock {
        if (!initialized) loadLocked()
        block()
    }

    /** 锁外初始化入口（供 spaces/observe 冷流在收集时触发，避免与 locked 互斥重入）。 */
    private suspend fun ensureInitialized() {
        mutex.withLock {
            if (!initialized) loadLocked()
        }
    }

    protected fun successMap(): Map<String, T> =
        (_state.value as? DemoState.Success)?.value ?: emptyMap()

    protected suspend fun commitLocked(updated: Map<String, T>) {
        _state.value = DemoState.Success(updated)
        persistLocked(updated)
    }

    /** transform 返回 null 表示不满足条件（目标不存在/无可删项），不提交。 */
    private suspend fun updateLocked(
        memorialId: String,
        transform: (T) -> T?,
    ): Boolean = locked {
        val map = successMap()
        val base = map[memorialId] ?: return@locked false
        val updated = transform(base) ?: return@locked false
        commitLocked(map + (memorialId to updated))
        true
    }

    private suspend fun loadLocked() {
        initialized = true
        _state.value = try {
            val snapshotText = snapshotIo?.read(snapshotName)
            val items: List<T> = if (snapshotText != null) {
                try {
                    AppJson.decodeFromString(listSerializer, snapshotText)
                } catch (_: Exception) {
                    seedProvider()
                }
            } else {
                seedProvider()
            }
            DemoState.Success(items.map(normalizeLoaded).associateBy { it.id })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DemoState.Error("纪念内容加载失败，请稍后重试。")
        }
    }

    private suspend fun persistLocked(updated: Map<String, T>) {
        val io = snapshotIo ?: return
        runCatching {
            val text = AppJson.encodeToString(
                listSerializer,
                updated.values.sortedBy { it.createdAtMillis },
            )
            io.write(snapshotName, text)
        }
        // 快照写入失败不阻断内存状态（快照保留为可选能力，不静默丢用户操作）。
    }
}

/** 人类纪念空间存储（独立入口）。 */
class HumanMemorialStore(
    seedProvider: suspend () -> List<HumanMemorial>,
    snapshotIo: MemorialSnapshotIo? = null,
    snapshotName: String = SNAPSHOT_HUMAN,
) : MemorialTrackStore<HumanMemorial>(
    seedProvider = seedProvider,
    snapshotIo = snapshotIo,
    snapshotName = snapshotName,
    listSerializer = ListSerializer(HumanMemorial.serializer()),
    normalizeLoaded = { memorial ->
        if (memorial.id == MOTHER_MEMORIAL_ID && memorial.portrait == HumanMemorial.PORTRAIT_DEFAULT) {
            memorial.copy(portrait = HumanMemorial.PORTRAIT_MOTHER)
        } else {
            memorial
        }
    },
) {

    internal override fun withParts(base: HumanMemorial, parts: ContentParts): HumanMemorial = base.copy(
        gallery = parts.gallery,
        messages = parts.messages,
        stories = parts.stories,
        letters = parts.letters,
        diary = parts.diary,
        jisiRecords = parts.jisiRecords,
    )

    override fun withMeta(base: HumanMemorial, name: String, relation: String, intro: String): HumanMemorial =
        base.copy(name = name, relation = relation, intro = intro)

    /** 新建人类纪念空间；返回创建后的对象。 */
    suspend fun create(draft: HumanMemorialDraft): HumanMemorial = locked {
        val now = System.currentTimeMillis()
        val created = HumanMemorial(
            id = MemorialTrack.nextId(MemorialTrack.HUMAN),
            name = buildDraftName(draft.name),
            relation = draft.relation.trim(),
            intro = draft.intro.trim(),
            portrait = draft.portrait,
            createdAtMillis = now,
        )
        val map = successMap()
        commitLocked(map + (created.id to created))
        created
    }

    /** 编辑基本信息；返回更新后对象或 null（不存在）。 */
    suspend fun updateMeta(memorialId: String, name: String, relation: String, intro: String): HumanMemorial? =
        locked {
            val map = successMap()
            val base = map[memorialId] ?: return@locked null
            val updated = withMeta(base, buildDraftName(name), relation.trim(), intro.trim())
            commitLocked(map + (memorialId to updated))
            updated
        }

    /** 删除纪念空间（二次确认由 UI 层负责）；返回是否删除成功。 */
    suspend fun delete(memorialId: String): Boolean = locked {
        val map = successMap()
        if (map[memorialId] == null) return@locked false
        commitLocked(map - memorialId)
        true
    }

    private companion object {
        const val SNAPSHOT_HUMAN = "human_memorials.json"
        const val MOTHER_MEMORIAL_ID = "hm-002"
    }
}

/** 宠物纪念空间存储（独立入口）。 */
class PetMemorialStore(
    seedProvider: suspend () -> List<PetMemorial>,
    snapshotIo: MemorialSnapshotIo? = null,
    snapshotName: String = SNAPSHOT_PET,
) : MemorialTrackStore<PetMemorial>(
    seedProvider = seedProvider,
    snapshotIo = snapshotIo,
    snapshotName = snapshotName,
    listSerializer = ListSerializer(PetMemorial.serializer()),
) {

    internal override fun withParts(base: PetMemorial, parts: ContentParts): PetMemorial = base.copy(
        gallery = parts.gallery,
        messages = parts.messages,
        stories = parts.stories,
        letters = parts.letters,
        diary = parts.diary,
        jisiRecords = parts.jisiRecords,
    )

    override fun withMeta(base: PetMemorial, name: String, relation: String, intro: String): PetMemorial =
        base.copy(name = name, relation = relation, intro = intro)

    /** 新建宠物纪念空间；返回创建后的对象。 */
    suspend fun create(draft: PetMemorialDraft): PetMemorial = locked {
        val now = System.currentTimeMillis()
        val created = PetMemorial(
            id = MemorialTrack.nextId(MemorialTrack.PET),
            name = buildDraftName(draft.name),
            relation = draft.relation.trim(),
            intro = draft.intro.trim(),
            portrait = draft.portrait,
            createdAtMillis = now,
        )
        val map = successMap()
        commitLocked(map + (created.id to created))
        created
    }

    /** 编辑基本信息；返回更新后对象或 null（不存在）。 */
    suspend fun updateMeta(memorialId: String, name: String, relation: String, intro: String): PetMemorial? =
        locked {
            val map = successMap()
            val base = map[memorialId] ?: return@locked null
            val updated = withMeta(base, buildDraftName(name), relation.trim(), intro.trim())
            commitLocked(map + (memorialId to updated))
            updated
        }

    /** 删除纪念空间（二次确认由 UI 层负责）。 */
    suspend fun delete(memorialId: String): Boolean = locked {
        val map = successMap()
        if (map[memorialId] == null) return@locked false
        commitLocked(map - memorialId)
        true
    }

    private companion object {
        const val SNAPSHOT_PET = "pet_memorials.json"
    }
}

/** 名称校验后 trim（供创建/编辑使用）。 */
private fun MemorialTrackStore<*>.buildDraftName(name: String): String {
    val trimmed = name.trim()
    require(MemorialFormRules.nameError(trimmed) == null) { "纪念对象名称不合法: $name" }
    return trimmed
}
