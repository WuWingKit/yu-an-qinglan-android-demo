/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val route: AppRoute,
    val label: String,
    val icon: ImageVector,
) {
    HOME(AppRoute.HOME, "首页", Icons.Outlined.Home),
    BURIAL(AppRoute.BURIAL, "安葬", Icons.Outlined.Forest),
    MEMORIAL(AppRoute.MEMORIAL_HOME, "追忆", Icons.Outlined.PhotoLibrary),
    TREEHOLE(AppRoute.TREEHOLE_SELECT, "树洞", Icons.Outlined.MailOutline),
    PROFILE(AppRoute.PROFILE, "我的", Icons.Outlined.PersonOutline),
}
