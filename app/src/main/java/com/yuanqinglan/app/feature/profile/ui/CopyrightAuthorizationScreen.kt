/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.profile.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.ReferenceNote

/**
 * 版权与授权静态内容（与仓库根 LICENSE 及 GitHub 授权公告保持一致）。
 * 仅随 APK 内置的本地常量：不写日志、不参与分析事件、不落外部存储。
 */
object CopyrightLicense {
    /** 版权所有者（权利人）。 */
    const val Owner = "西南大学24级学行科创班胡荣杰（WuWingKit）"

    /** 被授权对象。 */
    const val Licensee = "西南大学经济管理学院李芸凤"

    /** App 内仅展示这一句授权摘要，详细范围以签名授权书为准。 */
    const val AuthorizationSummary = "本软件使用权已经授权给西南大学经济管理学院李芸凤。"

    /** 书面授权联系邮箱。 */
    const val ContactEmail = "hurongjie@qianban.online"
}

/**
 * 版权及授权详情页：完整呈现专有 License 的版权边界；授权情况仅展示
 * 被授权对象，并提供已签名授权书查看器入口。
 * 页面采用克制排版（标题 + 正文），不使用营销式卡片或突出横幅。
 */
@Composable
fun CopyrightAuthorizationScreen(onBack: () -> Unit) {
    var showViewer by rememberSaveable { mutableStateOf(false) }

    AppScaffold(title = "版权及授权情况", onBack = onBack) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
        ) {
            Spacer(Modifier.size(14.dp))
            LicenseSection(
                title = "版权所有者",
                body = "渝安青澜为专有软件，原创代码、界面、文档与素材的权利归 ${CopyrightLicense.Owner} 所有，保留所有权利。",
            )
            LicenseSection(
                title = "许可边界",
                body = "本仓库公开、可访问或可下载，不构成任何明示或默示许可。未经权利人事先明确的书面授权，任何个人或组织不得以商业、非商业、个人、教育、研究、竞赛、公益、展示或其他目的利用本项目的全部或任何部分。",
            )
            LicenseSection(
                title = "禁止行为",
                body = "未经授权禁止运行、使用、复制、下载后使用、修改、改编、翻译、反编译、反汇编、逆向工程、摘编、合并、制作衍生作品、训练或评估模型、部署、托管、公开展示、传播、发行、再许可、出租、出售、提供服务，以及据此开发、宣传或交付产品或成果。",
            )
            LicenseSection(
                title = "书面授权与平台权限",
                body = "授权须由权利人以书面文件或由 ${CopyrightLicense.ContactEmail} 发出的明确邮件作出，并载明获授权主体、内容、范围与期限；未明确授予的权利仍由权利人保留。GitHub 用户仅保有服务条款为浏览、展示和 Fork 所必需的平台权限，不因此取得额外的使用、修改、部署、传播或商业化权利。",
            )
            LicenseSection(
                title = "第三方内容与责任",
                body = "第三方软件、依赖、字体、图标和素材分别适用其原始许可。任何未经授权的利用均可能构成侵权，权利人有权要求停止使用、删除副本并依法追究责任。本项目按“现状”提供，在法律允许的最大范围内不附带任何明示或默示保证。",
            )

            LicenseSection(
                title = "授权情况",
                body = CopyrightLicense.AuthorizationSummary,
            )

            Spacer(Modifier.size(10.dp))
            TextButton(onClick = { showViewer = true }) {
                Text("查看软件使用授权书")
            }

            Spacer(Modifier.size(12.dp))
            ReferenceNote(
                text = "版权条款沿用仓库根 LICENSE；具体授权以软件使用授权书为准。",
            )
            ProfileBottomSpace()
        }
    }

    if (showViewer) {
        AuthorizationViewerDialog(onDismiss = { showViewer = false })
    }
}

/** 详情页小节：标题 + 正文（克制排版，无卡片背景）。 */
@Composable
private fun LicenseSection(title: String, body: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary,
        modifier = Modifier.padding(top = 14.dp),
    )
    Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
        modifier = Modifier.padding(top = 5.dp),
    )
}
