/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.treehole.ui

import android.app.Application
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.LawnSoft
import com.yuanqinglan.app.core.designsystem.QingLanGreenSoft
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.designsystem.currentTouchTargetSize
import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.ConfirmDangerDialog
import com.yuanqinglan.app.core.ui.EmptyState
import com.yuanqinglan.app.core.ui.ErrorRetry
import com.yuanqinglan.app.core.ui.FormTextField
import com.yuanqinglan.app.core.ui.LoadingState
import com.yuanqinglan.app.core.ui.NoticeBanner
import com.yuanqinglan.app.core.ui.NoticeTone
import com.yuanqinglan.app.core.ui.PrimaryButton
import com.yuanqinglan.app.core.ui.SecondaryButton
import com.yuanqinglan.app.feature.treehole.data.TreeholePool
import com.yuanqinglan.app.feature.treehole.model.KindResponse
import com.yuanqinglan.app.feature.treehole.model.TreeholeAttachment
import com.yuanqinglan.app.feature.treehole.model.TreeholeLetterLike
import com.yuanqinglan.app.feature.treehole.model.TreeholePaperStyle

/**
 * 树洞内容池页（人间/生灵共用同一实现，仅入参池不同）：
 * 顶部 [AppScaffold] + 三段模式切换（寄信 / 拾信 / 我的信件）。
 * 人间与生灵分别持有自己的池与 ViewModel，状态互不共享。
 */
@Composable
fun TreeholePoolScreen(
    title: String,
    pool: TreeholePool<out TreeholeLetterLike>,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: TreeholePoolViewModel = viewModel(
        modelClass = TreeholePoolViewModel::class.java,
        factory = remember(pool) { TreeholeViewModelFactory(application, pool) },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val mineLetters by pool.mineLetters.collectAsStateWithLifecycle()

    var lettersReload by remember { mutableIntStateOf(0) }
    val publicLettersFlow = remember(pool, lettersReload) { pool.publicLetters() }
    val lettersState by publicLettersFlow.collectAsStateWithLifecycle(initialValue = DemoState.Loading)

    AppScaffold(
        title = title,
        onBack = onBack,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TreeholeModeSegment(
                current = state.tab,
                onSelect = viewModel::selectTab,
            )
            Spacer(Modifier.height(12.dp))
            state.infoBanner?.let { banner ->
                NoticeBanner(text = banner, tone = NoticeTone.INFO)
                Spacer(Modifier.height(10.dp))
            }
            when (state.tab) {
                TreeholePoolTab.WRITE -> {
                    WriteTabContent(
                        viewModel = viewModel,
                        state = state,
                        categories = pool.availableCategories(),
                    )
                }
                TreeholePoolTab.READ -> {
                    ReadTabContent(
                        viewModel = viewModel,
                        state = state,
                        lettersState = lettersState,
                        reloadLetters = { lettersReload += 1 },
                    )
                }
                TreeholePoolTab.MINE -> {
                    MineTabContent(
                        viewModel = viewModel,
                        letters = mineLetters,
                    )
                }
            }
        }
    }
}

/** 三段模式切换（寄信/拾信/我的信件），选中态主色底纹。 */
@Composable
private fun TreeholeModeSegment(
    current: TreeholePoolTab,
    onSelect: (TreeholePoolTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = listOf(
        TreeholePoolTab.WRITE to "寄信",
        TreeholePoolTab.READ to "拾信",
        TreeholePoolTab.MINE to "我的信件",
    )
    val touchTarget = currentTouchTargetSize()
    val shape = RoundedCornerShape(AppDimensions.CompactRadius)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        entries.forEach { (tab, label) ->
            val isSelected = tab == current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = touchTarget)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        },
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                        onClickLabel = "切换到$label",
                        onClick = { onSelect(tab) },
                    )
                    .semantics { this.selected = isSelected },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

// ---------------------------------------------------------------- 寄信

@Composable
private fun WriteTabContent(
    viewModel: TreeholePoolViewModel,
    state: TreeholePoolUiState,
    categories: List<String>,
) {
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri -> viewModel.importImageFromUri(uri) }
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri -> viewModel.importAudioFromUri(uri) }
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.startRecording()
        } else {
            viewModel.onRecordPermissionDenied()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SectionLabel(text = "信纸样式")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TreeholePaperStyle.entries.forEach { style ->
                PaperStyleCard(
                    style = style,
                    selected = state.paper == style,
                    onClick = { viewModel.selectPaper(style) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel(text = "分类（选择最贴近的一种）")
        Spacer(Modifier.height(8.dp))
        CategoryChips(
            categories = categories,
            selected = state.category,
            onSelect = viewModel::selectCategory,
        )
        state.categoryError?.let { error ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(16.dp))
        FormTextField(
            label = "标题",
            value = state.title,
            onValueChange = viewModel::onTitleChange,
            isError = state.titleError != null,
            supportingText = state.titleError ?: "30 字以内",
        )

        Spacer(Modifier.height(14.dp))
        BodyField(
            value = state.body,
            onValueChange = viewModel::onBodyChange,
            isError = state.bodyError != null,
            supportingText = state.bodyError ?: "正文不超过 600 字",
        )

        Spacer(Modifier.height(18.dp))
        SectionLabel(text = "附件（可选）")
        state.attachmentNote?.let { note ->
            Spacer(Modifier.height(8.dp))
            NoticeBanner(text = note, tone = NoticeTone.WARNING)
        }
        if (state.importing) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "正在导入附件…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionLabel(text = "图片附件（不超过 10MB）", small = true)
        Spacer(Modifier.height(6.dp))
        val image = state.image
        if (image == null) {
            SecondaryButton(
                text = "选择一张图片",
                onClick = { imagePicker.launch("image/*") },
            )
        } else {
            DraftAttachmentRow(
                attachment = image,
                hint = "图片",
                onRemove = viewModel::removeImage,
            )
        }

        Spacer(Modifier.height(14.dp))
        SectionLabel(text = "音频附件（不超过 5MB）", small = true)
        Spacer(Modifier.height(6.dp))
        val audio = state.audio
        if (state.recording) {
            RecordingPanel(
                onStop = viewModel::finishRecordingAndAttach,
                onCancel = viewModel::cancelRecording,
            )
        } else if (audio == null) {
            SecondaryButton(text = "从文件选择音频", onClick = { audioPicker.launch("audio/*") })
            Spacer(Modifier.height(8.dp))
            SecondaryButton(
                text = "录音",
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        viewModel.startRecording()
                    } else {
                        recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
            )
        } else {
            DraftAttachmentRow(
                attachment = audio,
                hint = "音频",
                onRemove = viewModel::removeAudio,
            )
        }

        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = if (state.submitting) "正在寄出…" else "寄出这封信",
            onClick = viewModel::submitLetter,
            enabled = !state.submitting && !state.importing && !state.recording,
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SectionLabel(
    text: String,
    small: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = if (small) {
            MaterialTheme.typography.labelMedium
        } else {
            MaterialTheme.typography.titleMedium
        },
        color = if (small) TextSecondary else TextPrimary,
        modifier = modifier,
    )
}

@Composable
private fun PaperStyleCard(
    style: TreeholePaperStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = when (style) {
        TreeholePaperStyle.PLAIN -> SurfaceCard
        TreeholePaperStyle.GREEN -> QingLanGreenSoft
        TreeholePaperStyle.WARM -> LawnSoft
    }
    val shape = RoundedCornerShape(AppDimensions.CompactRadius)
    val outlineColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    Column(
        modifier = modifier
            .border(BorderStroke(if (selected) 2.dp else 1.dp, outlineColor), shape)
            .clip(shape)
            .background(background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.RadioButton,
                onClickLabel = "选择${style.label}信纸",
                onClick = onClick,
            )
            .semantics { this.selected = selected }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = style.label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else TextPrimary,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(
    categories: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onSelect(category) },
                label = { Text(text = category) },
            )
        }
    }
}

@Composable
private fun BodyField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    supportingText: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(text = "正文") },
        isError = isError,
        supportingText = {
            Text(
                text = supportingText,
                color = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        },
        minLines = 4,
        maxLines = 10,
        shape = RoundedCornerShape(AppDimensions.CompactRadius),
    )
}

@Composable
private fun DraftAttachmentRow(
    attachment: TreeholeAttachment,
    hint: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CompactRadius),
        color = SurfaceCard,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(QingLanGreenSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.name.ifBlank { hint },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatByteSize(attachment.sizeBytes),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
            TextButton(onClick = onRemove) {
                Text(
                    text = "移除",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun RecordingPanel(
    onStop: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        NoticeBanner(
            text = "正在录音…完成后请点“停止并保存”。",
            tone = NoticeTone.INFO,
        )
        Spacer(Modifier.height(8.dp))
        PrimaryButton(text = "停止并保存", onClick = onStop)
        Spacer(Modifier.height(8.dp))
        SecondaryButton(text = "取消录音", onClick = onCancel)
    }
}

// ---------------------------------------------------------------- 拾信

@Composable
private fun ReadTabContent(
    viewModel: TreeholePoolViewModel,
    state: TreeholePoolUiState,
    lettersState: DemoState<List<TreeholeLetterLike>>,
    reloadLetters: () -> Unit,
) {
    val candidates: List<TreeholeLetterLike> =
        (lettersState as? DemoState.Success)?.value ?: emptyList()

    LaunchedEffect(state.tab, lettersState, candidates.isEmpty()) {
        if (state.tab == TreeholePoolTab.READ && candidates.isNotEmpty()) {
            viewModel.ensureCurrentLetter(candidates)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (lettersState) {
            is DemoState.Loading -> LoadingState()
            is DemoState.Error -> ErrorRetry(
                message = lettersState.message,
                onRetry = reloadLetters,
            )
            is DemoState.Empty -> EmptyReadState()
            is DemoState.Success -> {
                if (candidates.isEmpty()) {
                    EmptyReadState()
                } else {
                    val letter = state.currentLetter
                    if (letter == null) {
                        LoadingState()
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                        ) {
                            SecondaryButton(
                                text = "换一封",
                                onClick = { viewModel.changeLetter(candidates) },
                            )
                            Spacer(Modifier.height(12.dp))
                            ReadLetterCard(letter = letter)
                            Spacer(Modifier.height(14.dp))
                            ResponseSection(
                                viewModel = viewModel,
                                letter = letter,
                                candidates = candidates,
                                responseMessage = state.responseMessage,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyReadState(modifier: Modifier = Modifier) {
    EmptyState(
        title = "还没有可拾取的信件",
        description = "新的来信正在路上，稍后再来看看。",
        modifier = modifier,
    )
}

@Composable
private fun ReadLetterCard(letter: TreeholeLetterLike, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        color = SurfaceCard,
    ) {
        Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TreeholeCategoryTag(text = letter.category)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "信纸 · ${letter.paper.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = letter.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            TreeholeLetterBodyText(body = letter.body)
            TreeholeLetterAttachmentBlock(letter = letter)
        }
    }
}

@Composable
private fun ResponseSection(
    viewModel: TreeholePoolViewModel,
    letter: TreeholeLetterLike,
    candidates: List<TreeholeLetterLike>,
    responseMessage: String?,
    modifier: Modifier = Modifier,
) {
    var showReportDialog by remember(letter.id) { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "轻回应",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "送出一份温和的关怀，不留下任何痕迹与计数。",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KindResponseButton(
                kind = KindResponse.LIGHT,
                icon = Icons.Outlined.Lightbulb,
                onClick = { viewModel.respond(KindResponse.LIGHT) },
                modifier = Modifier.weight(1f),
            )
            KindResponseButton(
                kind = KindResponse.LEAF,
                icon = Icons.Outlined.Eco,
                onClick = { viewModel.respond(KindResponse.LEAF) },
                modifier = Modifier.weight(1f),
            )
            KindResponseButton(
                kind = KindResponse.FLOWER,
                icon = Icons.Outlined.LocalFlorist,
                onClick = { viewModel.respond(KindResponse.FLOWER) },
                modifier = Modifier.weight(1f),
            )
        }
        responseMessage?.let { message ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
        Spacer(Modifier.height(6.dp))
        TextButton(
            onClick = { showReportDialog = true },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                text = "举报这封信",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    if (showReportDialog) {
        ConfirmDangerDialog(
            title = "举报这封信？",
            message = "你的反馈仅作为本机记录，不会改动拾信池中的内容。",
            confirmLabel = "确认举报",
            onConfirm = {
                showReportDialog = false
                viewModel.reportCurrentLetter(candidates)
            },
            onDismiss = { showReportDialog = false },
        )
    }
}

@Composable
private fun KindResponseButton(
    kind: KindResponse,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(AppDimensions.CompactRadius))
            .background(SurfaceCard)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = kind.label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = kind.label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
        )
    }
}

// ---------------------------------------------------------------- 我的信件

@Composable
private fun MineTabContent(
    viewModel: TreeholePoolViewModel,
    letters: List<TreeholeLetterLike>,
    modifier: Modifier = Modifier,
) {
    var deleteTarget by remember { mutableStateOf<TreeholeLetterLike?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 2.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            NoticeBanner(
                text = "你寄出的信件会先进入审核，通过后才会与其他来信一同被拾取。",
                tone = NoticeTone.INFO,
            )
        }
        if (letters.isEmpty()) {
            item {
                EmptyState(
                    title = "还没有寄出过信件",
                    description = "在“寄信”里写下一封匿名信，它会先进入审核。",
                )
            }
        } else {
            items(items = letters, key = { it.id }) { letter ->
                MineLetterCard(
                    letter = letter,
                    onDelete = { deleteTarget = letter },
                )
            }
        }
    }

    deleteTarget?.let { target ->
        ConfirmDangerDialog(
            title = "删除这封信？",
            message = "删除后无法恢复；若有附件也会一并移除，仅影响你本机的信件。",
            confirmLabel = "删除",
            onConfirm = {
                deleteTarget = null
                viewModel.deleteLetter(target)
            },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun MineLetterCard(
    letter: TreeholeLetterLike,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        color = SurfaceCard,
    ) {
        Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TreeholeCategoryTag(text = letter.category)
                Spacer(Modifier.weight(1f))
                TreeholeReviewingBadge()
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = letter.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            TreeholeLetterBodyText(body = letter.body, collapsedMaxLines = 3)
            TreeholeLetterAttachmentBlock(letter = letter)
            Spacer(Modifier.height(10.dp))
            SecondaryButton(text = "删除这封信", onClick = onDelete)
        }
    }
}
