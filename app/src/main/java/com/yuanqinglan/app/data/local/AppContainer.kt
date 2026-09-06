/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.data.local

import android.content.Context

/**
 * 简单 ServiceLocator：懒加载各数据层入口，向 feature 提供统一获取点。
 * 必须在访问任何成员之前调用 [init]（MainActivity/Application 启动时传入
 * Application Context）。不引入依赖注入框架。
 */
object AppContainer {

    private const val ERROR_NOT_INITIALIZED =
        "AppContainer 尚未初始化：请先在 Application/MainActivity 中调用 AppContainer.init(context)"

    @Volatile
    private var appContext: Context? = null

    private val context: Context
        get() = checkNotNull(appContext) { ERROR_NOT_INITIALIZED }

    /** 初始化（幂等）：多次调用仅首次生效。需在 Application/Activity 启动时调用。 */
    fun init(applicationContext: Context) {
        if (appContext == null) {
            synchronized(this) {
                if (appContext == null) {
                    appContext = applicationContext.applicationContext
                }
            }
        }
    }

    val isInitialized: Boolean
        get() = appContext != null

    /** 本地设置仓库（老年模式/树洞开关/昵称/头像/隐私确认）。 */
    val settings: SettingsRepository by lazy { DataStoreSettingsRepository(context) }

    /** assets/demo JSON 加载器。 */
    val demoAssets: DemoAssetLoader by lazy { DemoAssetLoader(context) }

    /** 应用私有目录文件存储（头像/相册/日记/树洞附件）。 */
    val fileStorage: FileStorage by lazy { FileStorage(context) }
}
