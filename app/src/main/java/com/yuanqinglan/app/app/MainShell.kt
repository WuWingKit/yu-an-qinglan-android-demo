/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yuanqinglan.app.core.designsystem.AppBackground
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.data.local.AppContainer
import com.yuanqinglan.app.feature.burial.burialNavGraph
import com.yuanqinglan.app.feature.home.homeNavGraph
import com.yuanqinglan.app.feature.memorial.memorialNavGraph
import com.yuanqinglan.app.feature.policy.policyNavGraph
import com.yuanqinglan.app.feature.profile.profileNavGraph
import com.yuanqinglan.app.feature.treehole.treeholeNavGraph
import com.yuanqinglan.app.navigation.AppRoute
import com.yuanqinglan.app.navigation.TopLevelDestination

/**
 * 主外壳：Scaffold + 底部 5 Tab + NavHost。
 * Tab 各自保留回退栈（saveState / restoreState / launchSingleTop）。
 *
 * NavHost 集成（扩展函数契约由主 Agent 冻结，各 feature 位于自身目录内实现）：
 * 五个一级 Tab 根分别由 home / burial / memorial / treehole / profile 的 NavGraph
 * 扩展注册；树洞总开关关闭时，树洞 Tab 显示"已关闭"状态且内容不可达。
 */
@Composable
fun MainShell() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val treeholeEnabled by AppContainer.settings.treeholeEnabled
        .collectAsStateWithLifecycle(initialValue = true)

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
            memorialNavGraph(navController)
            profileNavGraph(navController)

            // 树洞总开关联动（SettingsRepository.treeholeEnabled）：
            // 关闭时仅注册"已关闭"占位路由，双内容池路由不可达。
            if (treeholeEnabled) {
                treeholeNavGraph(navController)
            } else {
                composable(AppRoute.TREEHOLE_SELECT.route) {
                    TreeholeDisabledScreen(
                        onGoSettings = {
                            navController.navigate(AppRoute.PROFILE.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        }
    }
}

/**
 * 树洞总开关关闭时的占位页：说明功能已关闭，并提供前往设置的入口。
 */
@Composable
private fun TreeholeDisabledScreen(onGoSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(AppDimensions.PageHorizontal),
    ) {
        Spacer(Modifier.height(18.dp))
        Text("心灵树洞", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "心灵树洞当前已关闭。",
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppDimensions.CardPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "如需使用，可前往「我的」中的树洞设置重新开启。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    onClick = onGoSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppDimensions.MinimumTouchTarget),
                ) {
                    Text("前往设置开启")
                }
            }
        }
    }
}
