/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial.ui

import androidx.compose.ui.graphics.Color
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 园区导览图例/点位辨识度测试：
 * 1) 出入口颜色与所有类型明显区分（色距阈值）；
 * 2) 出入口不单靠颜色：携带图标标识、圆角方形形状与可断言语义；
 * 3) 图例覆盖全部类型，且人宠分区隔离表达不弱化。
 */
class ParkNavigateLegendTest {

    @Test
    fun `出入口颜色与所有类型明显区分`() {
        val entry = ParkPointType.ENTRY.color
        ParkPointType.entries.filter { it != ParkPointType.ENTRY }.forEach { other ->
            val d = colorDistance(entry, other.color)
            assertTrue("ENTRY 与 ${other.label} 色距过小：$d", d >= 90f)
        }
    }

    @Test
    fun `仅出入口携带图标标识其余类型不单靠颜色`() {
        assertEquals("door", ParkPointType.ENTRY.iconKey)
        ParkPointType.entries.filter { it != ParkPointType.ENTRY }.forEach { type ->
            assertNull("${type.label} 不应有图标标识", type.iconKey)
        }
    }

    @Test
    fun `出入口语义描述含图标标识且不以颜色区分`() {
        val gate = PARK_POINTS.first { it.id == "gate" }
        val label = parkMarkerSemanticsLabel(gate, 1)
        assertTrue("应含图标标识：$label", label.contains("图标标识"))
        assertTrue("不应以颜色作为区分描述：$label", !label.contains("颜色"))
        assertTrue("应含编号：$label", label.contains("第 1 号点位"))
    }

    @Test
    fun `其他点位语义描述含编号与类型名`() {
        val tree = PARK_POINTS.first { it.id == "tree-zone" }
        val label = parkMarkerSemanticsLabel(tree, 3)
        assertTrue("应含编号：$label", label.contains("第 3 号点位"))
        assertTrue("应含类型名：$label", label.contains("人类安葬区"))
        assertTrue("非出入口不应标注图标标识", !label.contains("图标标识"))
    }

    @Test
    fun `图例覆盖全部类型且人宠分区表达保留`() {
        assertEquals(ParkPointType.entries.toSet(), PARK_LEGEND.toSet())
        val pet = PARK_POINTS.first { it.id == "pet-zone" }
        assertEquals(ParkPointType.PET_ZONE, pet.type)
        assertTrue("人宠隔离表达不应被弱化", pet.description.contains("隔离"))
        assertTrue("人类安葬区点位应不少于 3 个", PARK_POINTS.count { it.type == ParkPointType.HUMAN_ZONE } >= 3)
        assertTrue("宠物独立园区点位应保留", PARK_POINTS.count { it.type == ParkPointType.PET_ZONE } >= 1)
    }

    @Test
    fun `点位坐标均在底图范围内`() {
        PARK_POINTS.forEach { p ->
            assertTrue("${p.id} x 越界：${p.x}", p.x in 0f..1f)
            assertTrue("${p.id} y 越界：${p.y}", p.y in 0f..1f)
        }
    }
}

/** 线性 RGB 空间欧氏色距（0..255 尺度），阈值用于证明出入口与其他类型颜色明显区分。 */
private fun colorDistance(a: Color, b: Color): Float {
    val dr = (a.red - b.red) * 255f
    val dg = (a.green - b.green) * 255f
    val db = (a.blue - b.blue) * 255f
    return sqrt(dr * dr + dg * dg + db * db)
}
