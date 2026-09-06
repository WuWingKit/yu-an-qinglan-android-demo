/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.profile.model

import kotlinx.serialization.Serializable

/** 素材类别：本地图片 / 本地音频。 */
enum class ProfileMaterialKind {
    IMAGE,
    AUDIO,
}

/**
 * 本地素材索引项（只记索引与私有目录 Uri，不落文件内容）。
 * 由个人中心“素材管理”维护；删除即同时删除私有文件。
 */
@Serializable
data class LocalMaterialEntry(
    val id: String,
    val kind: ProfileMaterialKind,
    val uri: String,
    val name: String,
    val createdAtMillis: Long,
)

/** 意见反馈类型（本地表单选择）。 */
enum class FeedbackType(val label: String) {
    FUNCTION("功能建议"),
    CONTENT("内容问题"),
    SERVICE("服务体验"),
    OTHER("其他"),
}

/**
 * 已提交的本地反馈记录（仅本机保存，不对外传输）。
 * [attachmentUris] 为附件复制到私有目录后的 file uri 列表。
 */
@Serializable
data class FeedbackRecord(
    val id: String,
    val typeLabel: String,
    val body: String,
    val attachmentUris: List<String> = emptyList(),
    val submittedAtMillis: Long,
)
