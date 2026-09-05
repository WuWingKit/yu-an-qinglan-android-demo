/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 生命故事纪念册导出文本构建（纯函数，可单测）。
 *
 * 导出必须包含“全部已保存节点”——调用方传入排序后的完整节点列表
 * （含用户新增节点），本函数不做任何过滤。文本写入应用私有目录文件。
 */
object StoryAlbumExport {

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日")

    fun build(spaceName: String, stories: List<MemorialStory>): String {
        val sorted = stories.sortedBy { it.dateMillis }
        val today = LocalDate.now().format(dateFormatter)
        val body = buildString {
            append("《").append(spaceName).append("的生命故事》").append("\n")
            append("共 ").append(sorted.size).append(" 个节点 · 整理于 ").append(today).append("\n")
            append("本纪念册由家人整理，仅供私人纪念。\n")
            if (sorted.isEmpty()) {
                append("\n（暂无故事节点）\n")
            } else {
                sorted.forEachIndexed { index, story ->
                    append("\n—— 节点 ").append(index + 1).append(" ——\n")
                    append("标题：").append(story.title).append("\n")
                    append("时间：").append(story.dateText).append("\n")
                    append("\n").append(story.body).append("\n")
                }
            }
        }
        return body
    }
}
