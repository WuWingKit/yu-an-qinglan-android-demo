/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.home.ui

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.QingLanGreen
import com.yuanqinglan.app.core.designsystem.QingLanGreenDark
import com.yuanqinglan.app.core.designsystem.QingLanGreenSoft
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.NoticeBanner
import com.yuanqinglan.app.core.ui.NoticeTone
import com.yuanqinglan.app.core.ui.PrimaryButton
import com.yuanqinglan.app.core.ui.ReferenceNote
import com.yuanqinglan.app.core.ui.SecondaryButton
import com.yuanqinglan.app.feature.home.MatchViewModel
import com.yuanqinglan.app.feature.home.model.MatchOption
import com.yuanqinglan.app.feature.home.model.MatchQuestion
import com.yuanqinglan.app.feature.home.model.MatchRecommendation

/**
 * 智能匹配页：本地规则问卷（4 题）→ 推荐结果页；可"重新匹配"。
 * 推荐仅基于本地规则与所选偏好，不代表任何机构意见。
 */
@Composable
fun MatchRoute(
    onBack: () -> Unit,
    onOpenBurial: () -> Unit,
    onOpenPolicy: () -> Unit,
) {
    val vm: MatchViewModel = viewModel { MatchViewModel() }
    val answers by vm.answers.collectAsStateWithLifecycle()
    val result by vm.result.collectAsStateWithLifecycle()
    val submitAttempted by vm.submitAttempted.collectAsStateWithLifecycle()

    AppScaffold(
        title = if (result == null) "智能匹配" else "匹配结果",
        onBack = onBack,
    ) {
        val current = result
        if (current == null) {
            QuestionnaireContent(
                questions = vm.questions,
                answers = answers,
                submitAttempted = submitAttempted,
                onAnswer = vm::answer,
                onSubmit = { vm.submit() },
            )
        } else {
            MatchResultContent(
                result = current,
                onRestart = vm::restart,
                onOpenBurial = onOpenBurial,
                onOpenPolicy = onOpenPolicy,
            )
        }
    }
}

@Composable
private fun QuestionnaireContent(
    questions: List<MatchQuestion>,
    answers: Map<String, String>,
    submitAttempted: Boolean,
    onAnswer: (String, String) -> Unit,
    onSubmit: () -> Unit,
) {
    val answeredCount = questions.count { !answers[it.id].isNullOrBlank() }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = 6.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "回答几个小问题，帮你初步了解适合的安葬方向。结果仅供参考。",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
        item {
            Text(
                text = "已完成 $answeredCount/${questions.size} 题",
                style = MaterialTheme.typography.labelMedium,
                color = QingLanGreenDark,
            )
        }
        itemsIndexed(questions) { index, question ->
            QuestionCard(
                index = index,
                question = question,
                selectedValue = answers[question.id],
                onSelect = { option -> onAnswer(question.id, option) },
            )
        }
        if (submitAttempted && answeredCount < questions.size) {
            item {
                NoticeBanner(
                    text = "请完成全部问题后再查看推荐结果。",
                    tone = NoticeTone.WARNING,
                )
            }
        }
        item {
            PrimaryButton(
                text = "查看推荐结果",
                onClick = onSubmit,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        item {
            ReferenceNote(text = HOME_COMPLIANCE_SENTENCE)
        }
    }
}

@Composable
private fun QuestionCard(
    index: Int,
    question: MatchQuestion,
    selectedValue: String?,
    onSelect: (String) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
            Text(
                text = "${index + 1}. ${question.title}",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.height(10.dp))
            question.options.forEach { option ->
                OptionRow(
                    option = option,
                    selected = selectedValue == option.value,
                    onClick = { onSelect(option.value) },
                )
            }
        }
    }
}

@Composable
private fun OptionRow(
    option: MatchOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(AppDimensions.CompactRadius)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(if (selected) QingLanGreenSoft else Color.Transparent, shape)
            .clickable(role = Role.Button, onClickLabel = option.label, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    color = if (selected) QingLanGreen else QingLanGreenSoft,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = option.label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) QingLanGreenDark else TextPrimary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun MatchResultContent(
    result: MatchRecommendation,
    onRestart: () -> Unit,
    onOpenBurial: () -> Unit,
    onOpenPolicy: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = 8.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(AppDimensions.CardRadius),
                color = QingLanGreenSoft,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(AppDimensions.CardPadding)) {
                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = QingLanGreenDark,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = result.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("推荐理由", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                result.reasons.forEach { reason ->
                    Text(
                        text = "·  $reason",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("下一步可以做的事", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                result.nextActions.forEach { action ->
                    Text(
                        text = "·  $action",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(
                    text = "前往安葬服务",
                    onClick = onOpenBurial,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = "政策预审",
                    onClick = onOpenPolicy,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            SecondaryButton(text = "重新匹配", onClick = onRestart)
        }
        item {
            NoticeBanner(
                text = "匹配结果由本地偏好规则生成，仅供了解参考，不构成服务或政策承诺。",
                tone = NoticeTone.COMPLIANCE,
            )
        }
    }
}
