/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.home.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 首页资讯条目。正文以段落形式保存，[imageKey] 对应素材清单中的图片键
 * （如 news_ecoburial_cycle），由 UI 层解析为可绘制资源。
 */
@Serializable
data class NewsArticle(
    val id: String,
    val title: String,
    val source: String,
    val author: String,
    val publishTime: String,
    val summary: String,
    val imageKey: String? = null,
    val paragraphs: List<String> = emptyList(),
)

/** 活动状态：报名中 / 即将开始 / 已结束。 */
@Serializable
enum class ActivityStatus(val label: String) {
    @SerialName("报名中")
    SIGNING("报名中"),

    @SerialName("即将开始")
    UPCOMING("即将开始"),

    @SerialName("已结束")
    ENDED("已结束"),
}

/** 近期 / 集体纪念活动条目。 */
@Serializable
data class ActivityEvent(
    val id: String,
    val title: String,
    val type: String,
    val time: String,
    val location: String,
    val status: ActivityStatus,
    val organizer: String,
    val imageKey: String? = null,
    val summary: String,
    val detail: List<String> = emptyList(),
)

/** 生命教育课程 / 主题。 */
@Serializable
data class LifeEdCourse(
    val id: String,
    val title: String,
    val category: String,
    val durationMinutes: Int,
    val level: String? = null,
    val summary: String,
    val imageKey: String? = null,
    val paragraphs: List<String> = emptyList(),
)
