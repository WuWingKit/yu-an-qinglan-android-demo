/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial.ui

import androidx.annotation.DrawableRes
import com.yuanqinglan.app.R

/**
 * 服务 JSON 中的图片资源名 → drawable 映射。
 * 人类与宠物图片资源互不混用；未知资源名抛错并交由页面错误态/重试处理。
 */
object BurialArtwork {

    private val humanImageByMode: Map<String, String> = mapOf(
        "tree" to "burial_tree_grove",
        "flower" to "burial_flower_garden",
        "grass" to "burial_lawn",
    )

    private val petImageByMode: Map<String, String> = mapOf(
        "TREE" to "burial_pet_tree",
        "FLOWER" to "burial_pet_flower",
        "LAWN" to "burial_pet_lawn",
    )

    /** 人类服务缺省图片 token（与资源文件同名校验用，仅测试/回退引用）。 */
    fun humanImageTokenFor(modeToken: String): String =
        humanImageByMode[modeToken]
            ?: throw IllegalArgumentException("未收录的人类葬式图片配置: $modeToken")

    /** 宠物服务缺省图片 token（三模式各自正确区分）。 */
    fun petImageTokenFor(modeToken: String): String =
        petImageByMode[modeToken]
            ?: throw IllegalArgumentException("未收录的宠物葬式图片配置: $modeToken")

    @DrawableRes
    fun imageRes(name: String): Int = when (name) {
        "burial_tree_grove" -> R.drawable.burial_tree_grove
        "burial_flower_garden" -> R.drawable.burial_flower_garden
        "burial_lawn" -> R.drawable.burial_lawn
        "burial_pet_tree" -> R.drawable.burial_pet_tree
        "burial_pet_flower" -> R.drawable.burial_pet_flower
        "burial_pet_lawn" -> R.drawable.burial_pet_lawn
        "park_overview_map" -> R.drawable.park_overview_map
        else -> throw IllegalArgumentException("未收录的安葬图片资源: $name")
    }

    @DrawableRes
    fun parkMapRes(): Int = R.drawable.park_overview_map
}
