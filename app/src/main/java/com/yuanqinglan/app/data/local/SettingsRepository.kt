/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.data.local

import kotlinx.coroutines.flow.Flow

/**
 * 本地设置仓库契约：全部异步（Flow / suspend），供全局主题、首页右上角开关、
 * 个人中心与隐私设置共用，数据持久化于 DataStore。
 *
 * 默认值：老年模式关闭、树洞开启、昵称"渝安青澜用户"、无头像、隐私声明未确认。
 */
interface SettingsRepository {
    /** 老年模式（适老模式）开关。 */
    val elderMode: Flow<Boolean>
    suspend fun setElderMode(enabled: Boolean)

    /** 心灵树洞功能总开关。 */
    val treeholeEnabled: Flow<Boolean>
    suspend fun setTreeholeEnabled(enabled: Boolean)

    /** 本地昵称（1-12 字，调用方负责校验长度）。 */
    val nickname: Flow<String>
    suspend fun setNickname(nickname: String)

    /** 本地头像文件 URI（可空，空表示未设置）。 */
    val avatarUri: Flow<String?>
    suspend fun setAvatarUri(uri: String?)

    /** 隐私政策/声明是否已确认。 */
    val privacyAccepted: Flow<Boolean>
    suspend fun setPrivacyAccepted(accepted: Boolean)

    /** 恢复全部默认值（供个人中心重置）。 */
    suspend fun resetAll()
}
