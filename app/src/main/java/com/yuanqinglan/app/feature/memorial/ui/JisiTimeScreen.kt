/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.navigation.NavHostController
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.QingLanGreenSoft
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.model.AudienceTrack
import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.AudienceSegment
import com.yuanqinglan.app.core.ui.EmptyState
import com.yuanqinglan.app.core.ui.ErrorRetry
import com.yuanqinglan.app.core.ui.FormTextField
import com.yuanqinglan.app.core.ui.LoadingState
import com.yuanqinglan.app.core.ui.NoticeBanner
import com.yuanqinglan.app.core.ui.NoticeTone
import com.yuanqinglan.app.feature.memorial.data.JisiTimelineItem
import com.yuanqinglan.app.feature.memorial.data.MemorialRepository
import com.yuanqinglan.app.feature.memorial.data.MemorialServiceLocator
import com.yuanqinglan.app.feature.memorial.model.HumanMemorial
import com.yuanqinglan.app.feature.memorial.model.JisiVisitRecord
import com.yuanqinglan.app.feature.memorial.model.MemorialIds
import com.yuanqinglan.app.feature.memorial.model.MemorialLike
import com.yuanqinglan.app.feature.memorial.model.PetMemorial
import com.yuanqinglan.app.navigation.AppRoute
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 祭扫时光页 ViewModel：持续收集人类/宠物两轨空间流。
 * 轨道切换由页面本地状态决定读哪一侧；新增记录按空间 ID 前缀路由到所属轨存储。
 */
class JisiTimeViewModel(
    private val repository: MemorialRepository,
) : ViewModel() {

    private val _humanState = MutableStateFlow<DemoState<List<HumanMemorial>>>(DemoState.Loading)
    val humanState: StateFlow<DemoState<List<HumanMemorial>>> = _humanState.asStateFlow()

    private val _petState = MutableStateFlow<DemoState<List<PetMemorial>>>(DemoState.Loading)
    val petState: StateFlow<DemoState<List<PetMemorial>>> = _petState.asStateFlow()

    private var humanJob: Job? = null
    private var petJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        humanJob?.cancel()
        petJob?.cancel()
        humanJob = viewModelScope.launch {
            repository.humanSpaces().collect { _humanState.value = it }
        }
        petJob = viewModelScope.launch {
            repository.petSpaces().collect { _petState.value = it }
        }
    }

    /** 当前轨空间列表（公共只读形态）。 */
    fun spacesOf(track: AudienceTrack): List<MemorialLike> = when (track) {
        AudienceTrack.HUMAN ->
            (_humanState.value as? DemoState.Success<List<HumanMemorial>>)?.value ?: emptyList()
        AudienceTrack.PET ->
            (_petState.value as? DemoState.Success<List<PetMemorial>>)?.value ?: emptyList()
    }

    /** 新增一条祭扫记录（日期/地点必填已在表单校验）。 */
    fun addRecord(space: MemorialLike, date: LocalDate, place: String, message: String) {
        val trimmedPlace = place.trim()
        if (trimmedPlace.isEmpty()) return
        val record = JisiVisitRecord(
            id = MemorialIds.next("jisi"),
            dateMillis = localDateToMillis(date),
            dateText = dateTextOf(date),
            place = trimmedPlace,
            message = message.trim(),
        )
        viewModelScope.launch {
            repository.addJisiRecord(space.id, record)
        }
    }

    override fun onCleared() {
        humanJob?.cancel()
        petJob?.cancel()
        super.onCleared()
    }
}

@Composable
fun JisiTimeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val repository = remember(context) { MemorialServiceLocator.repository(context) }
    val viewModel: JisiTimeViewModel = viewModel(
        factory = remember(repository) {
            MemorialViewModelFactory { JisiTimeViewModel(repository) }
        },
    )
    JisiTimeContent(viewModel = viewModel, navController = navController)
}

@Composable
private fun JisiTimeContent(
    viewModel: JisiTimeViewModel,
    navController: NavHostController,
) {
    var trackName by rememberSaveable { mutableStateOf(AudienceTrack.HUMAN) }
    val humanState by viewModel.humanState.collectAsStateWithLifecycle()
    val petState by viewModel.petState.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    // 切换人/宠轨道时关闭记录对话框，避免记录被写入错误轨道。
    LaunchedEffect(trackName) {
        showAddDialog = false
    }

    val currentState = when (trackName) {
        AudienceTrack.HUMAN -> humanState
        AudienceTrack.PET -> petState
    }
    val spaces = viewModel.spacesOf(trackName)
    val timeline = remember(spaces) {
        timelineOf(spaces)
    }

    AppScaffold(
        title = "祭扫时光",
        onBack = { navController.popBackStack() },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                AudienceSegment(selected = trackName, onSelect = { trackName = it })
                Spacer(Modifier.height(10.dp))
                NoticeBanner(
                    text = "祭扫记录保存在本机，仅你和授权家人可见。",
                    tone = NoticeTone.INFO,
                )
                Spacer(Modifier.height(4.dp))
            }

            Box(modifier = Modifier.weight(1f)) {
                when (currentState) {
                    DemoState.Loading -> LoadingState()
                    is DemoState.Error -> ErrorRetry(
                        message = currentState.message,
                        onRetry = viewModel::refresh,
                    )
                    DemoState.Empty -> EmptyState(
                        title = "祭扫记录列表为空",
                        description = "当前还没有任何祭扫记录",
                    )
                    is DemoState.Success -> {
                        if (spaces.isEmpty()) {
                            EmptyState(
                                title = "还没有纪念空间",
                                description = "先为珍视的人或伙伴创建纪念空间，再记录祭扫时光",
                                actionLabel = "去新建",
                                onAction = { navController.navigate(AppRoute.MEMORIAL_HOME.route) },
                            )
                        } else {
                            JisiTimelineContent(
                                timeline = timeline,
                                onAdd = { showAddDialog = true },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog && spaces.isNotEmpty()) {
        JisiRecordAddDialog(
            spaces = spaces,
            onDismiss = { showAddDialog = false },
            onSubmit = { space, date, place, message ->
                showAddDialog = false
                viewModel.addRecord(space, date, place, message)
            },
        )
    }
}

/** 把某轨全部空间的所有祭扫记录展平并按日期升序。 */
private fun timelineOf(spaces: List<MemorialLike>): List<JisiTimelineItem> =
    spaces.flatMap { space ->
        space.sortedJisi().map { record ->
            JisiTimelineItem(memorialId = space.id, memorialName = space.name, record = record)
        }
    }.sortedBy { it.record.dateMillis }

@Composable
private fun JisiTimelineContent(
    timeline: List<JisiTimelineItem>,
    onAdd: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "祭扫记录",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = onAdd,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Text("新增祭扫记录")
                }
            }
        }
        if (timeline.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "还没有祭扫记录",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary,
                    )
                    Text(
                        text = "点击右上角「新增祭扫记录」，把每一次探望都记下来",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 6.dp, start = 24.dp, end = 24.dp),
                    )
                }
            }
        } else {
            items(timeline, key = { "${it.memorialId}-${it.record.id}" }) { entry ->
                JisiRecordCard(entry)
            }
        }
    }
}

@Composable
private fun JisiRecordCard(entry: JisiTimelineItem) {
    val record = entry.record
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        color = SurfaceCard,
    ) {
        Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = entry.memorialName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = record.dateText,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
            Text(
                text = record.place,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (record.message.isNotBlank()) {
                Text(
                    text = "寄语：${record.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** 新增祭扫记录对话框：选空间 → 选日期 → 地点/方式与寄语。 */
@Composable
private fun JisiRecordAddDialog(
    spaces: List<MemorialLike>,
    onDismiss: () -> Unit,
    onSubmit: (space: MemorialLike, date: LocalDate, place: String, message: String) -> Unit,
) {
    var selectedIndex by remember(spaces) { mutableIntStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var place by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val placeError = if (place.isBlank()) "请填写祭扫地点或方式" else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增祭扫记录", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "选择纪念空间",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(2.dp))
                spaces.forEachIndexed { index, space ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable { selectedIndex = index }
                            .padding(horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (index == selectedIndex) {
                                Icons.Filled.RadioButtonChecked
                            } else {
                                Icons.Outlined.RadioButtonUnchecked
                            },
                            contentDescription = null,
                            tint = if (index == selectedIndex) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                TextSecondary
                            },
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = space.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (space.relation.isNotBlank()) {
                            Text(
                                text = space.relation,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .background(QingLanGreenSoft, RoundedCornerShape(AppDimensions.CompactRadius))
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = selectedDate?.let { dateTextOf(it) } ?: "选择祭扫日期",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedDate != null) TextPrimary else TextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    if (selectedDate == null) {
                        Text(
                            text = "必填",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                FormTextField(
                    label = "地点 / 方式 *",
                    value = place,
                    onValueChange = { place = it },
                    isError = placeError != null,
                    supportingText = placeError,
                )
                Spacer(Modifier.height(6.dp))
                FormTextField(
                    label = "寄语（选填）",
                    value = message,
                    onValueChange = { message = it },
                )
            }
        },
        confirmButton = {
            val canSubmit = selectedDate != null && place.isNotBlank() && spaces.isNotEmpty()
            TextButton(
                enabled = canSubmit,
                onClick = {
                    val date = selectedDate ?: return@TextButton
                    val space = spaces[selectedIndex]
                    onSubmit(space, date, place, message)
                },
            ) {
                Text("保存记录")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        },
    )

    if (showDatePicker) {
        MemorialDatePickerDialog(
            title = "选择祭扫日期",
            initialDate = selectedDate,
            onConfirm = { date ->
                selectedDate = date
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}
