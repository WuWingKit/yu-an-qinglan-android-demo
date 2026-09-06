/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.treehole

import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.feature.treehole.data.TreeholePool
import com.yuanqinglan.app.feature.treehole.model.HUMAN_POOL_CATEGORIES
import com.yuanqinglan.app.feature.treehole.model.HumanLetter
import com.yuanqinglan.app.feature.treehole.model.PET_POOL_CATEGORIES
import com.yuanqinglan.app.feature.treehole.model.PetLetter
import com.yuanqinglan.app.feature.treehole.model.TreeholeLetterState
import com.yuanqinglan.app.feature.treehole.model.TreeholePaperStyle
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 树洞双池隔离与审核状态门禁（纯 JVM）：
 * - 人间/生灵两池为独立实例：互不影响对方"我的信件"列表，id 前缀互斥；
 * - 寄出后进入待审核（REVIEWING），不进入公共拾信池（publicLetters 数据源不变）。
 * 对应 repository 中 humanPool/petPool（poolFor 仅返回各自强类型池）。
 */
class TreeholeIsolationTest {

    // ---------- 构造辅助 ----------

    private fun seedHuman(id: String, title: String = "标题$id"): HumanLetter = HumanLetter(
        id = id,
        title = title,
        body = "这是 ${id} 的正文。",
        category = "思念",
        paper = TreeholePaperStyle.PLAIN,
        image = null,
        audio = null,
        createdAtMillis = 1_700_000_000_000L,
        state = TreeholeLetterState.PUBLISHED,
    )

    private fun seedPet(id: String, title: String = "标题$id"): PetLetter = PetLetter(
        id = id,
        title = title,
        body = "这是 ${id} 的正文。",
        category = "想念",
        paper = TreeholePaperStyle.GREEN,
        image = null,
        audio = null,
        createdAtMillis = 1_700_000_000_000L,
        state = TreeholeLetterState.PUBLISHED,
    )

    /** 模拟 repository 中 humanPool 的构造（提交即 REVIEWING、id 前缀 tlh-）。 */
    private fun humanPool(seeds: List<HumanLetter> = emptyList()): TreeholePool<HumanLetter> =
        TreeholePool<HumanLetter>(
            seedProvider = { seeds },
            idPrefix = "tlh-",
            categories = HUMAN_POOL_CATEGORIES,
            builder = { id, title, body, category, paper, image, audio, createdAtMillis ->
                HumanLetter(
                    id = id,
                    title = title,
                    body = body,
                    category = category,
                    paper = paper,
                    image = image,
                    audio = audio,
                    createdAtMillis = createdAtMillis,
                    state = TreeholeLetterState.REVIEWING,
                )
            },
        )

    /** 模拟 repository 中 petPool 的构造（提交即 REVIEWING、id 前缀 tlp-）。 */
    private fun petPool(seeds: List<PetLetter> = emptyList()): TreeholePool<PetLetter> =
        TreeholePool<PetLetter>(
            seedProvider = { seeds },
            idPrefix = "tlp-",
            categories = PET_POOL_CATEGORIES,
            builder = { id, title, body, category, paper, image, audio, createdAtMillis ->
                PetLetter(
                    id = id,
                    title = title,
                    body = body,
                    category = category,
                    paper = paper,
                    image = image,
                    audio = audio,
                    createdAtMillis = createdAtMillis,
                    state = TreeholeLetterState.REVIEWING,
                )
            },
        )

    // ---------- a) 双池互不串 ----------

    @Test
    fun `人间池提交后生灵池我的信件不受影响`() = runBlocking {
        val petSeeds = listOf(seedPet("tlp-001"), seedPet("tlp-002"))
        val human = humanPool(seeds = listOf(seedHuman("tlh-001")))
        val pet = petPool(seeds = petSeeds)

        val submitted = human.submit(
            title = "给老朋友",
            body = "有些话想写给你。",
            category = "思念",
            paper = TreeholePaperStyle.WARM,
            image = null,
            audio = null,
        )

        // 人间池收到本人信件
        assertEquals(1, human.mineLetters.value.size)
        assertEquals(submitted.id, human.mineLetters.value.single().id)
        assertEquals(TreeholeLetterState.REVIEWING, human.mineLetters.value.single().state)
        // 生灵池"我的信件"保持为空
        assertTrue(pet.mineLetters.value.isEmpty())

        // id 前缀互斥：tlh- 信件永远进不了 tlp- 池的 id 空间
        assertTrue(human.mineLetters.value.all { it.id.startsWith("tlh-") })
        assertTrue(petSeeds.all { it.id.startsWith("tlp-") })
        assertTrue(pet.mineLetters.value.none { it.id.startsWith("tlh-") })
    }

    @Test
    fun `生灵池提交后人间池我的信件不受影响`() = runBlocking {
        val humanSeeds = listOf(seedHuman("tlh-001"), seedHuman("tlh-002"))
        val human = humanPool(seeds = humanSeeds)
        val pet = petPool()

        pet.submit(
            title = "给窗台的小猫",
            body = "想你了。",
            category = "想念",
            paper = TreeholePaperStyle.PLAIN,
            image = null,
            audio = null,
        )

        assertEquals(1, pet.mineLetters.value.size)
        assertTrue(human.mineLetters.value.isEmpty())
        assertTrue(human.mineLetters.value.none { it.id.startsWith("tlp-") })
        assertTrue(pet.mineLetters.value.all { it.id.startsWith("tlp-") })
    }

    @Test
    fun `两池分类清单互斥且与各自内容池一致`() {
        val human = humanPool()
        val pet = petPool()
        assertEquals(HUMAN_POOL_CATEGORIES, human.availableCategories())
        assertEquals(PET_POOL_CATEGORIES, pet.availableCategories())
        // 分类不跨池：两池分类集合完全不相交
        assertTrue(human.availableCategories().none { it in pet.availableCategories() })
        assertTrue(pet.availableCategories().none { it in human.availableCategories() })
    }

    // ---------- b) 本人 REVIEWING 不进公共拾信池 ----------

    @Test
    fun `提交前后公共拾信池数据源相同且不包含本人信件`() = runBlocking {
        val seeds = listOf(seedHuman("tlh-101"), seedHuman("tlh-102"))
        val pool = humanPool(seeds = seeds)

        val before = pool.publicLetters().toList()
        assertEquals(2, before.size)
        assertEquals(DemoState.Loading, before[0])
        val beforeSuccess = before[1] as DemoState.Success<List<HumanLetter>>
        assertEquals(seeds, beforeSuccess.value)
        assertTrue(beforeSuccess.value.all { it.state == TreeholeLetterState.PUBLISHED })

        pool.submit(
            title = "写给未来的自己",
            body = "慢慢来，也来得及。",
            category = "祝福",
            paper = TreeholePaperStyle.GREEN,
            image = null,
            audio = null,
        )

        // 本人列表出现待审核信件
        val mine = pool.mineLetters.value
        assertEquals(1, mine.size)
        assertEquals(TreeholeLetterState.REVIEWING, mine.single().state)

        // 公共池数据源与提交前一致：REVIEWING 信件不会进入拾信池
        val after = pool.publicLetters().toList()
        assertEquals(2, after.size)
        assertEquals(DemoState.Loading, after[0])
        val afterSuccess = after[1] as DemoState.Success<List<HumanLetter>>
        assertEquals(beforeSuccess.value, afterSuccess.value)
        assertTrue(afterSuccess.value.none { it.id == mine.single().id })
        assertTrue(afterSuccess.value.all { it.state == TreeholeLetterState.PUBLISHED })
    }

    @Test
    fun `两池提交后合并 id 集合保持唯一`() = runBlocking {
        val human = humanPool()
        val pet = petPool()
        human.submit(
            title = "人间信",
            body = "人间正文",
            category = "倾诉",
            paper = TreeholePaperStyle.PLAIN,
            image = null,
            audio = null,
        )
        pet.submit(
            title = "生灵信",
            body = "生灵正文",
            category = "谢谢你",
            paper = TreeholePaperStyle.WARM,
            image = null,
            audio = null,
        )
        val allIds = human.mineLetters.value.map { it.id } + pet.mineLetters.value.map { it.id }
        assertEquals(2, allIds.toSet().size)
        assertTrue(human.mineLetters.value.single().id.startsWith("tlh-"))
        assertTrue(pet.mineLetters.value.single().id.startsWith("tlp-"))
    }
}
