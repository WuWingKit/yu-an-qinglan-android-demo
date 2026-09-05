/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yuanqinglan.app.feature.memorial.ui.AiEthicsScreen
import com.yuanqinglan.app.feature.memorial.ui.AiUploadScreen
import com.yuanqinglan.app.feature.memorial.ui.DaijiScreen
import com.yuanqinglan.app.feature.memorial.ui.JisiTimeScreen
import com.yuanqinglan.app.feature.memorial.ui.JitiHistoryScreen
import com.yuanqinglan.app.feature.memorial.ui.LetterViewScreen
import com.yuanqinglan.app.feature.memorial.ui.LetterWriteScreen
import com.yuanqinglan.app.feature.memorial.ui.MemorialCreateScreen
import com.yuanqinglan.app.feature.memorial.ui.MemorialDetailScreen
import com.yuanqinglan.app.feature.memorial.ui.MemorialDiaryScreen
import com.yuanqinglan.app.feature.memorial.ui.MemorialHomeScreen
import com.yuanqinglan.app.feature.memorial.ui.MemorialMainScreen
import com.yuanqinglan.app.feature.memorial.ui.MemorialStoryScreen
import com.yuanqinglan.app.feature.memorial.ui.StoryAddScreen
import com.yuanqinglan.app.navigation.AppRoute

/**
 * 追忆模块完整导航图：注册模块全部 15 个目的路由。
 *
 * 键一律使用冻结的 [AppRoute] 字面量；带参路由统一使用
 * `navArgument { type = NavType.StringType }` 声明参数。
 */
fun NavGraphBuilder.memorialNavGraph(navController: NavHostController) {
    composable(AppRoute.MEMORIAL_HOME.route) {
        MemorialHomeScreen(navController)
    }
    composable(AppRoute.MEMORIAL_CREATE.route) {
        MemorialCreateScreen(navController)
    }
    composable(
        route = "${AppRoute.MEMORIAL_DETAIL.route}/{memorialId}",
        arguments = listOf(navArgument(MEMORIAL_ID_ARG) { type = NavType.StringType }),
    ) { backStackEntry ->
        MemorialDetailScreen(
            memorialId = backStackEntry.arguments?.getString(MEMORIAL_ID_ARG).orEmpty(),
            navController = navController,
        )
    }
    composable(
        route = "${AppRoute.MEMORIAL_MAIN.route}/{memorialId}",
        arguments = listOf(navArgument(MEMORIAL_ID_ARG) { type = NavType.StringType }),
    ) { backStackEntry ->
        MemorialMainScreen(
            memorialId = backStackEntry.arguments?.getString(MEMORIAL_ID_ARG).orEmpty(),
            navController = navController,
        )
    }
    composable(
        route = "${AppRoute.PET_MEMORIAL.route}/{memorialId}",
        arguments = listOf(navArgument(MEMORIAL_ID_ARG) { type = NavType.StringType }),
    ) { backStackEntry ->
        MemorialDetailScreen(
            memorialId = backStackEntry.arguments?.getString(MEMORIAL_ID_ARG).orEmpty(),
            navController = navController,
        )
    }
    composable(
        route = "${AppRoute.MEMORIAL_STORY.route}/{memorialId}",
        arguments = listOf(navArgument(MEMORIAL_ID_ARG) { type = NavType.StringType }),
    ) { backStackEntry ->
        MemorialStoryScreen(
            memorialId = backStackEntry.arguments?.getString(MEMORIAL_ID_ARG).orEmpty(),
            navController = navController,
        )
    }
    composable(
        route = "${AppRoute.STORY_ADD.route}/{memorialId}",
        arguments = listOf(navArgument(MEMORIAL_ID_ARG) { type = NavType.StringType }),
    ) { backStackEntry ->
        StoryAddScreen(
            memorialId = backStackEntry.arguments?.getString(MEMORIAL_ID_ARG).orEmpty(),
            navController = navController,
        )
    }
    composable(AppRoute.MEMORIAL_TIME.route) {
        JisiTimeScreen(navController)
    }
    composable(
        route = "${AppRoute.MEMORIAL_DIARY.route}/{memorialId}",
        arguments = listOf(navArgument(MEMORIAL_ID_ARG) { type = NavType.StringType }),
    ) { backStackEntry ->
        MemorialDiaryScreen(
            memorialId = backStackEntry.arguments?.getString(MEMORIAL_ID_ARG).orEmpty(),
            navController = navController,
        )
    }
    composable(
        route = "${AppRoute.LETTER_WRITE.route}/{memorialId}",
        arguments = listOf(navArgument(MEMORIAL_ID_ARG) { type = NavType.StringType }),
    ) { backStackEntry ->
        LetterWriteScreen(
            memorialId = backStackEntry.arguments?.getString(MEMORIAL_ID_ARG).orEmpty(),
            navController = navController,
        )
    }
    composable(
        route = "${AppRoute.LETTER_VIEW.route}/{letterId}",
        arguments = listOf(navArgument(LETTER_ID_ARG) { type = NavType.StringType }),
    ) { backStackEntry ->
        LetterViewScreen(
            letterId = backStackEntry.arguments?.getString(LETTER_ID_ARG).orEmpty(),
            navController = navController,
        )
    }
    composable(AppRoute.AI_ETHICS.route) {
        AiEthicsScreen(navController)
    }
    composable(
        route = "${AppRoute.AI_UPLOAD.route}/{memorialId}",
        arguments = listOf(navArgument(MEMORIAL_ID_ARG) { type = NavType.StringType }),
    ) { backStackEntry ->
        AiUploadScreen(
            memorialId = backStackEntry.arguments?.getString(MEMORIAL_ID_ARG).orEmpty(),
            navController = navController,
        )
    }
    composable(
        route = "${AppRoute.PROXY_MEMORIAL.route}/{memorialId}",
        arguments = listOf(navArgument(MEMORIAL_ID_ARG) { type = NavType.StringType }),
    ) { backStackEntry ->
        DaijiScreen(
            memorialId = backStackEntry.arguments?.getString(MEMORIAL_ID_ARG).orEmpty(),
            navController = navController,
        )
    }
    composable(AppRoute.COLLECTIVE_HISTORY.route) {
        JitiHistoryScreen(navController)
    }
}

private const val MEMORIAL_ID_ARG = "memorialId"
private const val LETTER_ID_ARG = "letterId"
