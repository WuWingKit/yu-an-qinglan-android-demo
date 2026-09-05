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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yuanqinglan.app.core.designsystem.QingLanGreenDark
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.NoticeBanner
import com.yuanqinglan.app.core.ui.NoticeTone
import com.yuanqinglan.app.core.ui.ReferenceNote

/** 关于渝安青澜：产品定位与合规说明。页面不展示负责人、赛事与版本信息。 */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    AppScaffold(title = "关于渝安青澜", onBack = onBack) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
        ) {
            Spacer(Modifier.size(16.dp))
            Text(
                text = "渝安青澜",
                style = MaterialTheme.typography.headlineMedium,
                color = QingLanGreenDark,
            )
            Text(
                text = "让告别回归自然，让思念有所安放",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                modifier = Modifier.padding(top = 6.dp),
            )

            Spacer(Modifier.size(18.dp))
            NoticeBanner(
                text = "本应用提供生态殡葬相关信息的本地参考与流程记录服务。",
                tone = NoticeTone.COMPLIANCE,
            )

            Spacer(Modifier.size(14.dp))
            Text("产品定位", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Spacer(Modifier.size(6.dp))
            AboutParagraph(
                "渝安青澜面向生态安葬相关的信息查询与服务流程参考，聚合政策资讯、生态葬式介绍、" +
                    "纪念空间与树洞倾诉等本地功能，帮助使用者了解服务并留存私人纪念内容。",
            )
            AboutParagraph(
                "应用内所有个人资料、头像、图片、音频与纪念内容均只保存在本机私有目录，不对外传输，也不参与任何自动提交。",
            )

            Spacer(Modifier.size(12.dp))
            Text("数据与隐私", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Spacer(Modifier.size(6.dp))
            AboutParagraph(
                "浏览偏好、隐私开关与账号相关校验均记录在本机；相机、相册、麦克风等能力仅用于本地选择与录音。",
            )
            AboutParagraph(
                "政策、价格、补贴、预约与办理结果相关内容仅供参考，具体政策、费用与办理结果以主管机构和服务机构最终公布为准。",
            )

            Spacer(Modifier.size(16.dp))
            ReferenceNote(
                text = "相关信息仅供参考，具体政策、费用与办理结果以主管机构和服务机构最终公布为准。",
            )
            ProfileBottomSpace()
        }
    }
}

@Composable
private fun AboutParagraph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
        modifier = Modifier.padding(top = 6.dp),
    )
}
