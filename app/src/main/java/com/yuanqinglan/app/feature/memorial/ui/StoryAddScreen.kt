/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.FormTextField
import com.yuanqinglan.app.core.ui.NoticeBanner
import com.yuanqinglan.app.core.ui.NoticeTone
import com.yuanqinglan.app.core.ui.PrimaryButton
import com.yuanqinglan.app.core.ui.SecondaryButton
import com.yuanqinglan.app.data.local.AppContainer
import com.yuanqinglan.app.feature.memorial.data.MemorialRepository
import com.yuanqinglan.app.feature.memorial.data.MemorialServiceLocator
import com.yuanqinglan.app.feature.memorial.model.MediaKind
import com.yuanqinglan.app.feature.memorial.model.MediaRef
import com.yuanqinglan.app.feature.memorial.model.MemorialIds
import com.yuanqinglan.app.feature.memorial.model.MemorialStory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.launch

/** 无日期哨兵值（与真实毫秒时间戳不可能冲突）。 */
private const val NO_DATE = Long.MIN_VALUE

private const val MAX_STORY_TITLE_LENGTH = 40
private const val MAX_STORY_BODY_LENGTH = 1000

private fun storyTitleError(value: String): String? = when {
    value.isBlank() -> "请填写节点标题"
    value.length > MAX_STORY_TITLE_LENGTH -> "标题不能超过 $MAX_STORY_TITLE_LENGTH 个字"
    else -> null
}

private fun storyBodyError(value: String): String? =
    if (value.length > MAX_STORY_BODY_LENGTH) "正文不能超过 $MAX_STORY_BODY_LENGTH 个字" else null

/**
 * 新增故事节点：标题/日期/正文/可选单图；校验通过后落库，成功后返回时间线。
 * 路由：story-add/{memorialId}
 */
@Composable
fun StoryAddScreen(
    memorialId: String,
    navController: NavHostController,
) {
    val context = LocalContext.current
    val repository = remember(context) { MemorialServiceLocator.repository(context) }
    val scope = rememberCoroutineScope()

    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var selectedDateMillis by rememberSaveable { mutableStateOf(NO_DATE) }
    var pickedImage by remember { mutableStateOf<MediaRef?>(null) }
    var formTouched by rememberSaveable { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf(false) }

    val titleError = storyTitleError(title)
    val bodyError = storyBodyError(body)
    val pickedDate: LocalDate? = if (selectedDateMillis == NO_DATE) {
        null
    } else {
        Instant.ofEpochMilli(selectedDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    val dateError = if (pickedDate == null && formTouched) "请选择事件发生的日期" else null
    val canSave = !saving && titleError == null && pickedDate != null && bodyError == null

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null || saving) return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = readUriBytes(context, uri)
            if (bytes == null || bytes.isEmpty()) {
                imageError = true
                return@launch
            }
            imageError = false
            val saved = AppContainer.fileStorage.saveImage(bytes)
            pickedImage = MediaRef(
                id = MemorialIds.next("st"),
                kind = MediaKind.IMAGE_FILE,
                value = saved.toString(),
                name = uri.lastPathSegment ?: "照片",
                sizeBytes = bytes.size.toLong(),
            )
        }
    }

    fun resetForm() {
        title = ""
        body = ""
        selectedDateMillis = NO_DATE
        pickedImage = null
        formTouched = false
        saveFailed = false
        imageError = false
    }

    fun saveStory() {
        val date = pickedDate ?: return
        if (titleError != null || bodyError != null || saving) return
        saving = true
        saveFailed = false
        scope.launch {
            val story = MemorialStory(
                id = MemorialIds.next("story"),
                title = title.trim(),
                dateMillis = localDateToMillis(date),
                dateText = dateTextOf(date),
                body = body.trim(),
                image = pickedImage,
            )
            val ok = repository.addStory(memorialId, story)
            saving = false
            if (ok) {
                navController.popBackStack()
            } else {
                saveFailed = true
            }
        }
    }

    AppScaffold(
        title = "新增故事节点",
        onBack = { navController.popBackStack() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
        ) {
            if (saveFailed) {
                NoticeBanner(
                    text = "保存失败，请稍后重试。",
                    tone = NoticeTone.WARNING,
                )
                Spacer(Modifier.height(10.dp))
            }
            FormTextField(
                label = "节点标题",
                value = title,
                onValueChange = {
                    if (!saving) {
                        title = it
                        formTouched = true
                    }
                },
                isError = titleError != null && (formTouched || title.isNotBlank()),
                supportingText = titleError
                    ?: "${title.length}/$MAX_STORY_TITLE_LENGTH",
            )
            Spacer(Modifier.height(10.dp))

            Text(
                text = "事件日期",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            Spacer(Modifier.height(4.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !saving) { showDatePicker = true },
                shape = RoundedCornerShape(AppDimensions.CompactRadius),
                color = SurfaceCard,
                border = BorderStroke(
                    1.dp,
                    if (dateError != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = pickedDate?.let { dateTextOf(it) } ?: "请选择日期",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (pickedDate != null) TextPrimary else TextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (dateError != null) {
                Text(
                    text = dateError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                )
            }
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = body,
                onValueChange = {
                    if (!saving) {
                        body = it
                        formTouched = true
                    }
                },
                label = { Text("正文") },
                supportingText = {
                    Text(
                        text = bodyError ?: "可选，${body.length}/$MAX_STORY_BODY_LENGTH",
                        color = if (bodyError != null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                isError = bodyError != null,
                minLines = 5,
                maxLines = 10,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimensions.CompactRadius),
            )
            Spacer(Modifier.height(10.dp))

            if (imageError) {
                NoticeBanner(
                    text = "读取照片失败，请重新选择。",
                    tone = NoticeTone.WARNING,
                )
                Spacer(Modifier.height(10.dp))
            }

            Text(
                text = "配图（可选）",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            Spacer(Modifier.height(6.dp))
            val image = pickedImage
            if (image == null) {
                SecondaryButton(
                    text = "从相册选择一张照片",
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(AppDimensions.CompactRadius))
                        .clickable(enabled = !saving) { imagePicker.launch("image/*") },
                ) {
                    MediaThumb(
                        ref = image,
                        contentDescription = "已选故事配图",
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(30.dp)
                            .background(Color(0x99000000), CircleShape)
                            .clickable {
                                pickedImage = null
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "移除配图",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Text(
                    text = "点击图片可更换",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(
                    text = "重新填写",
                    onClick = { if (!saving) resetForm() },
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = if (saving) "保存中…" else "保存节点",
                    onClick = { saveStory() },
                    enabled = canSave,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(4.dp))
        }
    }

    if (showDatePicker) {
        MemorialDatePickerDialog(
            title = "选择节点日期",
            initialDate = pickedDate,
            onConfirm = { date ->
                selectedDateMillis = localDateToMillis(date)
                formTouched = true
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}
