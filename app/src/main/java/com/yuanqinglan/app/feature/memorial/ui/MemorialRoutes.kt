/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.ui

import com.yuanqinglan.app.navigation.AppRoute

/** 追忆模块路由串构造（键保持冻结 AppRoute，参数为纪念空间稳定 ID）。 */
object MemorialRoutes {
    private const val MEMORIAL_ID = "memorialId"
    private const val LETTER_ID = "letterId"

    fun detail(memorialId: String): String = "${AppRoute.MEMORIAL_DETAIL.route}/$memorialId"
    fun main(memorialId: String): String = "${AppRoute.MEMORIAL_MAIN.route}/$memorialId"
    fun petMemorial(memorialId: String): String = "${AppRoute.PET_MEMORIAL.route}/$memorialId"
    fun story(memorialId: String): String = "${AppRoute.MEMORIAL_STORY.route}/$memorialId"
    fun storyAdd(memorialId: String): String = "${AppRoute.STORY_ADD.route}/$memorialId"
    fun diary(memorialId: String): String = "${AppRoute.MEMORIAL_DIARY.route}/$memorialId"
    fun letterWrite(memorialId: String): String = "${AppRoute.LETTER_WRITE.route}/$memorialId"
    fun letterView(letterId: String): String = "${AppRoute.LETTER_VIEW.route}/$letterId"
    fun aiUpload(memorialId: String): String = "${AppRoute.AI_UPLOAD.route}/$memorialId"
    fun daiji(memorialId: String): String = "${AppRoute.PROXY_MEMORIAL.route}/$memorialId"
}
