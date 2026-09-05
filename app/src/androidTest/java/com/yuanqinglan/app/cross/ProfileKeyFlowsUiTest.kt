/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.cross

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yuanqinglan.app.core.designsystem.LocalElderMode
import com.yuanqinglan.app.core.designsystem.ProvideElderMode
import com.yuanqinglan.app.core.designsystem.YuanQingLanTheme
import com.yuanqinglan.app.data.local.AppContainer
import com.yuanqinglan.app.feature.profile.ui.ElderModeScreen
import com.yuanqinglan.app.feature.profile.ui.ElderModeViewModel
import com.yuanqinglan.app.feature.profile.ui.MaterialsSection
import com.yuanqinglan.app.feature.profile.ui.MaterialViewModel
import com.yuanqinglan.app.feature.profile.ui.MeScreen
import com.yuanqinglan.app.feature.profile.ui.MeViewModel
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * 跨模块质量 UI 测试（androidTest，设备/模拟器运行）：
 * - 老年模式开关通过公共设置仓库全局生效（复刻 App 根部的 ProvideElderMode 接线）；
 * - 个人中心关键链路：昵称边界、恢复默认设置二次确认、素材管理空态。
 */
class ProfileKeyFlowsUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun initContainer() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppContainer.init(context)
    }

    private val toggleableMatcher: SemanticsMatcher =
        androidx.compose.ui.test.isToggleable()

    @Test
    fun elderSwitch_togglesSharedSetting_andGlobalProbeReflects() {
        val settings = FakeSettingsRepository()
        val vm = ElderModeViewModel(settings)

        composeRule.setContent {
            // 根接线 + 老年模式页（页面在 ProvideElderMode 内渲染，与真实 App 一致）
            val elderMode by settings.elderMode.collectAsStateWithLifecycle()
            ProvideElderMode(enabled = elderMode) {
                YuanQingLanTheme {
                    Box {
                        Text(
                            text = if (LocalElderMode.current) "探针：老年模式已生效" else "探针：普通模式",
                            modifier = Modifier.testTag("elder_probe"),
                        )
                        ElderModeScreen(onBack = {}, vm = vm)
                    }
                }
            }
        }

        composeRule.onNodeWithText("开启后的变化").assertIsDisplayed()
        composeRule.onNodeWithTag("elder_probe").assertTextEquals("探针：普通模式")

        // 打开开关 → 写公共设置仓库，全局探针同步翻转
        composeRule.onNode(toggleableMatcher).performClick()

        composeRule.waitUntil(timeoutMillis = 3_000) {
            settings.elderMode.value
        }
        composeRule.onNodeWithTag("elder_probe").assertTextEquals("探针：老年模式已生效")
        composeRule.onNodeWithText("老年模式已开启，界面已放大字号并提高对比度。").assertIsDisplayed()
    }

    @Test
    fun nicknameDialog_rejects13Chars_andSavesShortName() {
        val settings = FakeSettingsRepository()
        val vm = MeViewModel(settings, FakeMediaImporter())

        composeRule.setContent {
            YuanQingLanTheme {
                MeScreen(
                    onOpenMemorialTab = {},
                    onOpenBurialTab = {},
                    onOpenElder = {},
                    onOpenPrivacy = {},
                    onOpenPassword = {},
                    onOpenPhone = {},
                    onOpenAbout = {},
                    onOpenFeedback = {},
                    vm = vm,
                )
            }
        }

        composeRule.onNodeWithText("点击头像更换，点击昵称可编辑（1-12 个字）").performClick()
        composeRule.onNodeWithText("修改昵称").assertIsDisplayed()

        // 13 个字 → 校验失败
        composeRule.onNode(hasSetTextAction()).performTextClearance()
        composeRule.onNode(hasSetTextAction()).performTextInput("一二三四五六七八九十甲乙丙")
        composeRule.onNodeWithText("保存").performClick()
        composeRule.onNodeWithText("昵称最多 12 个字").assertIsDisplayed()

        // 改为合法昵称并保存
        composeRule.onNode(hasSetTextAction()).performTextClearance()
        composeRule.onNode(hasSetTextAction()).performTextInput("王阿姨")
        composeRule.onNodeWithText("保存").performClick()

        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText("王阿姨").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun resetAll_requiresConfirmation_thenRestoresDefaults() {
        val settings = FakeSettingsRepository()
        val vm = MeViewModel(settings, FakeMediaImporter())

        // 先改一个非默认昵称，便于断言复位
        composeRule.runOnUiThread {
            settings.nickname.value = "测试用户"
        }

        composeRule.setContent {
            YuanQingLanTheme {
                MeScreen(
                    onOpenMemorialTab = {},
                    onOpenBurialTab = {},
                    onOpenElder = {},
                    onOpenPrivacy = {},
                    onOpenPassword = {},
                    onOpenPhone = {},
                    onOpenAbout = {},
                    onOpenFeedback = {},
                    vm = vm,
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText("测试用户").fetchSemanticsNodes().isNotEmpty()
        }

        // 第一次点恢复默认：确认框出现，点取消 → 未复位
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("恢复默认设置"))
        composeRule.onNodeWithText("恢复默认设置").performClick()
        composeRule.onNodeWithText("确认恢复").assertIsDisplayed()
        composeRule.onNodeWithText("取消").performClick()
        // 取消后列表仍停在底部，需滚回顶部昵称处断言未复位
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("测试用户"))
        composeRule.onNodeWithText("测试用户").assertIsDisplayed()

        // 第二次确认 → 复位为默认昵称（复位为异步；先滚回顶部昵称区再等待）
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("恢复默认设置"))
        composeRule.onNodeWithText("恢复默认设置").performClick()
        composeRule.onNodeWithText("确认恢复").performClick()
        composeRule.onNode(hasScrollAction()).performScrollToIndex(0)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("渝安青澜用户").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun materialsPage_showsEmptyStateWithAddActions() {
        val localStore = FakeProfileLocalStore()
        val vm = MaterialViewModel(localStore, FakeMediaImporter())

        composeRule.setContent {
            YuanQingLanTheme {
                MaterialsSection(onBack = {}, vm = vm)
            }
        }

        composeRule.onNodeWithText("素材管理").assertIsDisplayed()
        composeRule.onNodeWithText("暂无素材").assertIsDisplayed()
        composeRule.onNodeWithText("添加图片").assertIsDisplayed()
        composeRule.onNodeWithText("添加音频").assertIsDisplayed()
    }
}
