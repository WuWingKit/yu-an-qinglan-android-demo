/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.treehole.data

import com.yuanqinglan.app.feature.treehole.model.HumanLetter
import com.yuanqinglan.app.feature.treehole.model.PetLetter
import kotlinx.serialization.Serializable

/*
 * assets/demo/treehole 内置信件的强类型 DTO。
 * 人间（human-letters.json）与生灵（pet-letters.json）两套文件与 DTO 完全独立。
 */

@Serializable
internal data class HumanLettersFile(val letters: List<HumanLetter>)

@Serializable
internal data class PetLettersFile(val letters: List<PetLetter>)
