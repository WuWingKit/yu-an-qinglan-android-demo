/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yuanqinglan.app.R
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.ProvideElderMode
import com.yuanqinglan.app.core.designsystem.QingLanGreenDark
import com.yuanqinglan.app.core.designsystem.YuanQingLanTheme
import com.yuanqinglan.app.data.local.AppContainer

/**
 * App 根组件：
 * 1. 确保 [AppContainer] 已用 Application Context 初始化（幂等）；
 * 2. 订阅 [AppContainer.settings] 的老年模式开关，全局 [ProvideElderMode] + 主题；
 * 3. 启动页（Splash）→ [MainShell]（底部 5 Tab + NavHost）。
 */
@Composable
fun YuanQingLanApp() {
    val appContext = LocalContext.current.applicationContext
    AppContainer.init(appContext)

    val elderMode by AppContainer.settings.elderMode
        .collectAsStateWithLifecycle(initialValue = false)

    ProvideElderMode(enabled = elderMode) {
        YuanQingLanTheme {
            var showSplash by rememberSaveable { mutableStateOf(true) }
            if (showSplash) {
                SplashScreen(onEnter = { showSplash = false })
            } else {
                MainShell()
            }
        }
    }
}

/**
 * 启动页：全屏竖版插画背景 + 品牌名 + 进入按钮与合规句。
 * 文案口径："开始使用"，不含任何演示语义。
 */
@Composable
private fun SplashScreen(onEnter: () -> Unit) {
    BackHandler(enabled = true) {}
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.splash_chongqing_dawn),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x3DFFFFFF)),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 36.dp),
        ) {
            Text(
                text = "渝安青澜",
                color = QingLanGreenDark,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "让告别回归自然，让思念有所安放",
                color = QingLanGreenDark,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(
                onClick = onEnter,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp)
                    .height(AppDimensions.MinimumTouchTarget),
            ) {
                Text("开始使用")
            }
            Text(
                text = "相关信息仅供参考，具体政策、费用与办理结果以主管机构和服务机构最终公布为准。",
                color = QingLanGreenDark,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp),
            )
        }
    }
}
