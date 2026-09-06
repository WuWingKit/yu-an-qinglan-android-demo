/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.profile.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.QingLanGreen
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.NoticeBanner
import com.yuanqinglan.app.core.ui.NoticeTone
import com.yuanqinglan.app.core.ui.ReferenceNote
import com.yuanqinglan.app.data.local.AppContainer
import com.yuanqinglan.app.data.local.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 老年模式设置页 ViewModel：与公共设置仓库同源（首页右上角开关共用）。 */
class ElderModeViewModel(
    private val settings: SettingsRepository,
) : ViewModel() {

    val elderMode: StateFlow<Boolean> = settings.elderMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), false,
    )

    fun setElderMode(enabled: Boolean) {
        viewModelScope.launch { settings.setElderMode(enabled) }
    }
}

/** 老年模式页。 */
@Composable
fun ElderModeScreen(
    onBack: () -> Unit,
    vm: ElderModeViewModel? = null,
) {
    val context = LocalContext.current
    val settings = remember { AppContainer.settings }
    val effectiveViewModel: ElderModeViewModel = vm ?: viewModel(
        factory = remember {
            ProfileViewModelFactory { ElderModeViewModel(settings) }
        },
    )
    val elderMode by effectiveViewModel.elderMode.collectAsStateWithLifecycle()

    AppScaffold(title = "老年模式", onBack = onBack) {
        Column {
            Spacer(Modifier.size(12.dp))
            NoticeBanner(
                text = if (elderMode) {
                    "老年模式已开启，界面已放大字号并提高对比度。"
                } else {
                    "开启后字体更大、点击区域更宽、对比度更高，方便长辈使用。"
                },
                tone = if (elderMode) NoticeTone.INFO else NoticeTone.COMPLIANCE,
            )

            Spacer(Modifier.size(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "老年模式",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Text(
                        text = if (elderMode) "已开启" else "已关闭",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
                Switch(checked = elderMode, onCheckedChange = effectiveViewModel::setElderMode)
            }

            Spacer(Modifier.size(16.dp))
            Text(
                text = "开启后的变化",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            Spacer(Modifier.size(8.dp))
            ElderEffectRow("全局字号约放大 1.25 倍，阅读更清晰")
            ElderEffectRow("按钮与点击区域更大，减少误触")
            ElderEffectRow("文字与背景对比度增强")
            ElderEffectRow("弱化次要动效，减少视觉干扰")

            Spacer(Modifier.size(16.dp))
            ReferenceNote(
                text = "开关状态与本机设置同步保存，首页右上角开关与本页共用同一状态。",
            )
            ProfileBottomSpace()
        }
    }
}

@Composable
private fun ElderEffectRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = QingLanGreen,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.weight(1f),
        )
    }
}
