/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial.data

import android.content.Context
import com.yuanqinglan.app.data.local.DemoAssetLoader

/**
 * 安葬模块的进程级服务定位器：懒加载单例 [BurialRepository]。
 *
 * 依赖公共数据层 [DemoAssetLoader]（foundation_navigation 实现，构造方式见仓库说明），
 * 若公共层最终签名与本处假设不一致，只需修改本文件单点即可。
 */
object BurialServiceLocator {

    @Volatile
    private var instance: BurialRepository? = null

    fun repository(context: Context): BurialRepository {
        val appContext = context.applicationContext
        return instance ?: synchronized(this) {
            instance ?: BurialRepository(loader = DemoAssetLoader(appContext)).also { instance = it }
        }
    }
}
