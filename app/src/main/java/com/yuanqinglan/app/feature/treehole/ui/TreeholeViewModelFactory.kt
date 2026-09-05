/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.treehole.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.yuanqinglan.app.feature.treehole.data.TreeholePool
import com.yuanqinglan.app.feature.treehole.model.TreeholeLetterLike

/**
 * [TreeholePoolViewModel] 的小工厂：入参为 Application 与具体内容池实例，
 * 每个池页面（人间/生灵）各用一个工厂实例，保证两个页面 ViewModel 互不串池。
 * 模式参考 androidx.lifecycle.ViewModelProvider.Factory。
 */
class TreeholeViewModelFactory(
    private val application: Application,
    private val pool: TreeholePool<out TreeholeLetterLike>,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TreeholePoolViewModel::class.java)) {
            return TreeholePoolViewModel(application, pool) as T
        }
        throw IllegalArgumentException("未知的 ViewModel 类型：${modelClass.name}")
    }
}
