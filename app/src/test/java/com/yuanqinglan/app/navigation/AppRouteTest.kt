/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRouteTest {
    @Test
    fun routeKeysMatchAuditedDemo() {
        val routes = AppRoute.entries.map(AppRoute::route)

        // 45 个 v11 路由（Issue 1 冻结）+ App 2.0 新增版权及授权详情路由
        // （Issue #20：copyright-authorization）。
        assertEquals(46, routes.size)
        assertEquals(routes.size, routes.toSet().size)
        assertTrue("copyright-authorization" in routes)
    }

    @Test
    fun topLevelDestinationsUseStableRoutes() {
        val appRoutes = AppRoute.entries.toSet()

        assertEquals(5, TopLevelDestination.entries.size)
        assertTrue(TopLevelDestination.entries.all { it.route in appRoutes })
    }
}
