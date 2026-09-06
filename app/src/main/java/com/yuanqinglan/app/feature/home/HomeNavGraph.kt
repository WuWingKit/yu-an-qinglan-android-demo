/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yuanqinglan.app.feature.home.ui.ActivitiesRoute
import com.yuanqinglan.app.feature.home.ui.HomeRoute
import com.yuanqinglan.app.feature.home.ui.LifeEdRoute
import com.yuanqinglan.app.feature.home.ui.MatchRoute
import com.yuanqinglan.app.feature.home.ui.NewsDetailRoute
import com.yuanqinglan.app.navigation.AppRoute

/**
 * 首页 Tab 根路由与子页面（home_policy 独占实现）。
 * 由主 Agent 在顶层 NavHost 调用；此处注册：
 * home（Tab 根）、life-ed、activities、match、news-detail/{newsId}。
 */
fun NavGraphBuilder.homeNavGraph(navController: NavHostController) {

    /** 切换一级 Tab：与底栏语义一致（保留各 Tab 回退栈）。 */
    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(AppRoute.HOME.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    composable(AppRoute.HOME.route) {
        HomeRoute(
            onOpenBurial = { navigateTopLevel(AppRoute.BURIAL.route) },
            onOpenTree = { navController.navigate(AppRoute.TREE.route) },
            onOpenFlower = { navController.navigate(AppRoute.FLOWER.route) },
            onOpenLawn = { navController.navigate(AppRoute.GRASS.route) },
            onOpenPet = { navController.navigate("${AppRoute.PET_TREE.route}?mode=TREE") },
            onOpenPolicy = { navController.navigate(AppRoute.POLICY.route) },
            onOpenMemorial = { navigateTopLevel(AppRoute.MEMORIAL_HOME.route) },
            onOpenActivities = { navController.navigate(AppRoute.ACTIVITIES.route) },
            onOpenLifeEd = { navController.navigate(AppRoute.LIFE_EDUCATION.route) },
            onOpenMatch = { navController.navigate(AppRoute.MATCH.route) },
            onOpenNewsDetail = { newsId ->
                navController.navigate("${AppRoute.NEWS_DETAIL.route}/$newsId")
            },
        )
    }

    composable(AppRoute.LIFE_EDUCATION.route) {
        LifeEdRoute(onBack = { navController.popBackStack() })
    }

    composable(AppRoute.ACTIVITIES.route) {
        ActivitiesRoute(onBack = { navController.popBackStack() })
    }

    composable(AppRoute.MATCH.route) {
        MatchRoute(
            onBack = { navController.popBackStack() },
            onOpenBurial = { navigateTopLevel(AppRoute.BURIAL.route) },
            onOpenPolicy = { navController.navigate(AppRoute.POLICY.route) },
        )
    }

    composable(
        route = "${AppRoute.NEWS_DETAIL.route}/{newsId}",
        arguments = listOf(
            navArgument("newsId") { type = NavType.StringType },
        ),
    ) { entry ->
        val newsId = entry.arguments?.getString("newsId").orEmpty()
        NewsDetailRoute(
            newsId = newsId,
            onBack = { navController.popBackStack() },
        )
    }
}
