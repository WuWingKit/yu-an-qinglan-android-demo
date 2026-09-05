/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.policy.data

import com.yuanqinglan.app.data.local.DemoAssetLoader
import com.yuanqinglan.app.feature.policy.model.County
import com.yuanqinglan.app.feature.policy.model.PolicyArticle
import com.yuanqinglan.app.feature.policy.model.SeaGuide
import kotlinx.serialization.builtins.ListSerializer

/** 政策链路数据源契约（便于单测注入假实现）。 */
interface PolicyCatalogSource {
    suspend fun loadPolicies(): List<PolicyArticle>
    suspend fun loadCounties(): List<County>
    suspend fun loadSeaGuide(): SeaGuide
}

/** 默认实现：经公共 DemoAssetLoader 读取 assets/demo/policy 目录下的 JSON 文件。 */
class AssetPolicyCatalogSource(private val loader: DemoAssetLoader) : PolicyCatalogSource {

    private val policySerializer = ListSerializer(PolicyArticle.serializer())
    private val countySerializer = ListSerializer(County.serializer())

    override suspend fun loadPolicies(): List<PolicyArticle> =
        loader.load("policy/policy.json", policySerializer)

    override suspend fun loadCounties(): List<County> =
        loader.load("policy/counties.json", countySerializer)

    override suspend fun loadSeaGuide(): SeaGuide =
        loader.load("policy/sea.json", SeaGuide.serializer())
}
