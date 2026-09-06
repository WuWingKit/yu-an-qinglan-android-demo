/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.NoticeBanner
import com.yuanqinglan.app.core.ui.NoticeTone
import com.yuanqinglan.app.core.ui.PrimaryButton
import com.yuanqinglan.app.feature.memorial.data.AiFlowGate
import com.yuanqinglan.app.feature.memorial.data.EthicsGateRules
import com.yuanqinglan.app.navigation.AppRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AI 追忆伦理前置页 ViewModel：登记“已阅读并知晓”勾选状态；
 * 用户主动点击「同意并继续」时才授予会话同意并取出待进入的目标空间。
 */
class AiEthicsViewModel : ViewModel() {

    private val _readConfirmed = MutableStateFlow(false)
    val readConfirmed: StateFlow<Boolean> = _readConfirmed.asStateFlow()

    fun setReadConfirmed(confirmed: Boolean) {
        _readConfirmed.value = confirmed
    }

    /**
     * 同意并继续：先授予会话同意，再取出待进入的纪念空间 ID。
     * 不同意/直接返回时本方法不被调用，不改变同意状态。
     */
    fun grantAndConsumeTarget(): String? {
        AiFlowGate.grantConsent()
        return AiFlowGate.consumePending()
    }
}

@Composable
fun AiEthicsScreen(navController: NavHostController) {
    val viewModel: AiEthicsViewModel = viewModel(
        factory = remember { MemorialViewModelFactory { AiEthicsViewModel() } },
    )
    AiEthicsContent(viewModel = viewModel, navController = navController)
}

@Composable
private fun AiEthicsContent(
    viewModel: AiEthicsViewModel,
    navController: NavHostController,
) {
    val readConfirmed by viewModel.readConfirmed.collectAsStateWithLifecycle()

    AppScaffold(
        title = "AI 追忆 · 伦理与授权",
        onBack = { navController.popBackStack() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "开始使用 AI 追忆前，请先阅读以下说明。",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )

            EthicsPrincipleCard(
                icon = Icons.Outlined.UploadFile,
                title = "授权范围",
                body = "只处理你主动上传并授权的私人影像素材，不抓取、不联网使用。",
            )
            EthicsPrincipleCard(
                icon = Icons.Outlined.Lock,
                title = "私人访问",
                body = "生成内容仅本人与授权家人可见，不公开传播。",
            )
            EthicsPrincipleCard(
                icon = Icons.Outlined.Visibility,
                title = "用途透明",
                body = "素材与生成结果仅用于私人追忆展示，不用于其他用途。",
            )
            EthicsPrincipleCard(
                icon = Icons.Outlined.DeleteForever,
                title = "永久销毁",
                body = "你可随时一键永久销毁全部素材与生成内容，销毁后不可恢复。",
            )

            NoticeBanner(
                text = "本应用不提供逝者实时对话或语音互动。",
                tone = NoticeTone.WARNING,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .clickable { viewModel.setReadConfirmed(!readConfirmed) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (readConfirmed) {
                        Icons.Filled.CheckBox
                    } else {
                        Icons.Outlined.CheckBoxOutlineBlank
                    },
                    contentDescription = if (readConfirmed) "已阅读并知晓" else "未勾选",
                    tint = if (readConfirmed) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        TextSecondary
                    },
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "我已阅读并知晓上述全部说明",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                )
            }

            PrimaryButton(
                text = "同意并继续",
                enabled = EthicsGateRules.mayProceed(readConfirmed),
                onClick = {
                    val targetId = viewModel.grantAndConsumeTarget()
                    if (targetId != null) {
                        navController.navigate(MemorialRoutes.aiUpload(targetId)) {
                            popUpTo(AppRoute.AI_ETHICS.route) { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
            )
            Text(
                text = "同意仅本次会话有效；下次进入 AI 追忆前需重新确认。不同意时直接返回即可，不会改变你的授权状态。",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun EthicsPrincipleCard(
    icon: ImageVector,
    title: String,
    body: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        color = SurfaceCard,
    ) {
        Row(modifier = Modifier.padding(AppDimensions.CardPadding)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
    }
}
