/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.home.ui

import androidx.annotation.DrawableRes
import com.yuanqinglan.app.R

/**
 * 首页内容图片键 → 可绘制资源解析。
 * 图片键存于 JSON（news.imageKey 等）；素材随 drawable-nodpi 落位后按清单启用。
 * 未收录的键返回 null，页面以图标占位呈现。
 */
object HomeVisuals {

    private val newsImages: Map<String, Int> = mapOf(
        "news_ecoburial_cycle" to R.drawable.news_ecoburial_cycle,
        "news_bayu_customs" to R.drawable.news_bayu_customs,
    )

    private val activityImages: Map<String, Int> = mapOf(
        "activity_collective_memorial" to R.drawable.activity_collective_memorial,
        "activity_life_education" to R.drawable.activity_life_education,
    )

    private val lifeEdImages: Map<String, Int> = mapOf(
        "activity_life_education" to R.drawable.activity_life_education,
    )

    @DrawableRes
    fun newsImage(imageKey: String?): Int? = imageKey?.let(newsImages::get)

    @DrawableRes
    fun activityImage(imageKey: String?): Int? = imageKey?.let(activityImages::get)

    @DrawableRes
    fun lifeEdImage(imageKey: String?): Int? = imageKey?.let(lifeEdImages::get)
}
