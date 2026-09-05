/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.yuanqinglan.app.core.designsystem.YuanQingLanTheme
import com.yuanqinglan.app.core.model.AudienceTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 公共组件基础冒烟测试（Compose UI，androidTest）。
 * 覆盖：NoticeBanner 文案与语气、AudienceSegment 切换回调、
 * EmptyState 动作、ConfirmDangerDialog 确认/取消、FormTextField 输入。
 */
class CoreComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(content: @Composable () -> Unit) {
        composeRule.setContent {
            YuanQingLanTheme {
                content()
            }
        }
    }

    @Test
    fun noticeBanner_rendersTextAcrossTones() {
        setContent {
            NoticeBanner(text = "相关信息仅供参考", tone = NoticeTone.INFO)
            NoticeBanner(text = "政策以主管机构公布为准", tone = NoticeTone.COMPLIANCE)
            NoticeBanner(text = "请谨慎核对办理材料", tone = NoticeTone.WARNING)
        }
        composeRule.onNodeWithText("相关信息仅供参考").assertIsDisplayed()
        composeRule.onNodeWithText("政策以主管机构公布为准").assertIsDisplayed()
        composeRule.onNodeWithText("请谨慎核对办理材料").assertIsDisplayed()
    }

    @Test
    fun audienceSegment_reportsHumanAndPetSelection() {
        var selected by mutableStateOf(AudienceTrack.HUMAN)
        setContent {
            AudienceSegment(selected = selected, onSelect = { selected = it })
        }
        composeRule.onNodeWithText("人类").assertIsDisplayed()
        composeRule.onNodeWithText("宠物").performClick()
        assertEquals(AudienceTrack.PET, selected)
        composeRule.onNodeWithText("人类").performClick()
        assertEquals(AudienceTrack.HUMAN, selected)
    }

    @Test
    fun emptyState_invokesActionWhenAvailable() {
        var clicked = false
        setContent {
            EmptyState(
                title = "暂无内容",
                description = "稍后再来看看",
                actionLabel = "去看看",
                onAction = { clicked = true },
            )
        }
        composeRule.onNodeWithText("暂无内容").assertIsDisplayed()
        composeRule.onNodeWithText("去看看").performClick()
        assertTrue(clicked)
    }

    @Test
    fun confirmDangerDialog_confirmAndDismissCallbacks() {
        var confirmed = false
        var dismissed = false
        setContent {
            ConfirmDangerDialog(
                title = "确认删除",
                message = "删除后无法恢复。",
                confirmLabel = "删除",
                onConfirm = { confirmed = true },
                onDismiss = { dismissed = true },
            )
        }
        composeRule.onNodeWithText("确认删除").assertIsDisplayed()
        composeRule.onNodeWithText("取消").performClick()
        assertTrue(dismissed)
        assertFalse(confirmed)

        dismissed = false
        setContent {
            ConfirmDangerDialog(
                title = "确认删除",
                message = "删除后无法恢复。",
                confirmLabel = "删除",
                onConfirm = { confirmed = true },
                onDismiss = { dismissed = true },
            )
        }
        composeRule.onNodeWithText("删除").performClick()
        assertTrue(confirmed)
    }

    @Test
    fun formTextField_updatesValueOnInput() {
        var value by mutableStateOf("")
        setContent {
            FormTextField(
                label = "昵称",
                value = value,
                onValueChange = { value = it },
            )
        }
        composeRule.onNodeWithText("昵称").assertIsDisplayed()
        composeRule.onNode(hasSetTextAction()).performTextInput("王阿姨")
        assertEquals("王阿姨", value)
    }
}
