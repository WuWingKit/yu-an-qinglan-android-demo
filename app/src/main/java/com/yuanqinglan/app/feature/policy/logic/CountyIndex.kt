/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.policy.logic

import com.yuanqinglan.app.feature.policy.model.County

/** 区县检索与标题清洗（纯逻辑，便于单元测试）。 */
object CountyIndex {

    /**
     * 按关键词过滤区县：匹配区县名称或片区名称；空白关键词返回原列表。
     * 关键词两侧空白会被忽略，匹配不区分大小写（用于拼音/字母 id 检索）。
     */
    fun search(items: List<County>, query: String): List<County> {
        val keyword = query.trim()
        if (keyword.isEmpty()) return items
        val lower = keyword.lowercase()
        return items.filter { county ->
            county.name.contains(keyword) ||
                county.name.lowercase().contains(lower) ||
                county.zone.contains(keyword) ||
                county.id.lowercase().contains(lower)
        }
    }

    /** 按 id 查找区县。 */
    fun findById(items: List<County>, id: String): County? =
        items.firstOrNull { it.id == id }

    /**
     * 标题清洗：去掉网页残留的脏字符（`>`、`<`、引号等）。
     * 参考 Demo 中 county-detail 标题残留 `">` 前缀的缺陷，本实现不复制。
     */
    fun cleanTitle(raw: String): String =
        raw.filterNot { it == '>' || it == '<' || it == '"' || it == '\'' || it == '`' }
            .trim()
}
