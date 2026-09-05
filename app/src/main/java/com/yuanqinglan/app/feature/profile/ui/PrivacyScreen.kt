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
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Mic
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
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.InfoRow
import com.yuanqinglan.app.core.ui.NoticeBanner
import com.yuanqinglan.app.core.ui.NoticeTone
import com.yuanqinglan.app.core.ui.PrimaryButton
import com.yuanqinglan.app.core.ui.ReferenceNote
import com.yuanqinglan.app.data.local.AppContainer
import com.yuanqinglan.app.data.local.SettingsRepository
import com.yuanqinglan.app.feature.profile.data.DataStoreProfileLocalStore
import com.yuanqinglan.app.feature.profile.data.ProfileLocalStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 账号与隐私页 ViewModel：隐私开关持久化（DataStore）与 privacyAccepted 状态。 */
class PrivacyViewModel(
    private val settings: SettingsRepository,
    private val localStore: ProfileLocalStore,
) : ViewModel() {

    val allowBrowsePrefs: StateFlow<Boolean> = localStore.allowBrowsePrefs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), false,
    )
    val treeholeAnonymous: StateFlow<Boolean> = localStore.treeholeAnonymous.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), true,
    )
    val privacyAccepted: StateFlow<Boolean> = settings.privacyAccepted.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), false,
    )

    fun setAllowBrowsePrefs(enabled: Boolean) {
        viewModelScope.launch { localStore.setAllowBrowsePrefs(enabled) }
    }

    fun setTreeholeAnonymous(enabled: Boolean) {
        viewModelScope.launch { localStore.setTreeholeAnonymous(enabled) }
    }

    fun setPrivacyAccepted(accepted: Boolean) {
        viewModelScope.launch { settings.setPrivacyAccepted(accepted) }
    }
}

/** 账号与隐私页。 */
@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    vm: PrivacyViewModel? = null,
) {
    val context = LocalContext.current
    val settings = remember { AppContainer.settings }
    val localStore = remember(context) { DataStoreProfileLocalStore(context) }
    val effectiveViewModel: PrivacyViewModel = vm ?: viewModel(
        factory = remember {
            ProfileViewModelFactory { PrivacyViewModel(settings, localStore) }
        },
    )

    val allowBrowsePrefs by effectiveViewModel.allowBrowsePrefs.collectAsStateWithLifecycle()
    val treeholeAnonymous by effectiveViewModel.treeholeAnonymous.collectAsStateWithLifecycle()
    val privacyAccepted by effectiveViewModel.privacyAccepted.collectAsStateWithLifecycle()

    AppScaffold(title = "账号与隐私", onBack = onBack) {
        Column {
            Spacer(Modifier.size(10.dp))

            ProfileGroupTitle("隐私偏好")
            ProfileGroupCard {
                PrivacySwitchRow(
                    title = "允许保存浏览偏好",
                    description = "在本机保存浏览与使用偏好，便于下次访问",
                    checked = allowBrowsePrefs,
                    onCheckedChange = effectiveViewModel::setAllowBrowsePrefs,
                )
                ProfileDivider()
                PrivacySwitchRow(
                    title = "树洞匿名展示",
                    description = "树洞内容以匿名方式展示",
                    checked = treeholeAnonymous,
                    onCheckedChange = effectiveViewModel::setTreeholeAnonymous,
                )
            }

            ProfileGroupTitle("隐私确认")
            ProfileGroupCard {
                InfoRow(label = "隐私声明确认", value = if (privacyAccepted) "已确认" else "未确认")
                if (!privacyAccepted) {
                    Spacer(Modifier.size(6.dp))
                    NoticeBanner(
                        text = "确认即表示已阅读并同意隐私相关说明（本机记录，不对外传输）。",
                        tone = NoticeTone.COMPLIANCE,
                    )
                    Spacer(Modifier.size(10.dp))
                    PrimaryButton(
                        text = "确认隐私声明",
                        onClick = { effectiveViewModel.setPrivacyAccepted(true) },
                    )
                }
            }

            ProfileGroupTitle("权限说明")
            ProfileGroupCard {
                PermissionRow(Icons.Outlined.PhotoCamera, "相机", "仅用于本机拍摄素材与录音，不对外传输")
                ProfileDivider()
                PermissionRow(Icons.Outlined.PhotoLibrary, "相册", "仅用于选择头像与素材图片，内容保存在本机私有目录")
                ProfileDivider()
                PermissionRow(Icons.Outlined.Mic, "麦克风", "仅用于本机录音，不对外传输")
            }

            Spacer(Modifier.size(10.dp))
            NoticeBanner(
                text = "相机、相册与麦克风权限仅用于本地选择与录音，相关内容不对外传输。",
                tone = NoticeTone.COMPLIANCE,
            )
            Spacer(Modifier.size(10.dp))
            ReferenceNote(text = "所有隐私开关均保存在本机。")
            ProfileBottomSpace()
        }
    }
}

@Composable
private fun PrivacySwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimensions.CardPadding, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(vertical = 0.dp),
        )
    }
}

@Composable
private fun PermissionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimensions.CardPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
        }
    }
}
