/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.model.AudienceTrack
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.AudienceSegment
import com.yuanqinglan.app.core.ui.FormTextField
import com.yuanqinglan.app.core.ui.PrimaryButton
import com.yuanqinglan.app.core.ui.SecondaryButton
import com.yuanqinglan.app.feature.memorial.data.MemorialRepository
import com.yuanqinglan.app.feature.memorial.data.MemorialServiceLocator
import com.yuanqinglan.app.feature.memorial.model.HumanMemorialDraft
import com.yuanqinglan.app.feature.memorial.model.MemorialFormRules
import com.yuanqinglan.app.feature.memorial.model.MemorialTrack
import com.yuanqinglan.app.feature.memorial.model.PetMemorialDraft
import com.yuanqinglan.app.navigation.AppRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 新建纪念空间表单（类型=人/宠、名称、关系、简介、肖像选择）。
 * 本地校验通过后创建，成功后跳转到新空间详情；可“重新填写”。
 */
class MemorialCreateViewModel(
    private val repository: MemorialRepository,
) : ViewModel() {

    private val _track = MutableStateFlow(AudienceTrack.HUMAN)
    val track: StateFlow<AudienceTrack> = _track.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _relation = MutableStateFlow("")
    val relation: StateFlow<String> = _relation.asStateFlow()

    private val _intro = MutableStateFlow("")
    val intro: StateFlow<String> = _intro.asStateFlow()

    private val _portrait = MutableStateFlow(HumanMemorialDraft.DEFAULT_PORTRAIT)
    val portrait: StateFlow<String> = _portrait.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    private val _createdId = MutableStateFlow<String?>(null)
    val createdId: StateFlow<String?> = _createdId.asStateFlow()

    fun selectTrack(newTrack: AudienceTrack) {
        _track.value = newTrack
        _portrait.value = if (newTrack == AudienceTrack.HUMAN) {
            HumanMemorialDraft.DEFAULT_PORTRAIT
        } else {
            PetMemorialDraft.DEFAULT_PORTRAIT
        }
    }

    fun updateName(value: String) = apply { _name.value = value }
    fun updateRelation(value: String) = apply { _relation.value = value }
    fun updateIntro(value: String) = apply { _intro.value = value }
    fun selectPortrait(token: String) {
        _portrait.value = token
    }

    /** 提交（先校验；不满足直接忽略，错误由表单展示）。 */
    fun submit() {
        val n = _name.value
        val r = _relation.value
        val i = _intro.value
        if (!MemorialFormRules.canSubmit(n, r, i) || _submitting.value) return
        _submitting.value = true
        viewModelScope.launch {
            val id = if (_track.value == AudienceTrack.HUMAN) {
                repository.createHuman(
                    HumanMemorialDraft(
                        name = n,
                        relation = r,
                        intro = i,
                        portrait = _portrait.value,
                    ),
                ).id
            } else {
                repository.createPet(
                    PetMemorialDraft(
                        name = n,
                        relation = r,
                        intro = i,
                        portrait = _portrait.value,
                    ),
                ).id
            }
            _createdId.value = id
            _submitting.value = false
        }
    }

    /** 重新填写：清空输入（类型与肖像保留）。 */
    fun resetForm() {
        _name.value = ""
        _relation.value = ""
        _intro.value = ""
        _createdId.value = null
    }
}

@Composable
fun MemorialCreateScreen(navController: NavHostController) {
    val context = LocalContext.current
    val repository = remember(context) { MemorialServiceLocator.repository(context) }
    val viewModel: MemorialCreateViewModel = viewModel(
        factory = remember(repository) {
            MemorialViewModelFactory { MemorialCreateViewModel(repository) }
        },
    )
    val track by viewModel.track.collectAsStateWithLifecycle()
    val name by viewModel.name.collectAsStateWithLifecycle()
    val relation by viewModel.relation.collectAsStateWithLifecycle()
    val intro by viewModel.intro.collectAsStateWithLifecycle()
    val portrait by viewModel.portrait.collectAsStateWithLifecycle()
    val submitting by viewModel.submitting.collectAsStateWithLifecycle()
    val createdId by viewModel.createdId.collectAsStateWithLifecycle()

    createdId?.let { id ->
        val target = if (id.startsWith(MemorialTrack.PREFIX_HUMAN)) {
            MemorialRoutes.detail(id)
        } else {
            MemorialRoutes.petMemorial(id)
        }
        LaunchedEffect(id) {
            navController.navigate(target) {
                popUpTo(AppRoute.MEMORIAL_CREATE.route) { inclusive = true }
            }
        }
    }

    val vocab = MemorialVocab.ofTrack(
        if (track == AudienceTrack.HUMAN) MemorialTrack.HUMAN else MemorialTrack.PET,
    )
    val nameError = MemorialFormRules.nameError(name)
    val relationError = MemorialFormRules.relationError(relation)
    val introError = MemorialFormRules.introError(intro)

    AppScaffold(
        title = "新建纪念空间",
        onBack = { navController.popBackStack() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "先选择纪念类型，再填写基本信息。内容仅保存在本机。",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Spacer(Modifier.height(12.dp))
            AudienceSegment(selected = track, onSelect = viewModel::selectTrack)
            Spacer(Modifier.height(16.dp))

            Text("肖像选择", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            PortraitOptions(
                track = track,
                selectedToken = portrait,
                onSelect = viewModel::selectPortrait,
            )

            Spacer(Modifier.height(18.dp))
            Text("基本信息", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))

            FormTextField(
                label = if (track == AudienceTrack.HUMAN) "纪念对象名称 *" else "伙伴昵称 *",
                value = name,
                onValueChange = viewModel::updateName,
                isError = nameError != null,
                supportingText = nameError,
            )
            Spacer(Modifier.height(4.dp))
            FormTextField(
                label = "${vocab.relationLabel} *",
                value = relation,
                onValueChange = viewModel::updateRelation,
                isError = relationError != null,
                supportingText = relationError,
            )
            Spacer(Modifier.height(4.dp))
            FormTextField(
                label = "简介（选填）",
                value = intro,
                onValueChange = viewModel::updateIntro,
                isError = introError != null,
                supportingText = introError,
            )
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = "创建纪念空间",
                enabled = !submitting && MemorialFormRules.canSubmit(name, relation, intro),
                onClick = viewModel::submit,
            )
            Spacer(Modifier.height(8.dp))
            SecondaryButton(text = "重新填写", onClick = viewModel::resetForm)
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun PortraitOptions(
    track: AudienceTrack,
    selectedToken: String,
    onSelect: (String) -> Unit,
) {
    val options: List<String> = if (track == AudienceTrack.HUMAN) {
        listOf(HumanPortraitToken, GalleryHumanTeaToken)
    } else {
        listOf(PetPortraitToken, GalleryPetParkToken)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { token ->
            val selected = selectedToken == token
            val description = if (track == AudienceTrack.HUMAN) {
                HUMAN_PORTRAIT_DESCRIPTION
            } else {
                PET_PORTRAIT_DESCRIPTION
            }
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    )
                    .padding(if (selected) 3.dp else 0.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(memorialDrawable(token)),
                    contentDescription = description,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(if (selected) 66.dp else 72.dp)
                        .clip(CircleShape)
                        .clickable { onSelect(token) },
                )
            }
        }
    }
}
