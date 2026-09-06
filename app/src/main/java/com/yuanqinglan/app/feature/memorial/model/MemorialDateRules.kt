/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.model

import java.time.LocalDate

/**
 * 纪念对象日期（出生/离世）的纯规则：合法性校验、自然格式化与转换。
 * 全部为无状态纯函数，不依赖 Android/Store，可直接单元测试。
 *
 * 规则约定：
 * - 未知日期用 `null` 表示；仅年份/年月/完整日期用 [MemorialDate] 的精度表达；
 * - 未来日期禁止提交（按字段就近报错，如「出生日期不能晚于今天」）；
 * - 出生不晚于离世：以两者可比较的最细精度判断，无法证明逆序（如一方仅年份）
 *   时按不逆序处理，避免对不完整信息误报错；
 * - 完整日期须为真实历法日期（闰年感知，2 月 29 日仅闰年合法）。
 */
object MemorialDateRules {

    /** 仅年份输入允许的合理范围（UI 年份录入用；数据层合法范围更宽）。 */
    val YEAR_RANGE: IntRange = 1900..2100

    /**
     * 出生日期字段错误（未知/空返回 null）：内部不合法或晚于今天时给出就近文案。
     */
    fun birthError(birth: MemorialDate?, today: LocalDate): String? = when {
        birth == null -> null
        !birth.isValid() -> "出生日期不合法"
        isFuture(birth, today) -> "出生日期不能晚于今天"
        else -> null
    }

    /**
     * 离世日期字段错误（未知/空返回 null）：内部不合法或晚于今天时给出就近文案。
     */
    fun deathError(death: MemorialDate?, today: LocalDate): String? = when {
        death == null -> null
        !death.isValid() -> "离世日期不合法"
        isFuture(death, today) -> "离世日期不能晚于今天"
        else -> null
    }

    /**
     * 出生/离世先后关系错误：出生晚于离世时返回文案（挂在出生字段就近说明），
     * 无法证明逆序或任一为空时返回 null。
     */
    fun orderError(birth: MemorialDate?, death: MemorialDate?): String? {
        if (birth == null || death == null) return null
        if (!birth.isValid() || !death.isValid()) return null
        return if (birth.compareToPrecision(death) > 0) "出生日期不能晚于离世日期" else null
    }

    /**
     * 整组校验入口：返回按字段的错误；全部合法时 [MemorialDateValidation.isOk] 为 true。
     * [today] 可注入以便测试，默认取当前日期。
     */
    fun validate(
        birth: MemorialDate?,
        death: MemorialDate?,
        today: LocalDate = LocalDate.now(),
    ): MemorialDateValidation = MemorialDateValidation(
        birthError = birthError(birth, today) ?: orderError(birth, death),
        deathError = deathError(death, today),
    )

    /**
     * 自然格式化：未知 → 「未知」；仅年份 → 「1996年」；年月 → 「1996年2月」；
     * 完整 → 「1996年2月3日」。绝不输出 null/0/占位符。
     */
    fun formatMemorialDate(date: MemorialDate?): String = date?.displayText ?: "未知"

    /** 非空日期的自然展示文本（见 [formatMemorialDate]）。 */
    val MemorialDate.displayText: String
        get() = when {
            month == null -> "${year}年"
            day == null -> "${year}年${month}月"
            else -> "${year}年${month}月${day}日"
        }

    /** 是否晚于今天（仅年份按年比较，年月按到月比较，完整日期精确到日）。 */
    fun isFuture(date: MemorialDate, today: LocalDate): Boolean {
        if (date.year > today.year) return true
        if (date.year < today.year) return false
        val month = date.month ?: return false
        if (month > today.monthValue) return true
        if (month < today.monthValue) return false
        val day = date.day ?: return false
        return day > today.dayOfMonth
    }

    /**
     * 精度感知先后比较：先比年；同年且双方都有月时比月；同月且双方都有日时比日。
     * 任一比较维度缺失（无法证明先后）即返回 0，避免对不完整日期误判。
     */
    fun MemorialDate.compareToPrecision(other: MemorialDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        val thisMonth = month ?: return 0
        val otherMonth = other.month ?: return 0
        if (thisMonth != otherMonth) return thisMonth.compareTo(otherMonth)
        val thisDay = day ?: return 0
        val otherDay = other.day ?: return 0
        return thisDay.compareTo(otherDay)
    }

    /** 是否为真实历法日期：月 1..12、日按年月天数（闰年感知）。 */
    fun MemorialDate.isValid(): Boolean {
        if (year !in 1..9999) return false
        val m = month ?: return true
        if (m !in 1..12) return false
        val d = day ?: return true
        return d in 1..daysInMonth(year, m)
    }

    /** 完整日期转 LocalDate；非完整或非法返回 null（供日期选择器回填）。 */
    fun MemorialDate.toLocalDateOrNull(): LocalDate? {
        val m = month ?: return null
        val d = day ?: return null
        return runCatching { LocalDate.of(year, m, d) }.getOrNull()
    }

    /** 日期选择器选择的完整日期 → 结构化 [MemorialDate]。 */
    fun LocalDate.toMemorialDate(): MemorialDate = MemorialDate(year, monthValue, dayOfMonth)

    private fun daysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 0
    }

    private fun isLeapYear(year: Int): Boolean =
        year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
}

/** 日期校验结果：按字段错误 + 是否整体可提交。 */
data class MemorialDateValidation(
    val birthError: String? = null,
    val deathError: String? = null,
) {
    val isOk: Boolean get() = birthError == null && deathError == null
}
