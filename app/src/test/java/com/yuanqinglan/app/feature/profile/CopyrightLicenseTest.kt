/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.profile

import com.yuanqinglan.app.R
import com.yuanqinglan.app.feature.profile.ui.CopyrightLicense
import com.yuanqinglan.app.feature.profile.ui.authorizationPages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 版权与授权静态内容一致性测试：与仓库根 LICENSE 及授权公告对齐，防文案回归。
 * 关键字段（权利人 / 被授权对象 / 赛事范围 / 联系邮箱）与中英双页授权书资源顺序。
 */
class CopyrightLicenseTest {

    @Test
    fun licenseFacts_matchRootLicense() {
        assertTrue(CopyrightLicense.Owner.contains("胡荣杰"))
        assertTrue(CopyrightLicense.Owner.contains("WuWingKit"))
        assertEquals("西南大学经济管理学院李芸凤", CopyrightLicense.Licensee)
        assertTrue(CopyrightLicense.Competition.contains("2026"))
        assertEquals("hurongjie@qianban.online", CopyrightLicense.ContactEmail)
    }

    @Test
    fun viewer_hasTwoSignedPages_zhThenEn() {
        assertEquals(2, authorizationPages.size)
        assertEquals(R.drawable.authorization_li_yunfeng_zh, authorizationPages[0])
        assertEquals(R.drawable.authorization_li_yunfeng_en, authorizationPages[1])
    }
}
