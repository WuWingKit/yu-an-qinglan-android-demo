/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.policy

import com.yuanqinglan.app.feature.policy.logic.CountyIndex
import com.yuanqinglan.app.feature.policy.model.County
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 区县检索 / 标题清洗逻辑测试。 */
class CountyIndexTest {

    private val counties = listOf(
        County("cq-yuzhong", "渝中区", "中心城区", "简介", "政策摘要", "办理提示"),
        County("cq-yubei", "渝北区", "中心城区", "简介", "政策摘要", "办理提示"),
        County("cq-wanzhou", "万州区", "渝东北", "简介", "政策摘要", "办理提示"),
        County("cq-xiushan", "秀山土家族苗族自治县", "渝东南", "简介", "政策摘要", "办理提示"),
    )

    @Test
    fun `blank query returns all counties`() {
        assertEquals(4, CountyIndex.search(counties, "").size)
        assertEquals(4, CountyIndex.search(counties, "  ").size)
    }

    @Test
    fun `search by county name`() {
        val results = CountyIndex.search(counties, "渝北")
        assertEquals(listOf("cq-yubei"), results.map { it.id })
    }

    @Test
    fun `search by zone name`() {
        val results = CountyIndex.search(counties, "渝东南")
        assertEquals(listOf("cq-xiushan"), results.map { it.id })
    }

    @Test
    fun `search by id keyword is case insensitive`() {
        val results = CountyIndex.search(counties, "WANZHOU")
        assertEquals(listOf("cq-wanzhou"), results.map { it.id })
    }

    @Test
    fun `search with no match returns empty`() {
        assertEquals(0, CountyIndex.search(counties, "不存在的区县").size)
    }

    @Test
    fun `find by id works and missing id returns null`() {
        assertEquals("cq-yuzhong", CountyIndex.findById(counties, "cq-yuzhong")?.id)
        assertNull(CountyIndex.findById(counties, "cq-missing"))
    }

    @Test
    fun `cleanTitle strips dirty chars from demo titles`() {
        assertEquals("渝中区", CountyIndex.cleanTitle(">渝中区"))
        assertEquals("渝中区", CountyIndex.cleanTitle(">\"渝中区\""))
        assertEquals("沙坪坝区", CountyIndex.cleanTitle(">沙坪坝区"))
        assertEquals("区县详情", CountyIndex.cleanTitle("  >区县详情  "))
    }

    @Test
    fun `cleanTitle keeps clean titles untouched`() {
        assertEquals("渝北区", CountyIndex.cleanTitle("渝北区"))
        assertEquals("万州区", CountyIndex.cleanTitle("万州区"))
    }
}
