/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yuanqinglan.app.core.designsystem.AppBackground
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.navigation.AppRoute
import com.yuanqinglan.app.navigation.TopLevelDestination
import com.yuanqinglan.app.feature.burial.burialNavGraph
import com.yuanqinglan.app.feature.home.homeNavGraph
import com.yuanqinglan.app.feature.policy.policyNavGraph

/**
 * 主外壳：Scaffold + 底部 5 Tab + NavHost。
 * Tab 各自保留回退栈（saveState / restoreState / launchSingleTop）。
 *
 * NavHost 集成（扩展函数契约由主 Agent 冻结，各 feature 位于自身目录内实现）：
 * - 第一批（本次接入）：home/policy/burial
 * - 第二批（待接入，当前保留占位路由，5 个 Tab 根路由始终可到达）：
 *   TODO(foundation): memorial/treehole/profile 的 NavGraph 扩展落地后，
 *   以 memorialNavGraph / treeholeNavGraph / profileNavGraph 替换下方三个 composable 占位。
 */
@Composable
fun MainShell() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceCard,
                modifier = Modifier.navigationBarsPadding(),
            ) {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route.route,
                        onClick = {
                            navController.navigate(destination.route.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                            )
                        },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.HOME.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            homeNavGraph(navController)
            policyNavGraph(navController)
            burialNavGraph(navController)

            // TODO(foundation): 第二批 feature 接入点（见上方 KDoc），落地前以占位保证 5 Tab 可达。
            composable(AppRoute.MEMORIAL_HOME.route) {
                ModulePlaceholder("云端追忆", "保存生命故事与私人纪念空间")
            }
            composable(AppRoute.TREEHOLE_SELECT.route) {
                ModulePlaceholder("心灵树洞", "人间与生灵内容池相互隔离")
            }
            composable(AppRoute.PROFILE.route) {
                ModulePlaceholder("我的", "管理设置、隐私与适老模式")
            }
        }
    }
}

/**
 * 第二批模块占位页：仅用于尚未接入的 Tab 根路由，保证 5 个一级 Tab 可到达。
 * feature 落地后由对应 NavGraph 扩展替换。
 */
@Composable
private fun ModulePlaceholder(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(AppDimensions.PageHorizontal),
    ) {
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 6.dp),
        )
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(AppDimensions.CardRadius),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 20.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "模块内容正在完善中，敬请期待。",
                    modifier = Modifier.padding(AppDimensions.CardPadding),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
