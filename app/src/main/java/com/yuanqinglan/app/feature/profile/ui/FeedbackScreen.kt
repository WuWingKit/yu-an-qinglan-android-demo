/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.profile.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.NoticeBanner
import com.yuanqinglan.app.core.ui.NoticeTone
import com.yuanqinglan.app.core.ui.PrimaryButton
import com.yuanqinglan.app.core.ui.ReferenceNote
import com.yuanqinglan.app.feature.profile.data.DataStoreProfileLocalStore
import com.yuanqinglan.app.feature.profile.data.ProfileFileHandler
import com.yuanqinglan.app.feature.profile.data.ProfileLocalStore
import com.yuanqinglan.app.feature.profile.data.ProfileMediaImporter
import com.yuanqinglan.app.feature.profile.logic.ProfileRules
import com.yuanqinglan.app.feature.profile.model.FeedbackRecord
import com.yuanqinglan.app.feature.profile.model.FeedbackType
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 意见反馈页 ViewModel：类型/正文/附件本地校验，提交写入本机记录。 */
class FeedbackViewModel(
    private val localStore: ProfileLocalStore,
    private val mediaImporter: ProfileMediaImporter,
) : ViewModel() {

    private val _type = MutableStateFlow(FeedbackType.FUNCTION)
    val type: StateFlow<FeedbackType> = _type.asStateFlow()

    private val _body = MutableStateFlow("")
    val body: StateFlow<String> = _body.asStateFlow()

    /** 已复制到私有目录的附件 file uri。 */
    private val _attachments = MutableStateFlow<List<String>>(emptyList())
    val attachments: StateFlow<List<String>> = _attachments.asStateFlow()

    private val _bodyError = MutableStateFlow<String?>(null)
    val bodyError: StateFlow<String?> = _bodyError.asStateFlow()

    private val _attachmentError = MutableStateFlow<String?>(null)
    val attachmentError: StateFlow<String?> = _attachmentError.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    /** 已收到结果状态：提交成功后置 true，可再次填写。 */
    private val _received = MutableStateFlow(false)
    val received: StateFlow<Boolean> = _received.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun selectType(value: FeedbackType) {
        _type.value = value
    }

    fun onBodyChange(value: String) {
        _body.value = value.take(ProfileRules.FEEDBACK_BODY_MAX)
        _bodyError.value = null
    }

    fun attachImages(sourceUris: List<String>) {
        val remaining = ProfileRules.FEEDBACK_ATTACHMENT_MAX - _attachments.value.size
        if (remaining <= 0) {
            _attachmentError.value = "最多添加 ${ProfileRules.FEEDBACK_ATTACHMENT_MAX} 张图片"
            return
        }
        viewModelScope.launch {
            val toImport = sourceUris.take(remaining)
            val imported = mutableListOf<String>()
            for (uri in toImport) {
                val saved = mediaImporter.importImageToPrivate(uri)
                if (saved != null) imported.add(saved)
            }
            if (imported.isNotEmpty()) {
                _attachments.value = (_attachments.value + imported).take(
                    ProfileRules.FEEDBACK_ATTACHMENT_MAX,
                )
                _attachmentError.value = null
            } else {
                _attachmentError.value = "图片添加失败，请重试"
            }
        }
    }

    fun removeAttachment(index: Int) {
        val removed = _attachments.value.getOrNull(index) ?: return
        _attachments.value = _attachments.value.filterIndexed { i, _ -> i != index }
        viewModelScope.launch { mediaImporter.deletePrivateFile(removed) }
    }

    fun submit() {
        if (_submitting.value) return
        val issue = ProfileRules.feedbackBodyError(_body.value)
        if (issue != null) {
            _bodyError.value = issue
            return
        }
        viewModelScope.launch {
            _submitting.value = true
            localStore.addFeedback(
                FeedbackRecord(
                    id = UUID.randomUUID().toString(),
                    typeLabel = _type.value.label,
                    body = _body.value.trim(),
                    attachmentUris = _attachments.value,
                    submittedAtMillis = System.currentTimeMillis(),
                ),
            )
            _submitting.value = false
            _received.value = true
            _message.value = "已收到您的反馈"
        }
    }

    /** 再次填写：清空表单与结果状态。 */
    fun resetAndFillAgain() {
        _type.value = FeedbackType.FUNCTION
        _body.value = ""
        _bodyError.value = null
        _attachmentError.value = null
        val old = _attachments.value
        _attachments.value = emptyList()
        viewModelScope.launch { old.forEach { mediaImporter.deletePrivateFile(it) } }
        _received.value = false
    }
}

/** 意见反馈页。 */
@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
    vm: FeedbackViewModel? = null,
) {
    val context = LocalContext.current
    val localStore = remember(context) { DataStoreProfileLocalStore(context) }
    val mediaImporter = remember(context) { ProfileFileHandler(context) }
    val effectiveViewModel: FeedbackViewModel = vm ?: viewModel(
        factory = remember {
            ProfileViewModelFactory { FeedbackViewModel(localStore, mediaImporter) }
        },
    )

    val type by effectiveViewModel.type.collectAsStateWithLifecycle()
    val body by effectiveViewModel.body.collectAsStateWithLifecycle()
    val attachments by effectiveViewModel.attachments.collectAsStateWithLifecycle()
    val bodyError by effectiveViewModel.bodyError.collectAsStateWithLifecycle()
    val attachmentError by effectiveViewModel.attachmentError.collectAsStateWithLifecycle()
    val submitting by effectiveViewModel.submitting.collectAsStateWithLifecycle()
    val received by effectiveViewModel.received.collectAsStateWithLifecycle()
    val message by effectiveViewModel.message.collectAsStateWithLifecycle()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = ProfileRules.FEEDBACK_ATTACHMENT_MAX,
        ),
        onResult = { uris ->
            effectiveViewModel.attachImages(uris.map { it.toString() })
        },
    )

    if (received) {
        ReceivedResultView(
            onFillAgain = effectiveViewModel::resetAndFillAgain,
            onBack = onBack,
        )
        return
    }

    AppScaffold(title = "意见反馈", onBack = onBack) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
        ) {
            Spacer(Modifier.size(10.dp))
            NoticeBanner(
                text = "反馈内容与附件仅保存在本机，用于后续整理；不会自动对外提交。",
                tone = NoticeTone.COMPLIANCE,
            )

            Spacer(Modifier.size(12.dp))
            Text("反馈类型", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.size(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FeedbackType.entries.forEach { item ->
                    FilterChip(
                        selected = type == item,
                        onClick = { effectiveViewModel.selectType(item) },
                        label = { Text(item.label) },
                    )
                }
            }

            Spacer(Modifier.size(12.dp))
            OutlinedTextField(
                value = body,
                onValueChange = effectiveViewModel::onBodyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("反馈内容") },
                supportingText = {
                    Text(
                        text = bodyError ?: "请输入 1-500 字的反馈内容（必填）",
                        color = if (bodyError != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                },
                isError = bodyError != null,
                minLines = 4,
                maxLines = 6,
            )

            Spacer(Modifier.size(12.dp))
            Text("图片附件（可选，最多 3 张）", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.size(6.dp))
            if (attachmentError != null) {
                NoticeBanner(text = attachmentError.orEmpty(), tone = NoticeTone.WARNING)
                Spacer(Modifier.size(6.dp))
            }
            attachments.forEachIndexed { index, uri ->
                AttachmentRow(
                    uri = uri,
                    onRemove = { effectiveViewModel.removeAttachment(index) },
                )
            }
            if (attachments.size < ProfileRules.FEEDBACK_ATTACHMENT_MAX) {
                Spacer(Modifier.size(6.dp))
                PrimaryButton(
                    text = "选择图片",
                    onClick = {
                        imagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                )
            }

            Spacer(Modifier.size(14.dp))
            PrimaryButton(
                text = if (submitting) "提交中…" else "提交反馈",
                onClick = effectiveViewModel::submit,
                enabled = !submitting,
            )
            Spacer(Modifier.size(10.dp))
            ReferenceNote(text = "意见与建议仅保存在本机，我们会定期整理并改进服务。")
            ProfileBottomSpace()
        }
    }
}

/** 附件行：缩略图或图标 + 移除按钮。 */
@Composable
private fun AttachmentRow(
    uri: String,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val bitmap = rememberDecodedImage(uri)
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap,
                contentDescription = "反馈图片附件",
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 10.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(48.dp).padding(end = 10.dp),
            )
        }
        Text(
            text = "附件图片",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "移除附件",
                tint = Warning,
            )
        }
    }
}

/** “已收到”结果页（本地状态，可再次提交）。 */
@Composable
private fun ReceivedResultView(
    onFillAgain: () -> Unit,
    onBack: () -> Unit,
) {
    AppScaffold(title = "意见反馈", onBack = onBack) {
        Column {
            Spacer(Modifier.size(24.dp))
            NoticeBanner(
                text = "已收到您的反馈，感谢您的时间与建议。",
                tone = NoticeTone.INFO,
            )
            Spacer(Modifier.size(14.dp))
            PrimaryButton(text = "再写一条", onClick = onFillAgain)
            Spacer(Modifier.size(10.dp))
            ReferenceNote(text = "反馈记录仅保存在本机。")
            ProfileBottomSpace()
        }
    }
}
