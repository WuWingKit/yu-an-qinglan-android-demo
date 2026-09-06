/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.treehole.ui

import android.os.SystemClock
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.QingLanGreen
import com.yuanqinglan.app.core.designsystem.QingLanGreenDark
import com.yuanqinglan.app.core.designsystem.QingLanGreenSoft
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.designsystem.currentTouchTargetSize
import com.yuanqinglan.app.core.ui.ConfirmDangerDialog
import com.yuanqinglan.app.feature.treehole.model.KindResponse
import com.yuanqinglan.app.feature.treehole.model.TreeholeLetterLike
import kotlinx.coroutines.delay

/**
 * 轻回应区块：点灯 / 叶片 / 花朵三种一次性关怀动作与本地确认文案，
 * 不记录计数、排行或他人行为。App 2.0（Issue #17）为三种动作补充克制动画反馈：
 * 点灯柔和点亮并短暂扩散、叶片轻缓掠过、花朵轻微绽放并淡出（600–1200ms）。
 * 动画只在瞬时 UI 状态内（remember，非 rememberSaveable，旋转屏幕不重播）；
 * 系统低动态（animator_duration_scale=0）下降级为颜色/图标状态与确认文案；
 * 确认文案通过 live region 供 TalkBack 播报成功反馈。
 */
@Composable
internal fun ResponseSection(
    viewModel: TreeholePoolViewModel,
    letter: TreeholeLetterLike,
    candidates: List<TreeholeLetterLike>,
    responseMessage: String?,
    modifier: Modifier = Modifier,
) {
    var showReportDialog by remember(letter.id) { mutableStateOf(false) }

    // 瞬时动画状态：按信件独立；remember（非 rememberSaveable）→ 旋转屏幕不重播旧动画。
    val throttle = remember(letter.id) { ResponseAnimationThrottle() }
    var animationRequest by remember(letter.id) { mutableStateOf<ResponseAnimationRequest?>(null) }
    // 低动态/动画间隙的瞬时颜色与图标状态反馈（静态颜色切换，不产生位移）。
    var highlightedKind by remember(letter.id) { mutableStateOf<KindResponse?>(null) }
    val animatorDurationScale = rememberAnimatorDurationScale()
    val animationsEnabled = remember(animatorDurationScale) {
        responseAnimationEnabled(animatorDurationScale)
    }

    LaunchedEffect(highlightedKind) {
        if (highlightedKind != null) {
            delay(BUTTON_HIGHLIGHT_MILLIS)
            highlightedKind = null
        }
    }
    // 动画结束后清理覆盖层，动画只存在于瞬时 UI 状态内。
    LaunchedEffect(animationRequest) {
        if (animationRequest != null) {
            delay(maxAnimationMillis + OVERLAY_LINGER_MARGIN_MILLIS)
            animationRequest = null
        }
    }

    fun trigger(kind: KindResponse) {
        viewModel.respond(kind)
        highlightedKind = kind
        if (animationsEnabled) {
            throttle.request(kind, SystemClock.elapsedRealtime())?.let { request ->
                animationRequest = request
            }
        }
    }

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
        // 动画覆盖层与按钮行同尺寸：非交互、瞬时绘制，不拦截点击、不覆盖文本。
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                KindResponseButton(
                    kind = KindResponse.LIGHT,
                    icon = Icons.Outlined.Lightbulb,
                    highlighted = highlightedKind == KindResponse.LIGHT,
                    onClick = { trigger(KindResponse.LIGHT) },
                    modifier = Modifier.weight(1f),
                )
                KindResponseButton(
                    kind = KindResponse.LEAF,
                    icon = Icons.Outlined.Eco,
                    highlighted = highlightedKind == KindResponse.LEAF,
                    onClick = { trigger(KindResponse.LEAF) },
                    modifier = Modifier.weight(1f),
                )
                KindResponseButton(
                    kind = KindResponse.FLOWER,
                    icon = Icons.Outlined.LocalFlorist,
                    highlighted = highlightedKind == KindResponse.FLOWER,
                    onClick = { trigger(KindResponse.FLOWER) },
                    modifier = Modifier.weight(1f),
                )
            }
            animationRequest?.let { request ->
                val overlayModifier = Modifier.matchParentSize()
                key(request.id) {
                    ResponseFeedbackOverlay(
                        request = request,
                        modifier = overlayModifier,
                    )
                }
            }
        }
        responseMessage?.let { message ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                // TalkBack 播报：文案变化时通过 live region 温和播报成功反馈。
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
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
internal fun KindResponseButton(
    kind: KindResponse,
    icon: ImageVector,
    onClick: () -> Unit,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val touchTarget = currentTouchTargetSize()
    val containerColor by animateColorAsState(
        targetValue = if (highlighted) QingLanGreenSoft else SurfaceCard,
        animationSpec = tween(durationMillis = BUTTON_COLOR_ANIMATION_MILLIS),
        label = "轻回应按钮底色",
    )
    val tint by animateColorAsState(
        targetValue = if (highlighted) QingLanGreenDark else MaterialTheme.colorScheme.primary,
        animationSpec = tween(durationMillis = BUTTON_COLOR_ANIMATION_MILLIS),
        label = "轻回应图标颜色",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (highlighted) 1.12f else 1f,
        animationSpec = tween(durationMillis = BUTTON_ICON_ANIMATION_MILLIS, easing = FastOutSlowInEasing),
        label = "轻回应图标缩放",
    )
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = touchTarget)
            .clip(RoundedCornerShape(AppDimensions.CompactRadius))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = kind.label,
            tint = tint,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = kind.label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
        )
    }
}

/** 按当前信件播放克制的一次性动画反馈（由 [ResponseAnimationRequest.id] 唯一对应一轮）。 */
@Composable
private fun ResponseFeedbackOverlay(
    request: ResponseAnimationRequest,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        when (request.kind) {
            KindResponse.LIGHT -> LampLightAnimation(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = maxWidth * (LIGHT_ANCHOR_FRACTION - 0.5f),
                        y = 0.dp,
                    ),
            )
            KindResponse.LEAF -> LeafDriftAnimation(
                containerWidth = maxWidth,
                containerHeight = maxHeight,
                modifier = Modifier.matchParentSize().clipToBounds(),
            )
            KindResponse.FLOWER -> FlowerBloomAnimation(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = maxWidth * (FLOWER_ANCHOR_FRACTION - 0.5f),
                        y = 0.dp,
                    ),
            )
        }
    }
}

/** 点灯：柔和点亮并短暂扩散（浅绿光晕由小到大并淡出）。 */
@Composable
private fun LampLightAnimation(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = LAMP_ANIMATION_MILLIS, easing = FastOutSlowInEasing),
        )
    }
    val p = progress.value
    Box(
        modifier = modifier
            .size(LAMP_GLOW_SIZE)
            .graphicsLayer {
                scaleX = lampScale(p)
                scaleY = lampScale(p)
                this.alpha = lampAlpha(p)
            }
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        QingLanGreen.copy(alpha = 0.55f),
                        QingLanGreen.copy(alpha = 0.10f),
                        Color.Transparent,
                    ),
                ),
                shape = CircleShape,
            ),
    )
}

/** 叶片：少量叶片轻缓掠过按钮行（固定种子，位置稳定；透明度边缘渐隐）。 */
@Composable
private fun LeafDriftAnimation(
    containerWidth: Dp,
    containerHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = LEAF_ANIMATION_MILLIS, easing = LinearEasing),
        )
    }
    val density = LocalDensity.current
    val widthPx = with(density) { containerWidth.toPx() }
    val heightPx = with(density) { containerHeight.toPx() }
    val specs = remember { leafSpecs() }
    Box(modifier = modifier) {
        specs.forEach { spec ->
            val localProgress =
                ((progress.value - spec.startFraction) / (1f - spec.startFraction)).coerceIn(0f, 1f)
            LeafParticle(
                spec = spec,
                progress = localProgress,
                widthPx = widthPx,
                heightPx = heightPx,
            )
        }
    }
}

@Composable
private fun LeafParticle(
    spec: LeafSpec,
    progress: Float,
    widthPx: Float,
    heightPx: Float,
) {
    val x = (spec.startX + spec.travelX * progress) * widthPx
    val y = (spec.startY + spec.travelY * progress) * heightPx
    val alpha = leafAlpha(progress, spec.alphaPeak)
    Icon(
        imageVector = Icons.Outlined.Eco,
        contentDescription = null,
        tint = QingLanGreen.copy(alpha = alpha),
        modifier = Modifier
            .graphicsLayer {
                translationX = x
                translationY = y
                rotationZ = spec.rotationDegrees * progress
                scaleX = leafScale(progress)
                scaleY = leafScale(progress)
                this.alpha = alpha
            }
            .size(spec.sizeDp.dp),
    )
}

/** 花朵：轻微绽放并淡出（开合缩放 + 轻微摇摆 + 末段淡出）。 */
@Composable
private fun FlowerBloomAnimation(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = FLOWER_ANIMATION_MILLIS, easing = FastOutSlowInEasing),
        )
    }
    val p = progress.value
    Box(
        modifier = modifier
            .size(FLOWER_HALO_SIZE),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = bloomScale(p)
                    scaleY = bloomScale(p)
                    this.alpha = bloomAlpha(p) * 0.55f
                }
                .background(
                    brush = Brush.radialGradient(
                        listOf(QingLanGreenSoft, Color.Transparent),
                    ),
                    shape = CircleShape,
                ),
        )
        Icon(
            imageVector = Icons.Outlined.LocalFlorist,
            contentDescription = null,
            tint = QingLanGreen.copy(alpha = bloomAlpha(p)),
            modifier = Modifier
                .size(FLOWER_BLOOM_SIZE)
                .graphicsLayer {
                    scaleX = bloomScale(p)
                    scaleY = bloomScale(p)
                    rotationZ = bloomRotation(p)
                    this.alpha = bloomAlpha(p)
                },
        )
    }
}

/** 读取系统 animator_duration_scale：0 表示关闭动画，由 [responseAnimationEnabled] 降级。 */
@Composable
private fun rememberAnimatorDurationScale(): Float {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            DEFAULT_ANIMATOR_DURATION_SCALE,
        )
    }
}

private const val DEFAULT_ANIMATOR_DURATION_SCALE = 1f
private const val BUTTON_HIGHLIGHT_MILLIS = 480L
private const val BUTTON_COLOR_ANIMATION_MILLIS = 220
private const val BUTTON_ICON_ANIMATION_MILLIS = 180
private const val OVERLAY_LINGER_MARGIN_MILLIS = 150L
private val maxAnimationMillis = maxOf(LAMP_ANIMATION_MILLIS, LEAF_ANIMATION_MILLIS, FLOWER_ANIMATION_MILLIS)

/** 动画锚点：点灯对齐左侧按钮，花朵对齐右侧按钮（叶片铺满整行，无需偏移）。 */
private const val LIGHT_ANCHOR_FRACTION = 1f / 6f
private const val FLOWER_ANCHOR_FRACTION = 5f / 6f

private val LAMP_GLOW_SIZE = 56.dp
private val FLOWER_BLOOM_SIZE = 36.dp
private val FLOWER_HALO_SIZE = 52.dp
