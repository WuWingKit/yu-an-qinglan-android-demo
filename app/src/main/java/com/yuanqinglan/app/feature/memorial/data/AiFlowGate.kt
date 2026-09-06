/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AI 追忆流程门（进程内会话状态）。
 *
 * - [consented]：本会话是否已完成伦理前置确认；未确认前素材工作台不可进入生成流程；
 * - [pendingMemorialId]：从“AI 追忆入口”（始终携带纪念空间 ID）进入伦理页时的目标 ID，
 *   确认后由伦理页取出并导航到 ai-upload/{id}，避免无参路由丢失目标；
 * - 不同意/返回时不改变 [consented]，伦理页不可跳过。
 */
object AiFlowGate {

    private val _consented = MutableStateFlow(false)
    val consented: StateFlow<Boolean> = _consented.asStateFlow()

    private val _pendingMemorialId = MutableStateFlow<String?>(null)
    val pendingMemorialId: StateFlow<String?> = _pendingMemorialId.asStateFlow()

    /** 进入伦理前置页前登记目标纪念空间（仅 AI 入口调用）。 */
    fun prepare(memorialId: String) {
        require(memorialId.isNotBlank()) { "AI 追忆入口必须携带纪念空间 ID" }
        _pendingMemorialId.value = memorialId
    }

    /** 用户勾选并点击同意后调用（不可跳过）。 */
    fun grantConsent() {
        _consented.value = true
    }

    /** 取出并清空待进入的纪念空间 ID；无目标返回 null。 */
    fun consumePending(): String? {
        val pending = _pendingMemorialId.value
        _pendingMemorialId.value = null
        return pending
    }

    /** 本会话重置（一般不需调用；保留给整体流程重置）。 */
    fun resetSession() {
        _consented.value = false
        _pendingMemorialId.value = null
    }
}

/** 伦理前置页的本地规则：必须勾选“已阅读并知晓”才能继续（不可跳过）。 */
object EthicsGateRules {
    /** [readConfirmed] 为用户勾选状态；未勾选一律不允许继续。 */
    fun mayProceed(readConfirmed: Boolean): Boolean = readConfirmed
}
