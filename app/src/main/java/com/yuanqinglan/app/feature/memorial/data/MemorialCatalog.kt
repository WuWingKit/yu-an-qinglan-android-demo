/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.data

import com.yuanqinglan.app.feature.memorial.model.CollectiveActivity
import com.yuanqinglan.app.feature.memorial.model.DaijiPackage
import com.yuanqinglan.app.feature.memorial.model.HumanMemorial
import com.yuanqinglan.app.feature.memorial.model.PetMemorial
import kotlinx.serialization.Serializable

/*
 * assets/demo/memorial 内置 JSON 的强类型 DTO。
 * 人类/宠物纪念内容使用两套完全独立的文件与 DTO，任何解析都不会把两轨混入同一列表。
 */

@Serializable
internal data class HumanMemorialsFile(val memorials: List<HumanMemorial>)

@Serializable
internal data class PetMemorialsFile(val memorials: List<PetMemorial>)

/** 异地代祭套餐目录（与集体共祭活动目录独立）。 */
@Serializable
internal data class DaijiPackagesFile(val packages: List<DaijiPackage>)

/** 线上集体共祭活动目录（免费公益活动）。 */
@Serializable
internal data class CollectiveActivitiesFile(val activities: List<CollectiveActivity>)
