/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.profile

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.yuanqinglan.app.feature.profile.ui.AboutScreen
import com.yuanqinglan.app.feature.profile.ui.ElderModeScreen
import com.yuanqinglan.app.feature.profile.ui.FeedbackScreen
import com.yuanqinglan.app.feature.profile.ui.MeScreen
import com.yuanqinglan.app.feature.profile.ui.PasswordEditScreen
import com.yuanqinglan.app.feature.profile.ui.PhoneEditScreen
import com.yuanqinglan.app.feature.profile.ui.PrivacyScreen
import com.yuanqinglan.app.navigation.AppRoute

/**
 * 个人中心 NavGraph 扩展（供主 Agent 在顶层 NavHost 调用）。
 * 注册 profile 的 7 个冻结路由：me、elder、privacy、pwd-edit、phone-edit、about、feedback。
 *
 * me 为 5 Tab 之一（PROFILE 键）；业务子视图（我的订单/素材管理）在 me 目的地内切换，
 * 不额外新增路由键。跨模块跳转（追忆/安葬 Tab）只使用冻结路由字符串与 Tab 语义。
 */
fun NavGraphBuilder.profileNavGraph(navController: NavHostController) {

    /** 一级 Tab 切换：语义与 MainShell 底栏一致（保留各 Tab 回退栈）。 */
    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    composable(AppRoute.PROFILE.route) {
        MeScreen(
            onOpenMemorialTab = { navigateTopLevel(AppRoute.MEMORIAL_HOME.route) },
            onOpenBurialTab = { navigateTopLevel(AppRoute.BURIAL.route) },
            onOpenElder = { navController.navigate(AppRoute.ELDER_MODE.route) },
            onOpenPrivacy = { navController.navigate(AppRoute.PRIVACY.route) },
            onOpenPassword = { navController.navigate(AppRoute.PASSWORD_EDIT.route) },
            onOpenPhone = { navController.navigate(AppRoute.PHONE_EDIT.route) },
            onOpenAbout = { navController.navigate(AppRoute.ABOUT.route) },
            onOpenFeedback = { navController.navigate(AppRoute.FEEDBACK.route) },
        )
    }

    composable(AppRoute.ELDER_MODE.route) {
        ElderModeScreen(onBack = { navController.popBackStack() })
    }

    composable(AppRoute.PRIVACY.route) {
        PrivacyScreen(onBack = { navController.popBackStack() })
    }

    composable(AppRoute.PASSWORD_EDIT.route) {
        PasswordEditScreen(onBack = { navController.popBackStack() })
    }

    composable(AppRoute.PHONE_EDIT.route) {
        PhoneEditScreen(onBack = { navController.popBackStack() })
    }

    composable(AppRoute.ABOUT.route) {
        AboutScreen(onBack = { navController.popBackStack() })
    }

    composable(AppRoute.FEEDBACK.route) {
        FeedbackScreen(onBack = { navController.popBackStack() })
    }
}
