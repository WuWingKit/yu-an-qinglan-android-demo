/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yuanqinglan.app.feature.burial.model.BurialMode
import com.yuanqinglan.app.feature.burial.ui.BurialHomeScreen
import com.yuanqinglan.app.feature.burial.ui.BurialNavigateScreen
import com.yuanqinglan.app.feature.burial.ui.BurialOrderScreen
import com.yuanqinglan.app.feature.burial.ui.BurialPlanFormScreen
import com.yuanqinglan.app.feature.burial.ui.BurialPlanScreen
import com.yuanqinglan.app.feature.burial.ui.HumanBurialDetailScreen
import com.yuanqinglan.app.feature.burial.ui.PetBurialDetailScreen
import com.yuanqinglan.app.feature.burial.ui.PetParkScreen
import com.yuanqinglan.app.navigation.AppRoute

/**
 * 安葬模块 10 路由注册扩展（供主 Agent 在顶层 NavHost 中调用）。
 *
 * - tree / flower / grass：人类葬式详情，各自绑定固定服务 ID；
 * - pet-tree / pet-park：宠物三葬式共享参数化模板，?mode= 为受控枚举，
 *   非法/缺失值经 [BurialMode.parseRouteMode] 回退 TREE（三张宠物卡分别
 *   进入各自正确模式，不复制参考网页三卡同跳缺陷）；
 * - plan-form/{planId}、order/{orderId}：稳定 StringType ID。
 */
fun NavGraphBuilder.burialNavGraph(navController: NavHostController) {
    composable(AppRoute.BURIAL.route) {
        BurialHomeScreen(navController = navController)
    }

    composable(AppRoute.TREE.route) {
        HumanBurialDetailScreen(serviceId = "tree", navController = navController)
    }
    composable(AppRoute.FLOWER.route) {
        HumanBurialDetailScreen(serviceId = "flower", navController = navController)
    }
    composable(AppRoute.GRASS.route) {
        HumanBurialDetailScreen(serviceId = "grass", navController = navController)
    }

    composable(
        route = "${AppRoute.PET_TREE.route}?mode={mode}",
        arguments = listOf(
            navArgument(MODE_ARG) {
                type = NavType.StringType
                defaultValue = BurialMode.TREE.token
            },
        ),
    ) { entry ->
        PetBurialDetailScreen(
            mode = BurialMode.parseRouteMode(entry.arguments?.getString(MODE_ARG)),
            navController = navController,
        )
    }

    composable(
        route = "${AppRoute.PET_PARK.route}?mode={mode}",
        arguments = listOf(
            navArgument(MODE_ARG) {
                type = NavType.StringType
                defaultValue = BurialMode.TREE.token
            },
        ),
    ) { entry ->
        PetParkScreen(
            mode = BurialMode.parseRouteMode(entry.arguments?.getString(MODE_ARG)),
            navController = navController,
        )
    }

    composable(AppRoute.PLAN.route) {
        BurialPlanScreen(navController = navController)
    }

    composable(
        route = "${AppRoute.PLAN_FORM.route}/{planId}",
        arguments = listOf(
            navArgument(PLAN_ID_ARG) { type = NavType.StringType },
        ),
    ) { entry ->
        val planId = requireNotNull(entry.arguments?.getString(PLAN_ID_ARG)) {
            "plan-form 缺少 planId 参数"
        }
        BurialPlanFormScreen(planId = planId, navController = navController)
    }

    composable(
        route = "${AppRoute.ORDER.route}/{orderId}",
        arguments = listOf(
            navArgument(ORDER_ID_ARG) { type = NavType.StringType },
        ),
    ) { entry ->
        val orderId = requireNotNull(entry.arguments?.getString(ORDER_ID_ARG)) {
            "order 缺少 orderId 参数"
        }
        BurialOrderScreen(orderId = orderId, navController = navController)
    }

    composable(AppRoute.NAVIGATE.route) {
        BurialNavigateScreen(navController = navController)
    }
}

private const val MODE_ARG = "mode"
private const val PLAN_ID_ARG = "planId"
private const val ORDER_ID_ARG = "orderId"
