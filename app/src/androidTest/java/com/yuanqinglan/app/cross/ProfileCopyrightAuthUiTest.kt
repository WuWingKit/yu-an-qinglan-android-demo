/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.cross

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.test.platform.app.InstrumentationRegistry
import com.yuanqinglan.app.core.designsystem.YuanQingLanTheme
import com.yuanqinglan.app.data.local.AppContainer
import com.yuanqinglan.app.feature.profile.profileNavGraph
import com.yuanqinglan.app.navigation.AppRoute
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Issue #20 版权及授权情况：me 页单行低强调入口、详情页完整 License 展示与导航返回、
 * 证书查看器（切页/双语/缩放/平移不崩溃）。通过 profileNavGraph 验证真实路由注册。
 */
class ProfileCopyrightAuthUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun initContainer() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppContainer.init(context)
    }

    @Composable
    private fun ProfileNavHostUnderTest() {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = AppRoute.PROFILE.route,
        ) {
            profileNavGraph(navController)
        }
    }

    private fun openDetailFromMeRoot() {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("版权及授权情况"))
        composeRule.onNodeWithText("版权及授权情况").performClick()
    }

    @Test
    fun meRoot_showsSingleLowEmphasisRow_andNoLicenseLongText() {
        composeRule.setContent {
            YuanQingLanTheme { ProfileNavHostUnderTest() }
        }

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("版权及授权情况"))
        composeRule.onNodeWithText("版权及授权情况").assertIsDisplayed()
        // 整行可点击热区 ≥ 48dp（单行低强调入口）
        composeRule.onNodeWithText("版权及授权情况").assertHeightIsAtLeast(48.dp)

        // me 页不再展示证书缩略图、被授权人姓名、赛事说明与 License 长文
        composeRule.onAllNodesWithText("已授权给西南大学经济管理学院李芸凤").assertCountEquals(0)
        composeRule.onAllNodesWithText("查看软件使用授权书").assertCountEquals(0)
        composeRule.onAllNodesWithText("Copyright ©").assertCountEquals(0)
        composeRule.onAllNodesWithText("仅限 2026年重庆市大学生新文科实践创新大赛非商业用途").assertCountEquals(0)
    }

    @Test
    fun detail_showsLicenseFacts_andBackReturnsToMeRoot() {
        composeRule.setContent {
            YuanQingLanTheme { ProfileNavHostUnderTest() }
        }

        openDetailFromMeRoot()

        // 顶部小节（版权所有者 / 许可边界 / 被授权对象）
        composeRule.onNodeWithText("版权所有者").assertIsDisplayed()
        composeRule.onNodeWithText("许可边界").assertIsDisplayed()
        composeRule.onNodeWithText("被授权对象").assertIsDisplayed()
        composeRule.onNodeWithText("西南大学经济管理学院李芸凤").assertIsDisplayed()

        // 滚动依次确认下部小节与联系邮箱
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("授权用途"))
        composeRule.onNodeWithText("授权用途").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("修改限制"))
        composeRule.onNodeWithText("修改限制").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("有效性与联系"))
        composeRule.onNodeWithText("有效性与联系").assertIsDisplayed()
        composeRule.onNodeWithText("hurongjie@qianban.online", substring = true).assertIsDisplayed()

        // 详情导航返回：回到我的根
        composeRule.onNodeWithContentDescription("返回").performClick()
        composeRule.onNodeWithText("老年模式").assertIsDisplayed()
    }

    @Test
    fun viewer_switchesPages_bilingualLabels_andZoomPanNoCrash() {
        composeRule.setContent {
            YuanQingLanTheme { ProfileNavHostUnderTest() }
        }

        openDetailFromMeRoot()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("查看软件使用授权书"))
        composeRule.onNodeWithText("查看软件使用授权书").performClick()

        // 第 1 页：中文
        composeRule.onNodeWithText("授权书 1 / 2").assertIsDisplayed()
        composeRule.onNodeWithText("中文").assertIsDisplayed()

        // 缩放（双指捏合放大）+ 平移（拖拽）不应崩溃
        composeRule.onNodeWithContentDescription("软件使用授权书中文版")
            .performTouchInput {
                pinch(
                    start0 = center + Offset(-80f, 0f),
                    end0 = center + Offset(-260f, 0f),
                    start1 = center + Offset(80f, 0f),
                    end1 = center + Offset(260f, 0f),
                )
            }
        composeRule.onNodeWithContentDescription("软件使用授权书中文版")
            .performTouchInput { swipe(center, center + Offset(160f, 120f), durationMillis = 300) }

        // 下一页：English 版
        composeRule.onNodeWithContentDescription("下一页").performClick()
        composeRule.onNodeWithText("授权书 2 / 2").assertIsDisplayed()
        composeRule.onNodeWithText("English").assertIsDisplayed()

        // 上一页返回中文页
        composeRule.onNodeWithContentDescription("上一页").performClick()
        composeRule.onNodeWithText("授权书 1 / 2").assertIsDisplayed()
        composeRule.onNodeWithText("中文").assertIsDisplayed()

        // 关闭查看器回到详情页
        composeRule.onNodeWithContentDescription("关闭授权书").performClick()
        composeRule.onNodeWithText("查看软件使用授权书").assertIsDisplayed()
    }
}
