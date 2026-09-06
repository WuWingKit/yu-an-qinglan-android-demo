/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.profile.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yuanqinglan.app.core.designsystem.AppBackground
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.ConfirmDangerDialog
import com.yuanqinglan.app.core.ui.EmptyState
import com.yuanqinglan.app.core.ui.FormTextField
import com.yuanqinglan.app.core.ui.NoticeBanner
import com.yuanqinglan.app.core.ui.NoticeTone
import com.yuanqinglan.app.core.ui.ReferenceNote
import com.yuanqinglan.app.data.local.AppContainer
import com.yuanqinglan.app.data.local.SettingsRepository
import com.yuanqinglan.app.feature.profile.data.ProfileFileHandler
import com.yuanqinglan.app.feature.profile.data.ProfileMediaImporter
import com.yuanqinglan.app.feature.profile.logic.ProfileRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** me 页内子视图（不新增路由键，仅在 me 目的地内切换）。 */
enum class MeSection { MAIN, ORDERS, MATERIALS }

/** me 页 ViewModel：昵称/头像/树洞开关 + 恢复默认设置。 */
class MeViewModel(
    private val settings: SettingsRepository,
    private val mediaImporter: ProfileMediaImporter,
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val nickname: StateFlow<String> = settings.nickname.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), "",
    )
    val avatarUri: StateFlow<String?> = settings.avatarUri.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), null,
    )
    val treeholeEnabled: StateFlow<Boolean> = settings.treeholeEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), true,
    )

    /** 保存昵称：本地校验通过才写仓库，返回是否成功（失败原因写入 message）。 */
    fun saveNickname(raw: String): Boolean {
        val error = ProfileRules.nicknameError(raw)
        if (error != null) {
            _message.value = error
            return false
        }
        val value = raw.trim()
        viewModelScope.launch {
            settings.setNickname(value)
            _message.value = "昵称已更新"
        }
        return true
    }

    /** 更换头像：先把系统相册图拷入私有目录，再持久化 file uri。 */
    fun importAvatar(sourceUri: String) {
        viewModelScope.launch {
            val imported = mediaImporter.importImageToPrivate(sourceUri)
            if (imported == null) {
                _message.value = "头像导入失败，请重试"
                return@launch
            }
            val old = settings.avatarUri.first()
            settings.setAvatarUri(imported)
            if (old != null && old != imported) {
                mediaImporter.deletePrivateFile(old)
            }
            _message.value = "头像已更新"
        }
    }

    fun setTreeholeEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setTreeholeEnabled(enabled) }
    }

    /** 恢复默认设置（二次确认在界面完成）。 */
    fun resetAll() {
        viewModelScope.launch {
            val oldAvatar = settings.avatarUri.first()
            settings.resetAll()
            if (oldAvatar != null) mediaImporter.deletePrivateFile(oldAvatar)
            _message.value = "已恢复默认设置"
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}

/**
 * me 根路由（我的 Tab）。业务入口行：
 * - 我的纪念空间 → 顶层切换到追忆 Tab（由 NavHost 传回调）；
 * - 我的订单 / 素材管理 → me 内部子视图（ORDERS / MATERIALS）。
 */
@Composable
fun MeScreen(
    onOpenMemorialTab: () -> Unit,
    onOpenBurialTab: () -> Unit,
    onOpenElder: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenPassword: () -> Unit,
    onOpenPhone: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenFeedback: () -> Unit,
    vm: MeViewModel? = null,
) {
    val context = LocalContext.current
    val settings = remember { AppContainer.settings }
    val mediaImporter = remember(context) { ProfileFileHandler(context) }
    val effectiveViewModel: MeViewModel = vm ?: viewModel(
        factory = remember {
            ProfileViewModelFactory { MeViewModel(settings, mediaImporter) }
        },
    )

    var section by rememberSaveable { mutableStateOf(MeSection.MAIN) }
    BackHandler(enabled = section != MeSection.MAIN) { section = MeSection.MAIN }

    when (section) {
        MeSection.MAIN -> MeMainPage(
            viewModel = effectiveViewModel,
            onOpenOrders = { section = MeSection.ORDERS },
            onOpenMemorial = onOpenMemorialTab,
            onOpenMaterials = { section = MeSection.MATERIALS },
            onOpenElder = onOpenElder,
            onOpenPrivacy = onOpenPrivacy,
            onOpenPassword = onOpenPassword,
            onOpenPhone = onOpenPhone,
            onOpenAbout = onOpenAbout,
            onOpenFeedback = onOpenFeedback,
        )
        MeSection.ORDERS -> OrdersEntryView(
            onBack = { section = MeSection.MAIN },
            onOpenBurial = onOpenBurialTab,
        )
        MeSection.MATERIALS -> MaterialsSection(
            onBack = { section = MeSection.MAIN },
        )
    }
}

@Composable
private fun MeMainPage(
    viewModel: MeViewModel,
    onOpenOrders: () -> Unit,
    onOpenMemorial: () -> Unit,
    onOpenMaterials: () -> Unit,
    onOpenElder: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenPassword: () -> Unit,
    onOpenPhone: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenFeedback: () -> Unit,
) {
    val nickname by viewModel.nickname.collectAsStateWithLifecycle()
    val avatarUri by viewModel.avatarUri.collectAsStateWithLifecycle()
    val treeholeEnabled by viewModel.treeholeEnabled.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    var showNicknameDialog by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { viewModel.importAvatar(it.toString()) }
        },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppDimensions.PageHorizontal),
        ) {
            item {
                Spacer(Modifier.size(10.dp))
                Text("我的", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Spacer(Modifier.size(4.dp))
                Text(
                    text = "个人资料与账号相关设置仅保存在本机",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }

            item {
                Spacer(Modifier.size(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProfileAvatar(
                        uriString = avatarUri,
                        size = 72,
                        modifier = Modifier.padding(end = 14.dp),
                        onClick = {
                            avatarPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showNicknameDialog = true },
                    ) {
                        Text(
                            text = nickname.ifBlank { "渝安青澜用户" },
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
                            text = "点击头像更换，点击昵称可编辑（1-12 个字）",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                    }
                }
            }

            item { ProfileGroupTitle("服务") }
            item {
                ProfileGroupCard {
                    ProfileSettingRow(
                        icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                        title = "我的订单",
                        description = "本机预约订单进度入口",
                        onClick = onOpenOrders,
                    )
                    ProfileDivider()
                    ProfileSettingRow(
                        icon = Icons.Outlined.FavoriteBorder,
                        title = "我的纪念空间",
                        description = "前往云端追忆",
                        onClick = onOpenMemorial,
                    )
                    ProfileDivider()
                    ProfileSettingRow(
                        icon = Icons.Outlined.PhotoLibrary,
                        title = "素材管理",
                        description = "本地图片/音频素材列表与销毁",
                        onClick = onOpenMaterials,
                    )
                }
            }

            item { ProfileGroupTitle("树洞") }
            item {
                ProfileGroupCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppDimensions.CardPadding, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MailOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "心灵树洞总开关",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                            )
                            Text(
                                text = if (treeholeEnabled) "已开启，树洞内容可访问" else "已关闭，树洞入口不可用",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                            )
                        }
                        Switch(
                            checked = treeholeEnabled,
                            onCheckedChange = viewModel::setTreeholeEnabled,
                        )
                    }
                }
            }

            item { ProfileGroupTitle("系统") }
            item {
                ProfileGroupCard {
                    ProfileSettingRow(
                        icon = Icons.Outlined.AccessibilityNew,
                        title = "老年模式",
                        description = "字号放大、对比度增强、减少动效",
                        onClick = onOpenElder,
                    )
                    ProfileDivider()
                    ProfileSettingRow(
                        icon = Icons.Outlined.Lock,
                        title = "账号与隐私",
                        description = "隐私开关、权限说明与隐私确认",
                        onClick = onOpenPrivacy,
                    )
                    ProfileDivider()
                    ProfileSettingRow(
                        icon = Icons.Outlined.VpnKey,
                        title = "修改密码",
                        description = "本地密码校验与更新",
                        onClick = onOpenPassword,
                    )
                    ProfileDivider()
                    ProfileSettingRow(
                        icon = Icons.Outlined.Smartphone,
                        title = "更换手机号",
                        description = "本机预留手机号更新",
                        onClick = onOpenPhone,
                    )
                    ProfileDivider()
                    ProfileSettingRow(
                        icon = Icons.Outlined.Info,
                        title = "关于渝安青澜",
                        onClick = onOpenAbout,
                    )
                    ProfileDivider()
                    ProfileSettingRow(
                        icon = Icons.Outlined.RateReview,
                        title = "意见反馈",
                        description = "提交类型、内容与本地记录",
                        onClick = onOpenFeedback,
                    )
                }
            }

            item { ProfileGroupTitle("数据") }
            item {
                ProfileGroupCard {
                    ProfileSettingRow(
                        icon = Icons.Outlined.Restore,
                        title = "恢复默认设置",
                        description = "昵称、头像、开关与隐私确认恢复初始状态",
                        onClick = { showResetDialog = true },
                    )
                }
            }

            item {
                Spacer(Modifier.size(12.dp))
                if (message != null) {
                    NoticeBanner(text = message.orEmpty(), tone = NoticeTone.INFO)
                    Spacer(Modifier.size(10.dp))
                }
                ReferenceNote(
                    text = "个人资料与本地内容仅保存在本机私有目录，不对外传输。相关信息仅供参考，具体以主管机构和服务机构公布为准。",
                )
                AuthorizationCopyrightFooter()
                Spacer(Modifier.size(20.dp))
            }
        }
    }

    if (showNicknameDialog) {
        NicknameDialog(
            initial = nickname,
            onDismiss = { showNicknameDialog = false },
            onSave = { candidate ->
                if (viewModel.saveNickname(candidate)) {
                    showNicknameDialog = false
                }
            },
        )
    }

    if (showResetDialog) {
        ConfirmDangerDialog(
            title = "恢复默认设置",
            message = "将恢复昵称、头像、老年模式、树洞开关与隐私确认为初始状态，此操作不可撤销。本地素材与反馈记录不受影响。",
            confirmLabel = "确认恢复",
            onConfirm = {
                showResetDialog = false
                viewModel.resetAll()
            },
            onDismiss = { showResetDialog = false },
        )
    }
}

@Composable
private fun NicknameDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by rememberSaveable(initial) { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改昵称") },
        text = {
            FormTextField(
                label = "昵称",
                value = value,
                onValueChange = {
                    value = it
                    error = null
                },
                isError = error != null,
                supportingText = error ?: "1-12 个字，仅保存在本机",
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val issue = ProfileRules.nicknameError(value)
                if (issue == null) {
                    onSave(value.trim())
                } else {
                    error = issue
                }
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

/** 订单入口子视图：说明本地订单由安葬模块记录，提供前往安葬 Tab 的入口。 */
@Composable
fun OrdersEntryView(
    onBack: () -> Unit,
    onOpenBurial: () -> Unit,
) {
    AppScaffold(title = "我的订单", onBack = onBack) {
        Column {
            Spacer(Modifier.size(12.dp))
            NoticeBanner(
                text = "安葬预约订单与进度由安葬模块在本机记录，个人中心提供统一入口。",
                tone = NoticeTone.COMPLIANCE,
            )
            EmptyState(
                title = "暂无本机订单",
                description = "前往安葬服务完成预约后，可在此查看本机订单摘要。",
                actionLabel = "前往安葬服务",
                onAction = onOpenBurial,
            )
            Spacer(Modifier.size(10.dp))
            ReferenceNote(text = "订单记录保存在安葬模块本机，具体进度以安葬模块页面为准。")
            ProfileBottomSpace()
        }
    }
}
