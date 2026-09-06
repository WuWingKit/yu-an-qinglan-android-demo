/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.policy

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yuanqinglan.app.feature.policy.ui.CountyDetailRoute
import com.yuanqinglan.app.feature.policy.ui.PolicyRoute
import com.yuanqinglan.app.feature.policy.ui.PrecheckRoute
import com.yuanqinglan.app.feature.policy.ui.SeaDetailRoute
import com.yuanqinglan.app.navigation.AppRoute

/**
 * 政策链路 NavGraph（home_policy 独占实现）。
 * 由主 Agent 在顶层 NavHost 调用；此处注册：
 * policy、county-detail/{countyId}、presult?countyId=、sea-detail。
 */
fun NavGraphBuilder.policyNavGraph(navController: NavHostController) {

    composable(AppRoute.POLICY.route) {
        PolicyRoute(
            onBack = { navController.popBackStack() },
            onOpenCounty = { countyId ->
                navController.navigate("${AppRoute.COUNTY_DETAIL.route}/$countyId")
            },
            onOpenPrecheck = { countyId ->
                navController.navigate("${AppRoute.POLICY_RESULT.route}?countyId=$countyId")
            },
            onOpenSeaDetail = { navController.navigate(AppRoute.SEA_DETAIL.route) },
        )
    }

    composable(
        route = "${AppRoute.COUNTY_DETAIL.route}/{countyId}",
        arguments = listOf(
            navArgument("countyId") { type = NavType.StringType },
        ),
    ) { entry ->
        val countyId = entry.arguments?.getString("countyId").orEmpty()
        CountyDetailRoute(
            countyId = countyId,
            onBack = { navController.popBackStack() },
            onStartPrecheck = { id ->
                navController.navigate("${AppRoute.POLICY_RESULT.route}?countyId=$id")
            },
        )
    }

    composable(
        route = "${AppRoute.POLICY_RESULT.route}?countyId={countyId}",
        arguments = listOf(
            navArgument("countyId") {
                type = NavType.StringType
                defaultValue = ""
            },
        ),
    ) { entry ->
        val countyId = entry.arguments?.getString("countyId").orEmpty()
        PrecheckRoute(
            countyIdArg = countyId,
            onBack = { navController.popBackStack() },
            onOpenSeaDetail = { navController.navigate(AppRoute.SEA_DETAIL.route) },
        )
    }

    composable(AppRoute.SEA_DETAIL.route) {
        SeaDetailRoute(onBack = { navController.popBackStack() })
    }
}
