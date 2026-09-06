/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.home.logic

import kotlin.random.Random

/**
 * 首页资讯"换一换"引擎：保证单批不重复、相邻两批互不重复，
 * 且全部条目都有机会被展示。
 *
 * 策略：第一轮从全部条目中随机抽取一批；第二轮从剩余条目中抽取下一批，
 * 两批恰好覆盖全集；随后重新洗牌进入下一轮。因此任意相邻两批互斥，
 * 任意条目至多在两次刷新内出现。
 *
 * @param total 条目总数（如 8 篇资讯）。
 * @param batchSize 每批展示条数（如 4 条）。
 */
class NewsShuffleEngine(
    private val total: Int,
    private val batchSize: Int,
    private val random: Random = Random.Default,
) {
    init {
        require(total > 0) { "条目总数必须大于 0" }
        require(batchSize in 1..total) { "每批条数必须在 1..$total 之间" }
        require(total % batchSize == 0) { "条目总数必须是每批条数的整数倍，实际 total=$total batchSize=$batchSize" }
    }

    private var remainder: List<Int> = emptyList()

    /** 返回下一批条目的索引（下标，0..total-1）。 */
    fun nextBatch(): List<Int> {
        val remaining = if (remainder.isEmpty()) {
            (0 until total).shuffled(random)
        } else {
            remainder
        }
        val batch = remaining.take(batchSize)
        remainder = remaining.drop(batchSize)
        return batch
    }
}
