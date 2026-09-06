/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.data.local

import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [AppJson] 解码配置测试：验证 ignoreUnknownKeys 容错与默认值语义，
 * 保证 assets/demo JSON 的结构演进不会破坏旧字段解析。
 */
class AppJsonTest {

    @Serializable
    private data class Sample(
        val id: String,
        val title: String,
        val note: String? = null,
    )

    @Test
    fun decodeIgnoresUnknownKeysAndFillsDefaults() {
        val raw = """{"id":"a-1","title":"示例","extraField":123,"ignored":true}"""
        val decoded: Sample = AppJson.decodeFromString(Sample.serializer(), raw)

        assertEquals("a-1", decoded.id)
        assertEquals("示例", decoded.title)
        assertNull(decoded.note)
    }
}
