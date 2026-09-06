/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial.ui

import com.yuanqinglan.app.feature.burial.model.BurialMode
import com.yuanqinglan.app.navigation.AppRoute

/** 安葬模块路由串构造（键保持 45 个冻结键，参数为受控枚举/稳定 ID）。 */
object BurialRoutes {
    private const val MODE_ARG = "mode"

    fun petDetail(mode: BurialMode): String =
        "${AppRoute.PET_TREE.route}?$MODE_ARG=${mode.token}"

    fun petPark(mode: BurialMode): String =
        "${AppRoute.PET_PARK.route}?$MODE_ARG=${mode.token}"

    fun planForm(planId: String): String =
        "${AppRoute.PLAN_FORM.route}/$planId"

    fun order(orderId: String): String =
        "${AppRoute.ORDER.route}/$orderId"
}
