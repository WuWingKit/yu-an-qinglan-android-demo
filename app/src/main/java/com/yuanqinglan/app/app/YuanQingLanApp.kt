/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yuanqinglan.app.R
import com.yuanqinglan.app.core.designsystem.AppBackground
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.QingLanGreenDark
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.navigation.AppRoute
import com.yuanqinglan.app.navigation.TopLevelDestination

@Composable
fun YuanQingLanApp() {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    if (showSplash) {
        SplashScreen(onEnter = { showSplash = false })
    } else {
        MainShell()
    }
}

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
                Text("进入演示")
            }
            Text(
                text = "本应用内容均为虚构演示数据",
                color = QingLanGreenDark,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun MainShell() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceCard,
                modifier = Modifier.navigationBarsPadding(),
            ) {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route.route,
                        onClick = {
                            navController.navigate(destination.route.route) {
                                popUpTo(AppRoute.HOME.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                            )
                        },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.HOME.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppRoute.HOME.route) { HomeScreen() }
            composable(AppRoute.BURIAL.route) {
                ModulePlaceholder("生态安葬", "在人类与宠物服务之间清晰切换")
            }
            composable(AppRoute.MEMORIAL_HOME.route) {
                ModulePlaceholder("云端追忆", "保存生命故事与私人纪念空间")
            }
            composable(AppRoute.TREEHOLE_SELECT.route) {
                ModulePlaceholder("心灵树洞", "人间与生灵内容池相互隔离")
            }
            composable(AppRoute.PROFILE.route) {
                ModulePlaceholder("我的", "管理演示订单、隐私与适老设置")
            }
        }
    }
}

@Composable
private fun HomeScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = AppDimensions.PageHorizontal,
            top = 18.dp,
            end = AppDimensions.PageHorizontal,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SectionSpacing),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("渝安青澜", style = MaterialTheme.typography.headlineMedium)
                    Text("重庆生态安葬与生命纪念", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(AppDimensions.CompactRadius),
                ) {
                    Text(
                        text = "演示数据",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
            }
        }
        item { HomeBanner() }
        item { Text("常用服务", style = MaterialTheme.typography.titleLarge) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ServiceEntry("生态葬式", "了解树葬、花葬与草坪葬", Icons.Outlined.AccountTree, Modifier.weight(1f))
                    ServiceEntry("政策预审", "本地规则模拟测算", Icons.Outlined.Policy, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ServiceEntry("云端追忆", "建立私人纪念空间", Icons.Outlined.FavoriteBorder, Modifier.weight(1f))
                    ServiceEntry("公益活动", "查看集体纪念安排", Icons.Outlined.Celebration, Modifier.weight(1f))
                }
            }
        }
        item {
            Text(
                text = "真实预约、支付、定位与政务办理暂不接入。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun HomeBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(AppDimensions.SceneRadius)),
    ) {
        Image(
            painter = painterResource(R.drawable.home_carousel_chongqing),
            contentDescription = "晨雾中的重庆山城与江岸",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x33000000)),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp),
        ) {
            Text("山水有归处", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text("在熟悉的山城里，选择温和的告别", color = Color.White, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ServiceEntry(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.height(116.dp),
        shape = RoundedCornerShape(AppDimensions.CardRadius),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppDimensions.CardPadding),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ModulePlaceholder(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(AppDimensions.PageHorizontal),
    ) {
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 6.dp),
        )
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(AppDimensions.CardRadius),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
        ) {
            Text(
                text = "工程骨架已就绪，此模块将在对应功能 Issue 中接入本地演示数据。",
                modifier = Modifier.padding(AppDimensions.CardPadding),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
