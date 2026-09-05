/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
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
import com.yuanqinglan.app.data.local.AppContainer
import com.yuanqinglan.app.feature.memorial.data.AiFlowGate
import com.yuanqinglan.app.feature.memorial.data.MemorialRepository
import com.yuanqinglan.app.feature.memorial.data.MemorialServiceLocator
import com.yuanqinglan.app.feature.memorial.model.AlbumSelect
import com.yuanqinglan.app.feature.memorial.model.MediaKind
import com.yuanqinglan.app.feature.memorial.model.MediaRef
import com.yuanqinglan.app.feature.memorial.model.MemorialFormRules
import com.yuanqinglan.app.feature.memorial.model.MemorialIds
import com.yuanqinglan.app.feature.memorial.model.MemorialLike
import com.yuanqinglan.app.feature.memorial.model.MemorialMessage
import com.yuanqinglan.app.feature.memorial.model.MemorialTrack
import com.yuanqinglan.app.navigation.AppRoute
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 纪念空间详情页内 Tab。 */
private enum class DetailTab(val label: String) {
    HOME("主页"),
    GALLERY("相册"),
    MESSAGES("寄语"),
    AI("AI 追忆"),
    VISIT("祭扫延伸"),
}

/**
 * 纪念空间详情 ViewModel：按 memorialId 观察对应轨空间（只读公共形态）。
 * 人/宠由 ID 前缀路由到各自独立存储，绝不跨轨。
 */
class MemorialDetailViewModel(
    private val repository: MemorialRepository,
    val memorialId: String,
) : ViewModel() {

    private val _state = MutableStateFlow<DemoState<MemorialLike>>(DemoState.Loading)
    val state: StateFlow<DemoState<MemorialLike>> = _state.asStateFlow()

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    private var collectJob: kotlinx.coroutines.Job? = null

    init {
        startCollect()
    }

    private fun startCollect() {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            repository.observeSpace(memorialId).collect { _state.value = it }
        }
    }

    fun refresh() {
        startCollect()
    }

    override fun onCleared() {
        collectJob?.cancel()
        super.onCleared()
    }

    fun addMessage(text: String) {
        val content = text.trim()
        if (content.isEmpty()) return
        viewModelScope.launch {
            val message = MemorialMessage(
                id = MemorialIds.next("msg"),
                author = "我",
                text = content,
                createdAtMillis = System.currentTimeMillis(),
            )
            repository.addMessage(memorialId, message)
        }
    }

    fun addPhotos(refs: List<MediaRef>) {
        viewModelScope.launch {
            refs.forEach { repository.addGalleryMedia(memorialId, it) }
        }
    }

    fun deletePhotos(refIds: Set<String>) {
        viewModelScope.launch {
            repository.removeGalleryMediaBatch(memorialId, refIds)
        }
    }

    fun updateMeta(name: String, relation: String, intro: String) {
        viewModelScope.launch {
            when (MemorialTrack.ofId(memorialId)) {
                MemorialTrack.HUMAN -> repository.updateHumanMeta(memorialId, name, relation, intro)
                MemorialTrack.PET -> repository.updatePetMeta(memorialId, name, relation, intro)
            }
        }
    }

    fun deleteSpace() {
        viewModelScope.launch {
            val ok = when (MemorialTrack.ofId(memorialId)) {
                MemorialTrack.HUMAN -> repository.deleteHuman(memorialId)
                MemorialTrack.PET -> repository.deletePet(memorialId)
            }
            if (ok) _deleted.value = true
        }
    }
}

@Composable
fun MemorialDetailScreen(
    memorialId: String,
    navController: NavHostController,
) {
    val context = LocalContext.current
    val repository = remember(context) { MemorialServiceLocator.repository(context) }
    val viewModel: MemorialDetailViewModel = viewModel(
        factory = remember(repository, memorialId) {
            MemorialViewModelFactory { MemorialDetailViewModel(repository, memorialId) }
        },
    )
    MemorialDetailContent(viewModel = viewModel, navController = navController)
}

@Composable
private fun MemorialDetailContent(
    viewModel: MemorialDetailViewModel,
    navController: NavHostController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()

    LaunchedEffect(deleted) {
        if (deleted) navController.popBackStack()
    }

    when (val current = state) {
        DemoState.Loading -> AppScaffold(title = "纪念空间", onBack = { navController.popBackStack() }) {
            LoadingState()
        }
        is DemoState.Error -> AppScaffold(title = "纪念空间", onBack = { navController.popBackStack() }) {
            ErrorRetry(message = current.message, onRetry = { viewModel.refresh() })
        }
        DemoState.Empty -> AppScaffold(title = "纪念空间", onBack = { navController.popBackStack() }) {
            EmptyState(title = "纪念空间不存在", description = "可能已被删除", actionLabel = "返回", onAction = { navController.popBackStack() })
        }
        is DemoState.Success -> {
            val space = current.value
            val vocab = MemorialVocab.ofMemorialId(space.id)
            var tabIndex by rememberSaveable(space.id) { mutableIntStateOf(0) }
            var showEdit by remember { mutableStateOf(false) }
            var showDelete by remember { mutableStateOf(false) }

            AppScaffold(
                title = space.name,
                onBack = { navController.popBackStack() },
                actions = {
                    androidx.compose.material3.IconButton(onClick = { showDelete = true }) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "删除纪念空间",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TabRow(selectedTabIndex = tabIndex) {
                        DetailTab.entries.forEachIndexed { index, tab ->
                            Tab(
                                selected = tabIndex == index,
                                onClick = { tabIndex = index },
                                text = { Text(tab.label, maxLines = 1) },
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        when (DetailTab.entries[tabIndex]) {
                            DetailTab.HOME -> HomeTabContent(
                                space = space,
                                vocab = vocab,
                                onEdit = { showEdit = true },
                            )
                            DetailTab.GALLERY -> GalleryTabContent(
                                space = space,
                                vocab = vocab,
                                viewModel = viewModel,
                            )
                            DetailTab.MESSAGES -> MessagesTabContent(
                                space = space,
                                viewModel = viewModel,
                            )
                            DetailTab.AI -> AiTabContent(
                                memorialId = space.id,
                                navController = navController,
                            )
                            DetailTab.VISIT -> VisitTabContent(
                                memorialId = space.id,
                                navController = navController,
                            )
                        }
                    }
                }
            }

            if (showEdit) {
                MetaEditDialog(
                    space = space,
                    vocab = vocab,
                    onSave = { n, r, i -> viewModel.updateMeta(n, r, i); showEdit = false },
                    onDismiss = { showEdit = false },
                )
            }
            if (showDelete) {
                ConfirmDangerDialog(
                    title = "删除纪念空间",
                    message = "确定删除「${space.name}」吗？空间内的相册、寄语、故事与日记都会一并移除。",
                    confirmLabel = "删除",
                    onConfirm = { showDelete = false; viewModel.deleteSpace() },
                    onDismiss = { showDelete = false },
                )
            }
        }
    }
}

@Composable
private fun HomeTabContent(
    space: MemorialLike,
    vocab: com.yuanqinglan.app.feature.memorial.ui.MemorialVocabText,
    onEdit: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = 12.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(com.yuanqinglan.app.core.designsystem.QingLanGreenSoft),
                ) {
                    Image(
                        painter = painterResource(memorialDrawable(space.portrait)),
                        contentDescription = vocab.portraitDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(84.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(space.name, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (space.relation.isNotBlank()) {
                        Text(
                            "${vocab.relationLabel}：${space.relation}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = com.yuanqinglan.app.core.designsystem.TextSecondary,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Text(
                        "创建于 ${formatDateTimeText(space.createdAtMillis)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = com.yuanqinglan.app.core.designsystem.TextSecondary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
        item {
            if (space.intro.isNotBlank()) {
                Text(
                    space.intro,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        item {
            MemorialSectionTitle(text = "内容概况", trailing = {
                Text(
                    text = "编辑",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(4.dp)
                        .clickableSmall { onEdit() },
                )
            })
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverviewPill("相册 ${space.gallery.size}", Modifier.weight(1f))
                OverviewPill("故事 ${space.stories.size}", Modifier.weight(1f))
                OverviewPill("日记 ${space.diary.size}", Modifier.weight(1f))
                OverviewPill("信件 ${space.letters.size}", Modifier.weight(1f))
            }
        }
        item {
            MemorialSectionTitle(text = "寄语摘录")
        }
        val recentMessages = space.sortedMessagesDesc()
        if (recentMessages.isEmpty()) {
            item { MemorialEmptyHint("还没有寄语", "去“寄语”页写第一句心里话") }
        } else {
            recentMessages.take(2).forEach { message ->
                item {
                    Column {
                        Text(
                            "${message.author} · ${formatDateTimeText(message.createdAtMillis)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = com.yuanqinglan.app.core.designsystem.TextSecondary,
                        )
                        Text(
                            message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewPill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.primaryContainer,
                androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            )
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = com.yuanqinglan.app.core.designsystem.TextPrimary)
    }
}

private fun Modifier.clickableSmall(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))

@Composable
private fun GalleryTabContent(
    space: MemorialLike,
    vocab: com.yuanqinglan.app.feature.memorial.ui.MemorialVocabText,
    viewModel: MemorialDetailViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectionMode by rememberSaveable(space.id) { mutableStateOf(false) }
    var selection by rememberSaveable(space.id) { mutableStateOf(setOf<String>()) }
    var viewer by remember { mutableStateOf<MediaRef?>(null) }
    var adding by remember { mutableStateOf(false) }
    var importMessage by remember { mutableStateOf<String?>(null) }

    // 删除后清掉失效选择
    val validIds = space.gallery.map { it.id }.toSet()
    if (selection.any { it !in validIds }) {
        selection = AlbumSelect.prune(selection, validIds)
    }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        adding = false
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = readUriBytes(context, uri)
            if (bytes == null || bytes.isEmpty()) {
                importMessage = "读取照片失败，请重试"
                return@launch
            }
            val saved = AppContainer.fileStorage.saveImage(bytes)
            viewModel.addPhotos(
                listOf(
                    MediaRef(
                        id = MemorialIds.next("ph"),
                        kind = MediaKind.IMAGE_FILE,
                        value = saved.toString(),
                        name = uri.lastPathSegment ?: "照片",
                        sizeBytes = bytes.size.toLong(),
                    ),
                ),
            )
            importMessage = null
        }
    }

    LaunchedEffect(adding) {
        if (adding) {
            importMessage = null
            pickImage.launch("image/*")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 10.dp),
    ) {
        importMessage?.let { message ->
            NoticeBanner(text = message, tone = NoticeTone.WARNING)
            Spacer(Modifier.height(8.dp))
        }
        // 相册网格自带滚动：外层不套垂直滚动，避免与 LazyVerticalGrid 无界嵌套。
        Box(modifier = Modifier.weight(1f)) {
            MemorialAlbumPanel(
                photos = space.gallery,
                selection = selection,
                selectionMode = selectionMode,
                description = vocab.galleryDescription,
                onToggleSelection = { id -> selection = AlbumSelect.toggle(selection, id) },
                onEnterSelection = { selectionMode = true },
                onExitSelection = {
                    selectionMode = false
                    selection = emptySet()
                },
                onView = { viewer = it },
                onDeleteSelected = { ids ->
                    viewModel.deletePhotos(ids)
                    selectionMode = false
                    selection = emptySet()
                },
            )
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            text = "添加照片到相册",
            onClick = { adding = true },
        )
    }
    viewer?.let { photo ->
        FullscreenMediaDialog(photo = photo, description = vocab.galleryDescription, onDismiss = { viewer = null })
    }
}

@Composable
private fun MessagesTabContent(
    space: MemorialLike,
    viewModel: MemorialDetailViewModel,
) {
    var draft by rememberSaveable(space.id) { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = 10.dp,
            bottom = 20.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "写下想对${space.name}说的话，仅保存在本机。",
                style = MaterialTheme.typography.bodyMedium,
                color = com.yuanqinglan.app.core.designsystem.TextSecondary,
            )
        }
        item {
            Column {
                FormTextField(
                    label = "寄语",
                    value = draft,
                    onValueChange = { draft = it },
                )
                Spacer(Modifier.height(8.dp))
                PrimaryButton(
                    text = "写下寄语",
                    enabled = draft.isNotBlank(),
                    onClick = {
                        viewModel.addMessage(draft)
                        draft = ""
                    },
                )
            }
        }
        val messages = space.sortedMessagesDesc()
        if (messages.isEmpty()) {
            item { MemorialEmptyHint("还没有寄语", null) }
        } else {
            items(messages.size) { index ->
                val message = messages[index]
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${message.author} · ${formatDateTimeText(message.createdAtMillis)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = com.yuanqinglan.app.core.designsystem.TextSecondary,
                    )
                    Text(
                        message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AiTabContent(
    memorialId: String,
    navController: NavHostController,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NoticeBanner(
            text = "AI 追忆只处理你上传并授权的私人影像素材；生成内容仅本人与授权家人可见；可随时一键永久销毁；不提供逝者实时对话。",
            tone = NoticeTone.COMPLIANCE,
        )
        Text(
            "使用前需阅读并确认伦理与授权说明（授权范围、私人访问、用途透明、永久销毁）。",
            style = MaterialTheme.typography.bodyMedium,
        )
        PrimaryButton(
            text = "阅读伦理说明并进入素材工作台",
            onClick = {
                AiFlowGate.prepare(memorialId)
                navController.navigate(AppRoute.AI_ETHICS.route)
            },
        )
        Text(
            "生成能力为本地流程展示：不调用外部 AI 服务，素材与结果都不离开本机。",
            style = MaterialTheme.typography.labelMedium,
            color = com.yuanqinglan.app.core.designsystem.TextSecondary,
        )
    }
}

@Composable
private fun VisitTabContent(
    memorialId: String,
    navController: NavHostController,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NoticeBanner(
            text = "异地代祭为付费预约服务；线上集体共祭为公益报名活动。两类服务相互独立，不混用。",
            tone = NoticeTone.INFO,
        )
        PrimaryButton(
            text = "异地代祭预约",
            onClick = { navController.navigate(MemorialRoutes.daiji(memorialId)) },
        )
        SecondaryButton(
            text = "线上集体共祭（公益）",
            onClick = { navController.navigate(AppRoute.COLLECTIVE_HISTORY.route) },
        )
        SecondaryButton(
            text = "祭扫时光",
            onClick = { navController.navigate(AppRoute.MEMORIAL_TIME.route) },
        )
    }
}

/** 编辑基本信息对话框。 */
@Composable
private fun MetaEditDialog(
    space: MemorialLike,
    vocab: com.yuanqinglan.app.feature.memorial.ui.MemorialVocabText,
    onSave: (String, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(space.id) { mutableStateOf(space.name) }
    var relation by remember(space.id) { mutableStateOf(space.relation) }
    var intro by remember(space.id) { mutableStateOf(space.intro) }
    val nameError = MemorialFormRules.nameError(name)
    val relationError = MemorialFormRules.relationError(relation)
    val introError = MemorialFormRules.introError(intro)
    val canSave = nameError == null && relationError == null && introError == null

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑纪念空间", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                FormTextField(label = "名称 *", value = name, onValueChange = { name = it }, isError = nameError != null, supportingText = nameError)
                Spacer(Modifier.height(4.dp))
                FormTextField(label = "${vocab.relationLabel} *", value = relation, onValueChange = { relation = it }, isError = relationError != null, supportingText = relationError)
                Spacer(Modifier.height(4.dp))
                FormTextField(label = "简介（选填）", value = intro, onValueChange = { intro = it }, isError = introError != null, supportingText = introError)
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(enabled = canSave, onClick = { onSave(name, relation, intro) }) { Text("保存") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
