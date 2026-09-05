/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.home.data

import com.yuanqinglan.app.data.local.DemoAssetLoader
import com.yuanqinglan.app.feature.home.model.ActivityEvent
import com.yuanqinglan.app.feature.home.model.LifeEdCourse
import com.yuanqinglan.app.feature.home.model.NewsArticle
import kotlinx.serialization.builtins.ListSerializer

/** 首页内容数据源契约（便于单测注入假实现）。 */
interface HomeCatalogSource {
    suspend fun loadNews(): List<NewsArticle>
    suspend fun loadLifeEdCourses(): List<LifeEdCourse>
    suspend fun loadActivities(): List<ActivityEvent>
}

/**
 * 首页内容数据源默认实现：经公共 DemoAssetLoader 读取 assets/demo/home
 * 目录下的 JSON 文件。读取失败抛 IOException / SerializationException，
 * 由 ViewModel 转为失败态。
 */
class AssetHomeCatalogSource(private val loader: DemoAssetLoader) : HomeCatalogSource {

    private val newsSerializer = ListSerializer(NewsArticle.serializer())
    private val lifeEdSerializer = ListSerializer(LifeEdCourse.serializer())
    private val activitiesSerializer = ListSerializer(ActivityEvent.serializer())

    override suspend fun loadNews(): List<NewsArticle> =
        loader.load("home/news.json", newsSerializer)

    override suspend fun loadLifeEdCourses(): List<LifeEdCourse> =
        loader.load("home/life-ed.json", lifeEdSerializer)

    override suspend fun loadActivities(): List<ActivityEvent> =
        loader.load("home/activities.json", activitiesSerializer)
}
