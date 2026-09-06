/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.treehole

import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.yuanqinglan.app.feature.treehole.data.TreeholeServiceLocator
import com.yuanqinglan.app.feature.treehole.model.TreeholePoolType
import com.yuanqinglan.app.feature.treehole.ui.TreeholePoolScreen
import com.yuanqinglan.app.feature.treehole.ui.TreeholeSelectScreen
import com.yuanqinglan.app.navigation.AppRoute

/**
 * 树洞模块三条路由注册扩展（供顶层 NavHost 调用）：
 *
 * - shudong-select：树洞入口 Tab 根页（游客确认门在此页）；
 * - shudong-ren / shudong-sheng：人间/生灵两个独立内容池页，
 *   共用 [TreeholePoolScreen] 实现，仅池实例与标题不同。
 */
fun NavGraphBuilder.treeholeNavGraph(navController: NavHostController) {
    composable(AppRoute.TREEHOLE_SELECT.route) {
        TreeholeSelectScreen(navController = navController)
    }

    composable(AppRoute.TREEHOLE_HUMAN.route) {
        val context = LocalContext.current
        val repository = remember { TreeholeServiceLocator.repository(context) }
        TreeholePoolScreen(
            title = TreeholePoolType.HUMAN_POOL.poolLabel,
            pool = repository.humanPool,
            onBack = { navController.popBackStack() },
        )
    }

    composable(AppRoute.TREEHOLE_PET.route) {
        val context = LocalContext.current
        val repository = remember { TreeholeServiceLocator.repository(context) }
        TreeholePoolScreen(
            title = TreeholePoolType.PET_POOL.poolLabel,
            pool = repository.petPool,
            onBack = { navController.popBackStack() },
        )
    }
}
