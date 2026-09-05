/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.policy

import com.yuanqinglan.app.data.local.AppJson
import com.yuanqinglan.app.feature.policy.model.County
import java.io.File
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 区县数据完整性测试：读取 assets/demo/policy/counties.json（与运行时
 * DemoAssetLoader 相同的解码路径），校验 38 个区县、无重复 id/名称、覆盖全部名单。
 */
class CountyDataTest {

    private fun loadCountiesFromAsset(): List<County> {
        val file = File("src/main/assets/demo/policy/counties.json")
        assertTrue("找不到 counties.json（cwd=${File(".").absolutePath}）", file.exists())
        return AppJson.decodeFromString(
            ListSerializer(County.serializer()),
            file.readText(Charsets.UTF_8),
        )
    }

    @Test
    fun `counties json has exactly 38 unique counties`() {
        val counties = loadCountiesFromAsset()
        assertEquals("区县数量应为 38", 38, counties.size)
        assertEquals(38, counties.map { it.id }.toSet().size)
        assertEquals(38, counties.map { it.name }.toSet().size)
    }

    @Test
    fun `county list matches the full audited 38 county names`() {
        val expected = listOf(
            "渝中区", "大渡口区", "江北区", "沙坪坝区", "九龙坡区", "南岸区", "北碚区", "渝北区", "巴南区",
            "涪陵区", "长寿区", "江津区", "合川区", "永川区", "南川区", "綦江区", "大足区", "璧山区",
            "铜梁区", "潼南区", "荣昌区", "开州区", "梁平区", "万州区", "黔江区", "武隆区",
            "城口县", "丰都县", "垫江县", "忠县", "云阳县", "奉节县", "巫山县", "巫溪县",
            "石柱土家族自治县", "秀山土家族苗族自治县", "酉阳土家族苗族自治县", "彭水苗族土家族自治县",
        )
        val actual = loadCountiesFromAsset().map { it.name }
        assertEquals(expected.sorted(), actual.sorted())
        assertEquals(expected.toSet(), actual.toSet())
    }

    @Test
    fun `county zones follow the 9-12-11-6 partition`() {
        val zones = loadCountiesFromAsset().groupingBy { it.zone }.eachCount()
        assertEquals(9, zones["中心城区"])
        assertEquals(12, zones["主城新区"])
        assertEquals(11, zones["渝东北"])
        assertEquals(6, zones["渝东南"])
    }

    @Test
    fun `every county carries zone brief policy and tips text`() {
        val counties = loadCountiesFromAsset()
        counties.forEach { county ->
            assertTrue("${county.name} 缺少片区", county.zone.isNotBlank())
            assertTrue("${county.name} 缺少简介", county.brief.isNotBlank())
            assertTrue("${county.name} 缺少政策摘要", county.policySummary.isNotBlank())
            assertTrue("${county.name} 缺少办理提示", county.processTips.isNotBlank())
        }
    }

    @Test
    fun `county texts contain no banned wording and no dirty title chars`() {
        val banned = listOf("演示", "假数据", "原型", "纯前端")
        val counties = loadCountiesFromAsset()
        counties.forEach { county ->
            val text = listOf(county.name, county.zone, county.brief, county.policySummary, county.processTips)
                .joinToString("")
            banned.forEach { word ->
                assertTrue("${county.id} 出现禁用文案[$word]", !text.contains(word))
            }
            assertTrue("${county.id} 名称不应含脏字符", !county.name.contains('>'))
        }
    }
}
