/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.treehole

import com.yuanqinglan.app.feature.treehole.model.TreeholeAttachmentLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 附件上限边界（纯 JVM）：
 * 图片 ≤10MB、音频 ≤5MB；恰好等于上限允许，超过 1 字节即拒绝并给出明确提示。
 */
class TreeholeLimitsTest {

    @Test
    fun `图片恰好 10MB 允许`() {
        assertEquals(10L * 1024L * 1024L, TreeholeAttachmentLimits.MAX_IMAGE_BYTES)
        assertNull(TreeholeAttachmentLimits.imageErrorIfAny(TreeholeAttachmentLimits.MAX_IMAGE_BYTES))
    }

    @Test
    fun `图片超过 10MB 拒绝且提示明确`() {
        val over = TreeholeAttachmentLimits.MAX_IMAGE_BYTES + 1L
        val error = TreeholeAttachmentLimits.imageErrorIfAny(over)
        assertNotNull(error)
        assertEquals("图片不能超过 10MB，当前文件已超限", error)
    }

    @Test
    fun `图片小于上限允许`() {
        assertNull(TreeholeAttachmentLimits.imageErrorIfAny(0L))
        assertNull(TreeholeAttachmentLimits.imageErrorIfAny(1024L))
        assertNull(TreeholeAttachmentLimits.imageErrorIfAny(TreeholeAttachmentLimits.MAX_IMAGE_BYTES - 1L))
    }

    @Test
    fun `音频恰好 5MB 允许`() {
        assertEquals(5L * 1024L * 1024L, TreeholeAttachmentLimits.MAX_AUDIO_BYTES)
        assertNull(TreeholeAttachmentLimits.audioErrorIfAny(TreeholeAttachmentLimits.MAX_AUDIO_BYTES))
    }

    @Test
    fun `音频超过 5MB 拒绝且提示明确`() {
        val over = TreeholeAttachmentLimits.MAX_AUDIO_BYTES + 1L
        val error = TreeholeAttachmentLimits.audioErrorIfAny(over)
        assertNotNull(error)
        assertEquals("音频不能超过 5MB，当前文件已超限", error)
    }

    @Test
    fun `音频小于上限允许`() {
        assertNull(TreeholeAttachmentLimits.audioErrorIfAny(0L))
        assertNull(TreeholeAttachmentLimits.audioErrorIfAny(64L * 1024L))
        assertNull(TreeholeAttachmentLimits.audioErrorIfAny(TreeholeAttachmentLimits.MAX_AUDIO_BYTES - 1L))
    }

    @Test
    fun `图片与音频上限互不混用`() {
        // 图片按 10MB 校验，超过 5MB 但未到 10MB 不应被音频规则拒绝
        assertNull(TreeholeAttachmentLimits.imageErrorIfAny(6L * 1024L * 1024L))
        // 音频按 5MB 校验，超过 5MB 即拒绝（哪怕未到图片上限）
        assertNotNull(TreeholeAttachmentLimits.audioErrorIfAny(6L * 1024L * 1024L))
    }
}
