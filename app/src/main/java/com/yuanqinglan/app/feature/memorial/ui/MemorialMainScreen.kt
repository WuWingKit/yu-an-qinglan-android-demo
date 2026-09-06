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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.QingLanGreenSoft
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.EmptyState
import com.yuanqinglan.app.core.ui.ErrorRetry
import com.yuanqinglan.app.core.ui.FormTextField
import com.yuanqinglan.app.core.ui.LoadingState
import com.yuanqinglan.app.core.ui.NoticeBanner
import com.yuanqinglan.app.core.ui.NoticeTone
import com.yuanqinglan.app.core.ui.PrimaryButton
import com.yuanqinglan.app.data.local.AppContainer
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 纪念空间主页（集中管理）ViewModel：按 ID 观察对应轨空间，
 * 相册/寄语/信件等管理动作全部回到所属轨存储。
 */
class MemorialMainViewModel(
    private val repository: MemorialRepository,
    val memorialId: String,
) : ViewModel() {

    private val _state = MutableStateFlow<DemoState<MemorialLike>>(DemoState.Loading)
    val state: StateFlow<DemoState<MemorialLike>> = _state.asStateFlow()

    private var collectJob: Job? = null

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

    fun updateMeta(name: String, relation: String, intro: String) {
        viewModelScope.launch {
            when (MemorialTrack.ofId(memorialId)) {
                MemorialTrack.HUMAN -> repository.updateHumanMeta(memorialId, name, relation, intro)
                MemorialTrack.PET -> repository.updatePetMeta(memorialId, name, relation, intro)
            }
        }
    }

    override fun onCleared() {
        collectJob?.cancel()
        super.onCleared()
    }
}

@Composable
fun MemorialMainScreen(
    memorialId: String,
    navController: NavHostController,
) {
    val context = LocalContext.current
    val repository = remember(context) { MemorialServiceLocator.repository(context) }
    val viewModel: MemorialMainViewModel = viewModel(
        factory = remember(repository, memorialId) {
            MemorialViewModelFactory { MemorialMainViewModel(repository, memorialId) }
        },
    )
    MemorialMainContent(viewModel = viewModel, navController = navController)
}

@Composable
private fun MemorialMainContent(
    viewModel: MemorialMainViewModel,
    navController: NavHostController,
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value

    when (state) {
        DemoState.Loading -> AppScaffold(title = "纪念空间", onBack = { navController.popBackStack() }) {
            LoadingState()
        }
        is DemoState.Error -> AppScaffold(title = "纪念空间", onBack = { navController.popBackStack() }) {
            ErrorRetry(message = state.message, onRetry = viewModel::refresh)
        }
        DemoState.Empty -> AppScaffold(title = "纪念空间", onBack = { navController.popBackStack() }) {
            EmptyState(title = "纪念空间不存在", description = "可能已被删除", actionLabel = "返回", onAction = { navController.popBackStack() })
        }
        is DemoState.Success -> {
            MainPageContent(
                space = state.value,
                viewModel = viewModel,
                navController = navController,
            )
        }
    }
}

@Composable
private fun MainPageContent(
    space: MemorialLike,
    viewModel: MemorialMainViewModel,
    navController: NavHostController,
) {
    val vocab = MemorialVocab.ofMemorialId(space.id)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showEdit by remember { mutableStateOf(false) }
    var messageDraft by rememberSaveable(space.id) { mutableStateOf("") }
    var importMessage by remember { mutableStateOf<String?>(null) }

    // ---- 相册管理状态 ----
    var selectionMode by rememberSaveable(space.id) { mutableStateOf(false) }
    var selection by rememberSaveable(space.id) { mutableStateOf(setOf<String>()) }
    var viewer by remember { mutableStateOf<MediaRef?>(null) }
    var addingPhoto by remember { mutableStateOf(false) }

    val validIds = space.gallery.map { it.id }.toSet()
    if (selection.any { it !in validIds }) {
        selection = AlbumSelect.prune(selection, validIds)
    }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        addingPhoto = false
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

    LaunchedEffect(addingPhoto) {
        if (addingPhoto) {
            importMessage = null
            pickImage.launch("image/*")
        }
    }

    val letters = space.sortedLettersDesc()
    val messages = space.sortedMessagesDesc()

    AppScaffold(
        title = space.name,
        onBack = { navController.popBackStack() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ---- 大肖像 + 信息简介 ----
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(QingLanGreenSoft),
                ) {
                    Image(
                        painter = painterResource(memorialDrawable(space.portrait)),
                        contentDescription = vocab.portraitDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(112.dp),
                    )
                }
            }
            MemorialIntroBlock(
                memorial = space,
                vocab = vocab,
                onEdit = { showEdit = true },
                modifier = Modifier.fillMaxWidth(),
            )

            // ---- 相册集中管理 ----
            MemorialSectionTitle(text = "相册")
            importMessage?.let { message ->
                NoticeBanner(text = message, tone = NoticeTone.WARNING)
            }
            if (importMessage != null) {
                Spacer(Modifier.height(4.dp))
            }
            // 相册网格在固定高度区域内滚动，避免与整页滚动嵌套冲突。
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
            ) {
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
            PrimaryButton(
                text = "添加照片到相册",
                onClick = { addingPhoto = true },
            )

            // ---- 寄语 ----
            MemorialSectionTitle(text = "寄语")
            Text(
                text = "写下想对${space.name}说的话，仅保存在本机。",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Column {
                FormTextField(
                    label = "寄语",
                    value = messageDraft,
                    onValueChange = { messageDraft = it },
                )
                Spacer(Modifier.height(8.dp))
                PrimaryButton(
                    text = "写下寄语",
                    enabled = messageDraft.isNotBlank(),
                    onClick = {
                        viewModel.addMessage(messageDraft)
                        messageDraft = ""
                    },
                )
            }
            if (messages.isEmpty()) {
                MemorialEmptyHint("还没有寄语", "写第一句心里话，会保存在本机")
            } else {
                messages.forEach { message ->
                    MessageRow(message)
                }
            }

            // ---- 信件 ----
            MemorialSectionTitle(
                text = "信件",
                trailing = {
                    TextButton(
                        onClick = { navController.navigate(MemorialRoutes.letterWrite(space.id)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("写信")
                    }
                },
            )
            if (letters.isEmpty()) {
                MemorialEmptyHint("还没有信件", "点击右上角「写信」，把想说的话写成信")
            } else {
                letters.forEach { letter ->
                    LetterRow(
                        title = letter.title,
                        timeText = formatDateTimeText(letter.createdAtMillis),
                        onClick = { navController.navigate(MemorialRoutes.letterView(letter.id)) },
                    )
                }
            }
        }
    }

    if (showEdit) {
        MetaEditDialog(
            space = space,
            vocab = vocab,
            onSave = { n, r, i ->
                viewModel.updateMeta(n, r, i)
                showEdit = false
            },
            onDismiss = { showEdit = false },
        )
    }
    viewer?.let { photo ->
        FullscreenMediaDialog(photo = photo, description = vocab.galleryDescription, onDismiss = { viewer = null })
    }
}

@Composable
private fun MessageRow(message: MemorialMessage) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "${message.author} · ${formatDateTimeText(message.createdAtMillis)}",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )
        Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** 信件行：整行可点进入信件查看。 */
@Composable
private fun LetterRow(
    title: String,
    timeText: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        color = SurfaceCard,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppDimensions.CardPadding, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                text = "查看",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** 编辑纪念空间基本信息对话框（同详情页编辑规则）。 */
@Composable
private fun MetaEditDialog(
    space: MemorialLike,
    vocab: MemorialVocabText,
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

    AlertDialog(
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
            TextButton(enabled = canSave, onClick = { onSave(name, relation, intro) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
