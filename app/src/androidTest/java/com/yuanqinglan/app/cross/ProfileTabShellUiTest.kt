/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.cross

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yuanqinglan.app.core.designsystem.AppBackground
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.YuanQingLanTheme
import com.yuanqinglan.app.data.local.AppContainer
import com.yuanqinglan.app.feature.profile.profileNavGraph
import com.yuanqinglan.app.navigation.AppRoute
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * 跨模块质量 UI 测试（androidTest，设备/模拟器运行）：
 * 复刻主壳的 5-Tab 底栏语义 + profileNavGraph，验证我的 Tab 可达、
 * 子页面可进入与返回；供主 Agent 集成进 MainShell 后作回归基线。
 */
class ProfileTabShellUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun initContainer() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppContainer.init(context)
    }

    private data class Tab(val route: String, val label: String, val icon: ImageVector)

    private val tabs = listOf(
        Tab(AppRoute.HOME.route, "首页", Icons.Outlined.Home),
        Tab(AppRoute.BURIAL.route, "安葬", Icons.Outlined.Forest),
        Tab(AppRoute.MEMORIAL_HOME.route, "追忆", Icons.Outlined.PhotoLibrary),
        Tab(AppRoute.TREEHOLE_SELECT.route, "树洞", Icons.Outlined.MailOutline),
        Tab(AppRoute.PROFILE.route, "我的", Icons.Outlined.PersonOutline),
    )

    @Composable
    private fun FakeTabContent(title: String) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("Tab 内容：$title", modifier = Modifier.padding(16.dp))
        }
    }

    @Composable
    private fun ShellUnderTest() {
        val navController: NavHostController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        Scaffold(
            containerColor = AppBackground,
            bottomBar = {
                NavigationBar(containerColor = SurfaceCard) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { androidx.compose.material3.Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
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
                // 本测试只关心 profileNavGraph 与 Tab 语义集成，
                // 其余 Tab 用占位内容替代（不作为本测试的断言对象）。
                composable(AppRoute.HOME.route) { FakeTabContent("首页") }
                composable(AppRoute.BURIAL.route) { FakeTabContent("安葬") }
                composable(AppRoute.MEMORIAL_HOME.route) { FakeTabContent("追忆") }
                composable(AppRoute.TREEHOLE_SELECT.route) { FakeTabContent("树洞") }
                profileNavGraph(navController)
            }
        }
    }

    @Test
    fun tapProfileTab_showsMeRoot() {
        composeRule.setContent {
            YuanQingLanTheme {
                ShellUnderTest()
            }
        }
        composeRule.onNodeWithText("首页").assertIsDisplayed()
        composeRule.onNodeWithText("我的").performClick()
        composeRule.onNodeWithText("老年模式").assertIsDisplayed()
        composeRule.onNodeWithText("意见反馈").assertIsDisplayed()
        composeRule.onNodeWithText("恢复默认设置").assertIsDisplayed()
    }

    @Test
    fun tabSwitchesAcrossFiveTabs_andProfileSubPageNavigatesBack() {
        composeRule.setContent {
            YuanQingLanTheme {
                ShellUnderTest()
            }
        }

        // 除“我的”外，其余 4 个 Tab 为占位内容
        tabs.filter { it.route != AppRoute.PROFILE.route }.forEach { tab ->
            composeRule.onNodeWithText(tab.label).performClick()
            composeRule.onNodeWithText("Tab 内容：${tab.label}").assertIsDisplayed()
        }

        // 进入个人中心并打开子页（老年模式），返回回到我的根
        composeRule.onNodeWithText("我的").performClick()
        composeRule.onNodeWithText("老年模式").performClick()
        composeRule.onNodeWithText("开启后的变化").assertIsDisplayed()
        // 点击返回图标返回我的根
        composeRule.onNodeWithContentDescription("返回").performClick()
        composeRule.onNodeWithText("意见反馈").assertIsDisplayed()
    }

    @Test
    fun navGraphRegistersAllProfileRoutes() {
        // profileNavGraph 注册即不抛异常（路由重复会在此触发），并保留各路由可达性。
        composeRule.setContent {
            YuanQingLanTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = AppRoute.PROFILE.route,
                ) {
                    profileNavGraph(navController)
                }
            }
        }
        composeRule.onNodeWithText("老年模式").assertIsDisplayed()
        composeRule.onNodeWithText("修改密码").performClick()
        composeRule.onNodeWithText("原密码").assertIsDisplayed()
        // 返回
        composeRule.onNodeWithContentDescription("返回").performClick()
        composeRule.onNodeWithText("账号与隐私").performClick()
        composeRule.onNodeWithText("隐私偏好").assertIsDisplayed()
    }
}
