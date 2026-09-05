/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.home

import com.yuanqinglan.app.feature.home.logic.NewsShuffleEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 首页资讯"换一换"引擎测试：8 篇不重复、相邻批次互斥、
 * 全部文章都有机会出现、可反复刷新。
 */
class NewsShuffleEngineTest {

    private fun engine(total: Int = 8, batch: Int = 4) = NewsShuffleEngine(total, batch)

    @Test
    fun `first batch has exactly four unique indices`() {
        val batches = (1..6).map { engine().nextBatch() }
        batches.forEach { batch ->
            assertEquals(4, batch.size)
            assertEquals(4, batch.toSet().size)
            assertTrue(batch.all { it in 0 until 8 })
        }
    }

    @Test
    fun `two consecutive batches are disjoint and together cover all eight`() {
        val e = engine()
        val first = e.nextBatch()
        val second = e.nextBatch()
        assertTrue("相邻两批不应重复：$first vs $second", first.toSet().intersect(second.toSet()).isEmpty())
        assertEquals(8, (first + second).toSet().size)
    }

    @Test
    fun `every article appears across refreshes`() {
        val e = engine()
        val seen = mutableSetOf<Int>()
        repeat(6) { seen += e.nextBatch() }
        assertEquals((0 until 8).toSet(), seen)
    }

    @Test
    fun `can keep refreshing many times without exception`() {
        val e = engine()
        repeat(50) {
            val batch = e.nextBatch()
            assertEquals(4, batch.size)
        }
    }

    @Test
    fun `every batch in a long run is internally unique`() {
        val e = engine()
        repeat(100) {
            val batch = e.nextBatch()
            assertEquals(batch.size, batch.toSet().size)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects batch size larger than total`() {
        NewsShuffleEngine(total = 8, batchSize = 9)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects total not divisible by batch size`() {
        NewsShuffleEngine(total = 9, batchSize = 4)
    }
}
