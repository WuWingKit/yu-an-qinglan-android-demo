/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial.model

import com.yuanqinglan.app.core.model.AudienceTrack
import java.time.LocalDate
import java.time.LocalDateTime

/** 订单进度状态（单向推进状态机）。 */
enum class BurialOrderStatus(val step: Int, val title: String, val description: String) {
    SUBMITTED(1, "提交成功", "预约信息已提交，等待机构确认。"),
    CONFIRMED(2, "机构确认", "机构已确认服务安排。"),
    ARRANGED(3, "服务安排", "服务已排期，等待按约定办理。"),
    COMPLETED(4, "已完成", "服务已完成，可凭凭证按开放时间祭扫。"),
    ;

    /** 下一合法状态；终态 [COMPLETED] 返回 null。 */
    fun nextOrNull(): BurialOrderStatus? = entries.getOrNull(ordinal + 1)

    /** 是否可继续推进。 */
    fun canAdvance(): Boolean = this != COMPLETED
}

/**
 * 订单进度机：只暴露合法迁移（推进 / 重置），不允许任意跳转。
 * - advance：按 SUBMITTED → CONFIRMED → ARRANGED → COMPLETED 单向推进，终态幂等（保持 COMPLETED）；
 * - reset：回到初始 SUBMITTED，用于本地重复查看流程。
 */
object OrderProgress {
    fun advance(current: BurialOrderStatus): BurialOrderStatus =
        current.nextOrNull() ?: current

    fun reset(): BurialOrderStatus = BurialOrderStatus.SUBMITTED
}

/**
 * 订单（强类型 track）。
 *
 * [BurialOrder.audience] 标明轨道，[BurialOrder.orderNo] 为本地生成的订单号，
 * 不包含真实个人信息；姓名、手机号等仅在预约当次内存中流转，不落盘、不提交外部。
 */
data class BurialOrder(
    val id: String,
    val orderNo: String,
    val audience: AudienceTrack,
    val serviceId: String,
    val serviceName: String,
    val planId: String,
    val planTitle: String,
    val mode: BurialMode,
    /** 人类：逝者姓名；宠物：宠物昵称。字段语义随 track 固定，不跨轨共用。 */
    val deceasedName: String,
    val contactName: String,
    val phone: String,
    val expectDate: LocalDate?,
    val amountText: String,
    val planPriceYuan: Int? = null,
    val prepaidYears: Int = 0,
    val prepaidManagementYuan: Int = 0,
    val selectedAddOns: List<String> = emptyList(),
    val addOnYuan: Int = 0,
    val subsidyYuan: Int = 0,
    val totalYuan: Int? = null,
    val managementExpiresYear: Int? = null,
    val renewalAnnualYuan: Int = 0,
    val status: BurialOrderStatus = BurialOrderStatus.SUBMITTED,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) {
    fun copyStatus(newStatus: BurialOrderStatus): BurialOrder =
        copy(status = newStatus)
}

/**
 * 本地订单号生成器。
 * 格式 YQ-YYYYMMDD-NNNN（如 YQ-20260905-0001），时间与序号均可注入，便于测试。
 */
class OrderNumberGenerator(
    private val now: () -> LocalDateTime = LocalDateTime::now,
) {
    fun next(seq: Int): String {
        val t = now()
        val date = "%04d%02d%02d".format(t.year, t.monthValue, t.dayOfMonth)
        return "YQ-$date-${"%04d".format(seq)}"
    }
}
