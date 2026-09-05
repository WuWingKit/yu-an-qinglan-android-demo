/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.treehole.data

import android.content.Context
import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.data.local.DemoAssetLoader
import com.yuanqinglan.app.feature.treehole.model.HUMAN_POOL_CATEGORIES
import com.yuanqinglan.app.feature.treehole.model.HumanLetter
import com.yuanqinglan.app.feature.treehole.model.PET_POOL_CATEGORIES
import com.yuanqinglan.app.feature.treehole.model.PetLetter
import com.yuanqinglan.app.feature.treehole.model.TreeholeAttachment
import com.yuanqinglan.app.feature.treehole.model.TreeholeLetterLike
import com.yuanqinglan.app.feature.treehole.model.TreeholeLetterState
import com.yuanqinglan.app.feature.treehole.model.TreeholePaperStyle
import com.yuanqinglan.app.feature.treehole.model.TreeholePoolType
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update

/**
 * 单个树洞内容池（人间 [HumanLetter] / 生灵 [PetLetter] 各自实例化，类型互斥）。
 *
 * - 公共池内容（拾信候选）来自各自独立 JSON，经 [publicLetters] 流出（含加载/失败态）；
 * - 本人信件（寄信后进入待审核）存于 [mineLetters]，与公共池内容分开保存；
 * - 信纸样式、分类与附件在本池内使用，绝不跨池混用。
 */
class TreeholePool<T : TreeholeLetterLike>(
    private val seedProvider: suspend () -> List<T>,
    private val idPrefix: String,
    private val categories: List<String>,
    private val builder: (
        id: String,
        title: String,
        body: String,
        category: String,
        paper: TreeholePaperStyle,
        image: TreeholeAttachment?,
        audio: TreeholeAttachment?,
        createdAtMillis: Long,
    ) -> T,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    private val seq = AtomicLong(0L)

    private val _mine = MutableStateFlow<List<T>>(emptyList())
    val mineLetters: StateFlow<List<T>> = _mine.asStateFlow()

    /** 公共池内容流（每次订阅触发加载；失败可通过重新订阅重试）。 */
    fun publicLetters(): Flow<DemoState<List<T>>> = flow {
        emit(DemoState.Loading)
        emit(loadSeeds())
    }.flowOn(ioDispatcher)

    /** 本池可选分类（寄信表单使用）。 */
    fun availableCategories(): List<String> = categories

    /** 寄信：构造待审核信件并写入本人列表（公共拾信池不变）。 */
    suspend fun submit(
        title: String,
        body: String,
        category: String,
        paper: TreeholePaperStyle,
        image: TreeholeAttachment?,
        audio: TreeholeAttachment?,
    ): T {
        val now = System.currentTimeMillis()
        val id = "$idPrefix${now}-${seq.incrementAndGet()}"
        val letter = builder(
            id,
            title.trim(),
            body.trim(),
            category,
            paper,
            image,
            audio,
            now,
        )
        _mine.update { listOf(letter) + it.filterNot { existing -> existing.id == letter.id } }
        return letter
    }

    /** 本人信件删除（二次确认由 UI 负责）；不存在返回 false。 */
    suspend fun deleteMine(letterId: String): Boolean {
        var existed = false
        _mine.update { list ->
            if (list.any { it.id == letterId }) {
                existed = true
                list.filterNot { it.id == letterId }
            } else {
                list
            }
        }
        return existed
    }

    private suspend fun loadSeeds(): DemoState<List<T>> = try {
        val seeds = seedProvider()
        if (seeds.isEmpty()) DemoState.Empty else DemoState.Success(seeds)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DemoState.Error("树洞内容加载失败，请稍后重试。")
    }
}

/** 池内信件构造签名（人/宠分别构造各自强类型；参数序列固定）。 */
internal typealias TreeholeLetterBuilder<T> = (
    id: String,
    title: String,
    body: String,
    category: String,
    paper: TreeholePaperStyle,
    image: TreeholeAttachment?,
    audio: TreeholeAttachment?,
    createdAtMillis: Long,
) -> T

/** 树洞仓库：人间与生灵两套独立内容池与本地存储。 */
class TreeholeRepository(context: Context) {

    private val loader = DemoAssetLoader(context.applicationContext)

    /** 人间内容池（独立存储，只含 [HumanLetter]）。 */
    val humanPool: TreeholePool<HumanLetter> = TreeholePool(
        seedProvider = { loader.load(HUMAN_ASSET, HumanLettersFile.serializer()).letters },
        idPrefix = "tlh-",
        categories = HUMAN_POOL_CATEGORIES,
        builder = { id, title, body, category, paper, image, audio, now ->
            HumanLetter(
                id = id,
                title = title,
                body = body,
                category = category,
                paper = paper,
                image = image,
                audio = audio,
                createdAtMillis = now,
                state = TreeholeLetterState.REVIEWING,
            )
        },
    )

    /** 生灵内容池（独立存储，只含 [PetLetter]）。 */
    val petPool: TreeholePool<PetLetter> = TreeholePool(
        seedProvider = { loader.load(PET_ASSET, PetLettersFile.serializer()).letters },
        idPrefix = "tlp-",
        categories = PET_POOL_CATEGORIES,
        builder = { id, title, body, category, paper, image, audio, now ->
            PetLetter(
                id = id,
                title = title,
                body = body,
                category = category,
                paper = paper,
                image = image,
                audio = audio,
                createdAtMillis = now,
                state = TreeholeLetterState.REVIEWING,
            )
        },
    )

    /** 取指定池（只读公共形态；类型由池决定，不跨池）。 */
    fun poolFor(type: TreeholePoolType): TreeholePool<out TreeholeLetterLike> = when (type) {
        TreeholePoolType.HUMAN_POOL -> humanPool
        TreeholePoolType.PET_POOL -> petPool
    }

    private companion object {
        const val HUMAN_ASSET = "treehole/human-letters.json"
        const val PET_ASSET = "treehole/pet-letters.json"
    }
}

/** 树洞仓库定位器（懒加载单例）。 */
object TreeholeServiceLocator {

    @Volatile
    private var repository: TreeholeRepository? = null

    fun repository(context: Context): TreeholeRepository {
        val appContext = context.applicationContext
        return repository ?: synchronized(this) {
            repository ?: TreeholeRepository(appContext).also { repository = it }
        }
    }
}
