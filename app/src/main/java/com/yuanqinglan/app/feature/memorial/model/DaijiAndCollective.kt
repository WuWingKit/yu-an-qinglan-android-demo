/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.model

import java.time.LocalDateTime
import kotlinx.serialization.Serializable

/*
 * 异地代祭（付费预约、线下履约）与线上集体共祭（公益报名）是两类相互独立
 * 的服务：模型、状态机、存储与页面全部分开，绝不混成同一付费流程。
 */

// ---------------- 异地代祭 ----------------

/** 异地代祭服务套餐（展示性本地内容）。 */
@Serializable
data class DaijiPackage(
    val id: String,
    val title: String,
    val description: String,
    val priceText: String,
    val durationText: String,
    val contents: List<String> = emptyList(),
)

/** 代祭订单履约状态（单向推进状态机）。 */
enum class DaijiOrderStatus(val step: Int, val title: String, val description: String) {
    SUBMITTED(1, "预约提交", "代祭预约已记录，等待服务机构确认。"),
    CONFIRMED(2, "已确认", "服务机构已确认代祭安排与时间。"),
    COMPLETED(3, "履约完成", "代祭服务已完成，影像已归档至纪念空间。"),
    ;

    fun nextOrNull(): DaijiOrderStatus? = entries.getOrNull(ordinal + 1)

    fun canAdvance(): Boolean = this != COMPLETED
}

/** 代祭订单状态机：只暴露合法迁移（推进/重置）。 */
object DaijiOrderProgress {
    fun advance(current: DaijiOrderStatus): DaijiOrderStatus = current.nextOrNull() ?: current

    fun reset(): DaijiOrderStatus = DaijiOrderStatus.SUBMITTED
}

/**
 * 异地代祭订单（独立于集体共祭报名）。
 * 不收集真实姓名/手机号；称呼与留言仅在本地内存与私有目录流转。
 */
@Serializable
data class DaijiOrder(
    val id: String,
    val orderNo: String,
    val memorialId: String,
    val memorialName: String,
    val packageId: String,
    val packageTitle: String,
    val priceText: String,
    /** 委托人称呼（不填真实姓名）。 */
    val entrustName: String,
    val expectDateText: String,
    val message: String = "",
    /** 履约影像归档（file:// 私有目录），同时归档进所属纪念空间相册。 */
    val archiveImages: List<MediaRef> = emptyList(),
    val status: DaijiOrderStatus = DaijiOrderStatus.SUBMITTED,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) {
    fun copyStatus(newStatus: DaijiOrderStatus): DaijiOrder = copy(status = newStatus)

    fun addArchiveImages(images: List<MediaRef>): DaijiOrder = copy(archiveImages = archiveImages + images)
}

/** 本地代祭单号生成器：格式 YQJ-YYYYMMDD-NNNN。 */
class DaijiOrderNumberGenerator(
    private val now: () -> LocalDateTime = LocalDateTime::now,
) {
    fun next(seq: Int): String {
        val t = now()
        val date = "%04d%02d%02d".format(t.year, t.monthValue, t.dayOfMonth)
        return "YQJ-$date-${"%04d".format(seq)}"
    }
}

// ---------------- 线上集体共祭（免费公益活动） ----------------

/** 线上集体共祭活动（公益、免费，与代祭完全独立）。 */
@Serializable
data class CollectiveActivity(
    val id: String,
    val title: String,
    val description: String,
    val dateText: String,
    val location: String,
    val host: String = "",
    /** 公益活动长期开放报名。 */
    val open: Boolean = true,
)

/** 共祭报名记录（本地状态，不提交外部）。 */
@Serializable
data class CollectiveSignup(
    val id: String,
    val activityId: String,
    val activityTitle: String,
    val joinedAtMillis: Long,
)
