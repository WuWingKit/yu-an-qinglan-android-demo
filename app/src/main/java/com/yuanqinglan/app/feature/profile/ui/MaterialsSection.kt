/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.profile.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.designsystem.Warning
import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.ConfirmDangerDialog
import com.yuanqinglan.app.core.ui.EmptyState
import com.yuanqinglan.app.core.ui.ErrorRetry
import com.yuanqinglan.app.core.ui.LoadingState
import com.yuanqinglan.app.core.ui.NoticeBanner
import com.yuanqinglan.app.core.ui.NoticeTone
import com.yuanqinglan.app.core.ui.PrimaryButton
import com.yuanqinglan.app.core.ui.ReferenceNote
import com.yuanqinglan.app.feature.profile.data.DataStoreProfileLocalStore
import com.yuanqinglan.app.feature.profile.data.ProfileFileHandler
import com.yuanqinglan.app.feature.profile.data.ProfileLocalStore
import com.yuanqinglan.app.feature.profile.data.ProfileMediaImporter
import com.yuanqinglan.app.feature.profile.model.LocalMaterialEntry
import com.yuanqinglan.app.feature.profile.model.ProfileMaterialKind
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 素材管理 ViewModel：列表加载/添加/永久删除。 */
class MaterialViewModel(
    private val localStore: ProfileLocalStore,
    private val mediaImporter: ProfileMediaImporter,
) : ViewModel() {

    private val _state = MutableStateFlow<DemoState<List<LocalMaterialEntry>>>(DemoState.Loading)
    val state: StateFlow<DemoState<List<LocalMaterialEntry>>> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var observeJob: Job? = null

    init {
        reload()
    }

    /** 重新订阅素材流（错误重试入口，可重复调用）。 */
    fun reload() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _state.value = DemoState.Loading
            localStore.materials.collect { list ->
                _state.value = if (list.isEmpty()) DemoState.Empty else DemoState.Success(list)
            }
        }
    }

    /** 从系统相册导入一张图片到私有目录并登记。 */
    fun importImage(sourceUri: String) {
        viewModelScope.launch {
            val saved = mediaImporter.importImageToPrivate(sourceUri)
            if (saved == null) {
                _message.value = "图片导入失败，请重试"
                return@launch
            }
            localStore.addMaterial(
                LocalMaterialEntry(
                    id = UUID.randomUUID().toString(),
                    kind = ProfileMaterialKind.IMAGE,
                    uri = saved,
                    name = "本地图片 ${System.currentTimeMillis() % 10000}",
                    createdAtMillis = System.currentTimeMillis(),
                ),
            )
            _message.value = "图片已添加"
        }
    }

    /** 从系统选择导入一段音频到私有目录并登记。 */
    fun importAudio(sourceUri: String) {
        viewModelScope.launch {
            val saved = mediaImporter.importAudioToPrivate(sourceUri)
            if (saved == null) {
                _message.value = "音频导入失败，请重试"
                return@launch
            }
            localStore.addMaterial(
                LocalMaterialEntry(
                    id = UUID.randomUUID().toString(),
                    kind = ProfileMaterialKind.AUDIO,
                    uri = saved,
                    name = "本地录音 ${System.currentTimeMillis() % 10000}",
                    createdAtMillis = System.currentTimeMillis(),
                ),
            )
            _message.value = "音频已添加"
        }
    }

    /** 永久删除素材（索引 + 私有文件）。 */
    fun deleteMaterial(entry: LocalMaterialEntry) {
        viewModelScope.launch {
            localStore.removeMaterial(entry.id)
            mediaImporter.deletePrivateFile(entry.uri)
            _message.value = "已永久删除"
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}

/** 素材管理子视图（me 内部页）。 */
@Composable
fun MaterialsSection(
    onBack: () -> Unit,
    vm: MaterialViewModel? = null,
) {
    val context = LocalContext.current
    val localStore = remember(context) { DataStoreProfileLocalStore(context) }
    val mediaImporter = remember(context) { ProfileFileHandler(context) }
    val effectiveViewModel: MaterialViewModel = vm ?: viewModel(
        factory = remember {
            ProfileViewModelFactory { MaterialViewModel(localStore, mediaImporter) }
        },
    )

    val state by effectiveViewModel.state.collectAsStateWithLifecycle()
    val message by effectiveViewModel.message.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<LocalMaterialEntry?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let { effectiveViewModel.importImage(it.toString()) } },
    )
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> uri?.let { effectiveViewModel.importAudio(it.toString()) } },
    )

    AppScaffold(title = "素材管理", onBack = onBack) {
        Column {
            Spacer(Modifier.size(12.dp))
            NoticeBanner(
                text = "本地素材仅保存在应用私有目录，不对外传输；删除为永久销毁操作。",
                tone = NoticeTone.WARNING,
            )
            Spacer(Modifier.size(12.dp))

            when (val current = state) {
                DemoState.Loading -> Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) { LoadingState() }
                is DemoState.Error -> Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) { ErrorRetry(message = current.message, onRetry = effectiveViewModel::reload) }
                DemoState.Empty -> Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        title = "暂无素材",
                        description = "从本机相册或文件中选择图片、音频，将复制到应用私有目录统一管理。",
                    )
                }
                is DemoState.Success -> LazyColumn(
                    modifier = Modifier.weight(1f),
                ) {
                    items(current.value, key = { it.id }) { entry ->
                        MaterialRow(
                            entry = entry,
                            onDelete = { pendingDelete = entry },
                        )
                    }
                }
            }

            if (message != null) {
                Spacer(Modifier.size(8.dp))
                NoticeBanner(text = message.orEmpty(), tone = NoticeTone.INFO)
            }

            Spacer(Modifier.size(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                PrimaryButton(
                    text = "添加图片",
                    onClick = {
                        imagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                )
                PrimaryButton(
                    text = "添加音频",
                    onClick = { audioPicker.launch("audio/*") },
                    modifier = Modifier.weight(1f).padding(start = 6.dp),
                )
            }
            Spacer(Modifier.size(10.dp))
            ReferenceNote(text = "素材与生成内容均只保存在本机，可按需永久删除。")
            ProfileBottomSpace()
        }
    }

    if (pendingDelete != null) {
        ConfirmDangerDialog(
            title = "永久删除素材",
            message = "删除后无法恢复，应用私有目录中的对应文件也会一并销毁。",
            confirmLabel = "永久删除",
            onConfirm = {
                effectiveViewModel.deleteMaterial(pendingDelete!!)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun MaterialRow(
    entry: LocalMaterialEntry,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (entry.kind == ProfileMaterialKind.IMAGE) {
                Icons.Outlined.Image
            } else {
                Icons.Outlined.AudioFile
            },
            contentDescription = if (entry.kind == ProfileMaterialKind.IMAGE) "图片素材" else "音频素材",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(30.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (entry.kind == ProfileMaterialKind.IMAGE) "图片" else "音频",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = "删除素材",
                tint = Warning,
            )
        }
    }
}
