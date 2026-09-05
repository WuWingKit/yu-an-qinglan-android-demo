/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.LawnSoft
import com.yuanqinglan.app.core.designsystem.OutlineWarm
import com.yuanqinglan.app.core.designsystem.QingLanGreenSoft
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
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
import com.yuanqinglan.app.feature.memorial.data.MemorialRepository
import com.yuanqinglan.app.feature.memorial.data.MemorialServiceLocator
import com.yuanqinglan.app.feature.memorial.model.MemorialIds
import com.yuanqinglan.app.feature.memorial.model.MemorialLetter
import com.yuanqinglan.app.feature.memorial.model.PaperStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val MAX_LETTER_TITLE_LENGTH = 40
private const val MAX_LETTER_BODY_LENGTH = 2000

private fun letterTitleError(value: String): String? = when {
    value.isBlank() -> "请填写信件主题"
    value.length > MAX_LETTER_TITLE_LENGTH -> "主题不能超过 $MAX_LETTER_TITLE_LENGTH 个字"
    else -> null
}

private fun letterBodyError(value: String): String? =
    if (value.length > MAX_LETTER_BODY_LENGTH) "正文不能超过 $MAX_LETTER_BODY_LENGTH 个字" else null

/** 信纸样式的底色（浅色区分，仅本地展示）。 */
private fun letterPaperColor(style: PaperStyle): Color = when (style) {
    PaperStyle.PLAIN -> Color.White
    PaperStyle.GREEN_LINES -> QingLanGreenSoft
    PaperStyle.WARM -> LawnSoft
}

/**
 * 写信页：选择信纸样式、填写主题与正文，保存为本地信件后进入信件查看页。
 * 路由：letter-write/{memorialId}
 */
@Composable
fun LetterWriteScreen(
    memorialId: String,
    navController: NavHostController,
) {
    val context = LocalContext.current
    val repository = remember(context) { MemorialServiceLocator.repository(context) }
    val scope = rememberCoroutineScope()

    var paperToken by rememberSaveable { mutableStateOf(PaperStyle.PLAIN.name) }
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    var savedLetterId by remember { mutableStateOf<String?>(null) }

    val paperStyle = PaperStyle.fromTokenOrNull(paperToken) ?: PaperStyle.PLAIN
    val paperColor = letterPaperColor(paperStyle)
    val titleError = letterTitleError(title)
    val bodyError = letterBodyError(body)
    val canSave = !saving && titleError == null && bodyError == null

    LaunchedEffect(savedLetterId) {
        val letterId = savedLetterId ?: return@LaunchedEffect
        val currentRoute = navController.currentDestination?.route
        navController.navigate(MemorialRoutes.letterView(letterId)) {
            if (currentRoute != null) {
                popUpTo(currentRoute) { inclusive = true }
            }
        }
    }

    AppScaffold(
        title = "写一封信",
        onBack = { navController.popBackStack() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
        ) {
            NoticeBanner(
                text = "本地信件仅保存在本机，供家人查看。",
                tone = NoticeTone.INFO,
            )
            Spacer(Modifier.height(14.dp))

            Text(
                text = "信纸样式",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaperStyle.entries.forEach { style ->
                    PaperStyleOption(
                        style = style,
                        selected = style == paperStyle,
                        onClick = { paperToken = style.name },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            FormTextField(
                label = "主题",
                value = title,
                onValueChange = {
                    if (!saving) {
                        title = it
                        saveFailed = false
                    }
                },
                isError = titleError != null && title.isNotBlank(),
                supportingText = titleError ?: "${title.length}/$MAX_LETTER_TITLE_LENGTH",
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = body,
                onValueChange = {
                    if (!saving) {
                        body = it
                        saveFailed = false
                    }
                },
                label = { Text("想说的话") },
                supportingText = {
                    Text(
                        text = bodyError ?: "可选，${body.length}/$MAX_LETTER_BODY_LENGTH",
                        color = if (bodyError != null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                isError = bodyError != null,
                minLines = 8,
                maxLines = 14,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimensions.CompactRadius),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = paperColor,
                    unfocusedContainerColor = paperColor,
                    disabledContainerColor = paperColor,
                    errorContainerColor = paperColor,
                ),
            )
            Spacer(Modifier.height(18.dp))

            if (saveFailed) {
                NoticeBanner(
                    text = "保存失败，请稍后重试。",
                    tone = NoticeTone.WARNING,
                )
                Spacer(Modifier.height(10.dp))
            }

            PrimaryButton(
                text = if (saving) "保存中…" else "保存信件",
                onClick = {
                    if (!canSave) return@PrimaryButton
                    saving = true
                    saveFailed = false
                    scope.launch {
                        val letter = MemorialLetter(
                            id = MemorialIds.next("ltr"),
                            memorialId = memorialId,
                            title = title.trim(),
                            body = body.trim(),
                            paper = paperStyle,
                            createdAtMillis = System.currentTimeMillis(),
                        )
                        val ok = repository.addLetter(memorialId, letter)
                        saving = false
                        if (ok) {
                            savedLetterId = letter.id
                        } else {
                            saveFailed = true
                        }
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
        }
    }
}

/** 单个信纸样式选择卡：色块预览 + 选中态。 */
@Composable
private fun PaperStyleOption(
    style: PaperStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        color = SurfaceCard,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else OutlineWarm,
        ),
        onClick = onClick,
    ) {
        Box {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(letterPaperColor(style)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = style.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                    )
                }
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "已选",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(16.dp),
                )
            }
        }
    }
}

/** 信件查看页的加载形态（区分就绪/缺失/失败）。 */
private sealed interface LetterUi {
    data object Loading : LetterUi
    data class Ready(val letter: MemorialLetter, val spaceName: String) : LetterUi
    data object Missing : LetterUi
    data class Failed(val message: String) : LetterUi
}

/**
 * 查信件：按 letterId 检索本地信件并渲染其纸面效果；支持删除。
 * 路由：letter-view/{letterId}
 */
@Composable
fun LetterViewScreen(
    letterId: String,
    navController: NavHostController,
) {
    val context = LocalContext.current
    val repository = remember(context) { MemorialServiceLocator.repository(context) }
    var attempt by remember { mutableIntStateOf(0) }

    val ui by produceState<LetterUi>(
        initialValue = LetterUi.Loading,
        key1 = letterId,
        key2 = attempt,
    ) {
        value = try {
            val letter = repository.letterById(letterId)
            if (letter == null) {
                LetterUi.Missing
            } else {
                val spaceName = repository.space(letter.memorialId)?.name
                LetterUi.Ready(letter, spaceName ?: "我的思念")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LetterUi.Failed("信件读取失败，请稍后重试。")
        }
    }

    when (val current = ui) {
        LetterUi.Loading -> AppScaffold(title = "信件", onBack = { navController.popBackStack() }) {
            LoadingState()
        }
        is LetterUi.Failed -> AppScaffold(title = "信件", onBack = { navController.popBackStack() }) {
            ErrorRetry(message = current.message, onRetry = { attempt += 1 })
        }
        LetterUi.Missing -> AppScaffold(title = "信件", onBack = { navController.popBackStack() }) {
            EmptyState(
                title = "没有找到这封信",
                description = "可能已被删除。",
                actionLabel = "返回",
                onAction = { navController.popBackStack() },
            )
        }
        is LetterUi.Ready -> LetterViewContent(
            letter = current.letter,
            spaceName = current.spaceName,
            repository = repository,
            onBack = { navController.popBackStack() },
        )
    }
}

@Composable
private fun LetterViewContent(
    letter: MemorialLetter,
    spaceName: String,
    repository: MemorialRepository,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }
    var deleteFailed by remember { mutableStateOf(false) }
    val paperColor = letterPaperColor(letter.paper)

    AppScaffold(
        title = letter.title.ifBlank { "信件" },
        onBack = onBack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimensions.CardRadius),
                color = paperColor,
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "致 $spaceName",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                    )
                    if (letter.title.isNotBlank()) {
                        Text(
                            text = letter.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                    if (letter.body.isNotBlank()) {
                        Text(
                            text = letter.body,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    Text(
                        text = formatDateTimeText(letter.createdAtMillis),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
            Text(
                text = "信件已保存在本机",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            )
            if (deleteFailed) {
                NoticeBanner(
                    text = "删除失败，请稍后重试。",
                    tone = NoticeTone.WARNING,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            SecondaryButton(
                text = "删除这封信",
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
        }
    }

    if (confirmDelete) {
        ConfirmDangerDialog(
            title = "删除这封信",
            message = "这封信只保存在本机，删除后家人也无法再查看，且无法恢复。",
            confirmLabel = "删除",
            onConfirm = {
                confirmDelete = false
                deleteFailed = false
                scope.launch {
                    val ok = repository.removeLetter(letter.memorialId, letter.id)
                    if (ok) {
                        onBack()
                    } else {
                        deleteFailed = true
                    }
                }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
