/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.policy

import com.yuanqinglan.app.data.local.AppJson
import com.yuanqinglan.app.feature.policy.model.PolicyArticle
import com.yuanqinglan.app.feature.policy.model.PolicyLevel
import com.yuanqinglan.app.feature.policy.model.SeaGuide
import java.io.File
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** policy.json / sea.json 数据完整性测试（解码路径与运行时一致）。 */
class PolicyAssetJsonTest {

    private fun readAssetJson(name: String): String {
        val file = File("src/main/assets/demo/policy/$name")
        assertTrue("找不到 $name（cwd=${File(".").absolutePath}）", file.exists())
        return file.readText(Charsets.UTF_8)
    }

    private val bannedWords = listOf("演示", "假数据", "原型", "纯前端")

    @Test
    fun `policy json parses unique entries across all levels`() {
        val text = readAssetJson("policy.json")
        bannedWords.forEach { word -> assertTrue("policy.json 出现禁用文案[$word]", !text.contains(word)) }

        val policies: List<PolicyArticle> = AppJson.decodeFromString(
            ListSerializer(PolicyArticle.serializer()),
            text,
        )
        assertEquals(8, policies.size)
        assertEquals(8, policies.map { it.id }.toSet().size)
        policies.forEach { article ->
            assertTrue("${article.id} 标题为空", article.title.isNotBlank())
            assertTrue("${article.id} 摘要为空", article.summary.isNotBlank())
        }
        val levels = policies.map { it.level }.toSet()
        assertTrue(levels.containsAll(setOf(PolicyLevel.NATIONAL, PolicyLevel.CITY)))
        assertTrue(levels.contains(PolicyLevel.TIP) || levels.contains(PolicyLevel.NOTICE))
    }

    @Test
    fun `sea json contains flow apply and compliance sections`() {
        val text = readAssetJson("sea.json")
        bannedWords.forEach { word -> assertTrue("sea.json 出现禁用文案[$word]", !text.contains(word)) }

        val sea: SeaGuide = AppJson.decodeFromString(SeaGuide.serializer(), text)
        assertTrue(sea.title.isNotBlank())
        assertTrue(sea.introParagraphs.size >= 2)
        assertTrue(sea.flowSteps.size >= 4)
        sea.flowSteps.forEach { step ->
            assertTrue(step.title.isNotBlank())
            assertTrue(step.detail.isNotBlank())
        }
        assertTrue(sea.applyParagraph.isNotBlank())
        assertTrue(sea.notices.isNotEmpty())
        assertTrue(sea.complianceNote.contains("仅供参考"))
    }
}
