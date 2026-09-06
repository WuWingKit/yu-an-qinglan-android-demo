/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.model

import java.util.concurrent.atomic.AtomicLong

/**
 * 纪念空间标识管理：人类 `hm-*` / 宠物 `pm-*` 两套互斥的 ID 空间。
 * 轨道隔离在标识层即生效——任何按 ID 解析轨道/仓库的逻辑都不可能命中另一轨。
 */
enum class MemorialTrack {
    HUMAN,
    PET,
    ;

    companion object {
        const val PREFIX_HUMAN = "hm-"
        const val PREFIX_PET = "pm-"

        /** 按纪念空间 ID 解析所属轨道；未知前缀视为非法并抛出明确错误。 */
        fun ofId(memorialId: String): MemorialTrack = when {
            memorialId.startsWith(PREFIX_HUMAN) -> HUMAN
            memorialId.startsWith(PREFIX_PET) -> PET
            else -> throw IllegalArgumentException(
                "纪念空间 ID 无法解析所属轨道: $memorialId（人类前缀 hm-、宠物前缀 pm-）",
            )
        }

        fun nextId(track: MemorialTrack): String = when (track) {
            HUMAN -> "$PREFIX_HUMAN${IdCounter.next()}"
            PET -> "$PREFIX_PET${IdCounter.next()}"
        }
    }
}

/** 模块内稳定 ID 生成（内容节点/信件/日记等子对象共用）。 */
object MemorialIds {
    private val seq = AtomicLong(0L)

    fun next(prefix: String): String {
        val base = seq.incrementAndGet()
        return "$prefix${System.currentTimeMillis()}-$base"
    }
}

/**
 * 纪念空间创建表单本地校验（纯规则，无状态）。
 * 规则：名称必填且 ≤ 12 字；关系/称呼必填且 ≤ 12 字；简介 ≤ 60 字（可为空）。
 */
object MemorialFormRules {
    const val MAX_NAME_LENGTH = 12
    const val MAX_RELATION_LENGTH = 12
    const val MAX_INTRO_LENGTH = 60

    fun nameError(name: String): String? = when {
        name.isBlank() -> "请填写纪念对象名称"
        name.trim().length > MAX_NAME_LENGTH -> "名称不能超过 $MAX_NAME_LENGTH 个字"
        else -> null
    }

    /** relation 语义随轨道不同（人类为关系称呼，宠物为陪伴身份）。 */
    fun relationError(relation: String): String? = when {
        relation.isBlank() -> "请填写与纪念对象的关系"
        relation.trim().length > MAX_RELATION_LENGTH -> "关系不能超过 $MAX_RELATION_LENGTH 个字"
        else -> null
    }

    fun introError(intro: String): String? =
        if (intro.trim().length > MAX_INTRO_LENGTH) "简介不能超过 $MAX_INTRO_LENGTH 个字" else null

    /** 整表单是否可提交。 */
    fun canSubmit(name: String, relation: String, intro: String): Boolean =
        nameError(name) == null && relationError(relation) == null && introError(intro) == null
}

private object IdCounter {
    private val counter = AtomicLong(0L)

    fun next(): Long {
        val now = System.currentTimeMillis()
        val seq = counter.incrementAndGet()
        return now * 1_000_000L + seq
    }
}
