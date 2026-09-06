/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.treehole.ui

import com.yuanqinglan.app.feature.treehole.model.KindResponse
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * 轻回应动画反馈的纯逻辑层（Issue #17）：
 * 确认文案、系统低动态降级决策、连续点击节流/取消状态机与动画参数。
 * 全部为纯 Kotlin，可在 JVM 单测中直接验证；不依赖 Compose/Android 框架，
 * 不触碰仓库与持久化——动画状态只存在于瞬时 UI 内存中，不记录次数、排行或热度。
 */

/** 三种轻回应的本地确认文案（与 [TreeholePoolViewModel.respond] 对外契约一致，不显示任何计数）。 */
internal fun kindResponseMessage(kind: KindResponse): String = when (kind) {
    KindResponse.LIGHT -> "已为你点亮一盏灯，愿这份关怀被温柔接住"
    KindResponse.LEAF -> "已为你寄去一片新叶，愿这份关怀被温柔接住"
    KindResponse.FLOWER -> "已为你送上一朵小花，愿这份关怀被温柔接住"
}

/**
 * 系统低动态降级决策：读取 Settings.Global 的 animator_duration_scale 后调用。
 * 0 表示系统关闭动画，须降级为颜色/图标状态与确认文案；>0（含降速）保留动画。
 */
internal fun responseAnimationEnabled(animatorDurationScale: Float): Boolean =
    animatorDurationScale > 0f

/**
 * 一次动画请求的瞬时身份。id 单调递增，UI 以 id 为 key 重建动画（取消前一轮）。
 * 该请求只携带身份与类型，不携带任何计数/排行/热度信息。
 */
internal data class ResponseAnimationRequest(
    val id: Long,
    val kind: KindResponse,
)

/**
 * 连续点击节流/取消状态机（每个展示中的信件独立持有一个实例）：
 * - 冷却窗口内返回 null：确认文案照常更新，但动画不重复叠加；
 * - 冷却结束后返回新请求：id 递增，UI 以 id 为 key 重建动画，自动取消前一轮。
 * 只暴露"最新一次请求或 null"，不提供任何累计计数接口。
 */
internal class ResponseAnimationThrottle(
    private val minIntervalMillis: Long = RESPONSE_ANIMATION_MIN_INTERVAL_MILLIS,
) {
    private var lastAcceptedMillis: Long = Long.MIN_VALUE
    private var nextId: Long = 0L

    fun request(kind: KindResponse, nowMillis: Long): ResponseAnimationRequest? {
        if (lastAcceptedMillis != Long.MIN_VALUE && nowMillis - lastAcceptedMillis < minIntervalMillis) {
            return null
        }
        lastAcceptedMillis = nowMillis
        nextId += 1L
        return ResponseAnimationRequest(id = nextId, kind = kind)
    }
}

// ---------- 动画时长与参数（600–1200ms；固定随机种子保证截图/测试稳定） ----------

/** 点灯动画时长：柔和点亮并短暂扩散。 */
internal const val LAMP_ANIMATION_MILLIS = 900

/** 叶片动画时长：少量叶片轻缓掠过。 */
internal const val LEAF_ANIMATION_MILLIS = 1050

/** 花朵动画时长：轻微绽放并淡出。 */
internal const val FLOWER_ANIMATION_MILLIS = 880

/** 同一信件连续触发的冷却窗口：窗口内动画不叠加。 */
internal const val RESPONSE_ANIMATION_MIN_INTERVAL_MILLIS = 320L

/** 点灯：光晕尺寸随进度放大（0.35 → 1.15，柔和扩散）。 */
internal fun lampScale(progress: Float): Float = 0.35f + 0.8f * progress.coerceIn(0f, 1f)

/** 点灯：扩散末段淡出。 */
internal fun lampAlpha(progress: Float): Float = (1f - progress.coerceIn(0f, 1f)).coerceIn(0f, 1f)

/** 叶片数量：少量即可，避免打扰。 */
internal const val LEAF_COUNT = 3

/** 叶片参数固定种子：同一种子得到同一序列，保证截图/测试稳定。 */
internal const val LEAF_ANIMATION_SEED = 17L

internal const val LEAF_FADE_IN_FRACTION = 0.15f
internal const val LEAF_FADE_OUT_FRACTION = 0.25f

/** 叶片透明度曲线：进入渐现、中段保持、末段渐隐，边缘几乎透明、不过度打扰。 */
internal fun leafAlpha(progress: Float, peak: Float): Float {
    val fadeIn = (progress / LEAF_FADE_IN_FRACTION).coerceIn(0f, 1f)
    val fadeOut = ((1f - progress) / LEAF_FADE_OUT_FRACTION).coerceIn(0f, 1f)
    return (peak.coerceIn(0f, 1f) * fadeIn * fadeOut).coerceIn(0f, 1f)
}

/** 叶片进入与离开时轻微收放，避免图标在边缘突然出现或消失。 */
internal fun leafScale(progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    return 0.72f + 0.28f * sin(PI.toFloat() * p)
}

/** 单枚叶片的运动参数（以容器宽/高比例为单位的纯数据，不携带计数）。 */
internal data class LeafSpec(
    val startFraction: Float,
    val startX: Float,
    val travelX: Float,
    val startY: Float,
    val travelY: Float,
    val sizeDp: Float,
    val rotationDegrees: Float,
    val alphaPeak: Float,
)

/** 固定种子生成叶片参数：从左向右轻缓掠过并微微上飘，垂直方向保持在行高范围内。 */
internal fun leafSpecs(seed: Long = LEAF_ANIMATION_SEED): List<LeafSpec> {
    val random = Random(seed)
    return List(LEAF_COUNT) {
        LeafSpec(
            startFraction = random.nextFloat() * 0.3f,
            startX = -0.15f + random.nextFloat() * 0.3f,
            travelX = 0.55f + random.nextFloat() * 0.4f,
            startY = 0.35f + random.nextFloat() * 0.45f,
            travelY = -(0.1f + random.nextFloat() * 0.25f),
            sizeDp = 9f + random.nextFloat() * 6f,
            rotationDegrees = -14f + random.nextFloat() * 28f,
            alphaPeak = 0.45f + random.nextFloat() * 0.4f,
        )
    }
}

/** 花朵：轻微绽放（开合缩放，0.55 → 1.15 后保持）。 */
internal fun bloomScale(progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    val opening = easeOutCubic((p / 0.65f).coerceAtMost(1f))
    return 0.55f + 0.6f * opening
}

/** 花朵：轻微摇摆旋转（0° → 6° → 0°）。 */
internal fun bloomRotation(progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    return 6f * sin(PI.toFloat() * p)
}

/** 花朵：快速点亮后于末段淡出。 */
internal fun bloomAlpha(progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    val fadeIn = (p / 0.12f).coerceIn(0f, 1f)
    val fadeOut = ((1f - p) / 0.32f).coerceIn(0f, 1f)
    return (fadeIn * fadeOut).coerceIn(0f, 1f)
}

private fun easeOutCubic(t: Float): Float {
    val u = t.coerceIn(0f, 1f)
    return 1f - (1f - u) * (1f - u) * (1f - u)
}
