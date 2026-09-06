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

    /** 授权用途限定的赛事范围。 */
    const val Competition = "2026年重庆市大学生新文科实践创新大赛"

    /** 书面授权联系邮箱。 */
    const val ContactEmail = "hurongjie@qianban.online"
}

/**
 * 版权及授权详情页：版权所有者、专有 License 边界、被授权对象、授权用途、
 * 修改限制与有效性、联系邮箱，并提供已签名授权书查看器入口。
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
                body = "本应用为专有软件，未授予许可。未经权利人书面授权，任何个人或组织不得以任何目的使用本应用的全部或任何部分（含商业与非商业）。未经授权禁止运行、复制、修改、改编、翻译、反编译、逆向工程、制作衍生作品、训练模型、部署、托管、传播、发行、再许可、出租、出售或提供服务。",
            )
            LicenseSection(title = "被授权对象", body = CopyrightLicense.Licensee)
            LicenseSection(
                title = "授权用途",
                body = "仅限 ${CopyrightLicense.Competition} 的非商业用途；超出该范围的使用须另行取得书面授权。",
            )
            LicenseSection(
                title = "修改限制",
                body = "未经书面授权不得二次修改或改编源码；未明确授予的权利仍由权利人保留。",
            )
            LicenseSection(
                title = "有效性与联系",
                body = "任何授权须由权利人书面文件或官方邮箱明确邮件作出，并载明获授权主体、内容、范围与期限；口头陈述、Issue、Pull Request、评论或未答复的请求均不构成授权。联系邮箱：${CopyrightLicense.ContactEmail}",
            )

            Spacer(Modifier.size(10.dp))
            TextButton(onClick = { showViewer = true }) {
                Text("查看软件使用授权书")
            }

            Spacer(Modifier.size(12.dp))
            ReferenceNote(
                text = "本页版权与授权信息与仓库根 LICENSE 及 GitHub 授权公告一致，具体授权以书面文件为准。",
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
