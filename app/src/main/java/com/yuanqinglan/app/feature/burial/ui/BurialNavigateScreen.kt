/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.yuanqinglan.app.core.designsystem.AppDimensions
import com.yuanqinglan.app.core.designsystem.QingLanGreen
import com.yuanqinglan.app.core.designsystem.QingLanGreenDark
import com.yuanqinglan.app.core.designsystem.SurfaceCard
import com.yuanqinglan.app.core.designsystem.TextPrimary
import com.yuanqinglan.app.core.designsystem.TextSecondary
import com.yuanqinglan.app.core.ui.AppScaffold
import com.yuanqinglan.app.core.ui.ReferenceNote

/** 点位类型（图例/颜色）。 */
enum class ParkPointType(val label: String, val color: Color) {
    ENTRY("出入口", QingLanGreenDark),
    SERVICE("接待服务", Color(0xFF6E93A8)),
    HUMAN_ZONE("人类安葬区", QingLanGreen),
    PET_ZONE("宠物独立园区", Color(0xFFB08A58)),
}

/** 园区点位：坐标为底图内相对位置（0..1），便于素材落位后统一校核。 */
data class ParkPoint(
    val id: String,
    val name: String,
    val type: ParkPointType,
    val x: Float,
    val y: Float,
    val description: String,
    val routeText: String,
)

/** 地图底图尺寸（素材 1537x1025，约 3:2）；点位相对坐标与此匹配。 */
private val MAP_ASPECT_RATIO: Float = 1537f / 1025f

private val PARK_POINTS: List<ParkPoint> = listOf(
    ParkPoint(
        id = "gate",
        name = "门岗",
        type = ParkPointType.ENTRY,
        x = 0.14f,
        y = 0.87f,
        description = "园区主入口，来访登记处设于此。",
        routeText = "入园后沿主路前行约 3 分钟可到达接待中心。",
    ),
    ParkPoint(
        id = "reception",
        name = "接待中心",
        type = ParkPointType.SERVICE,
        x = 0.32f,
        y = 0.74f,
        description = "提供安葬咨询、手续办理、档案登记与祭扫指引服务。",
        routeText = "由门岗沿主路直行，见服务指示牌后右转即达。",
    ),
    ParkPoint(
        id = "tree-zone",
        name = "生态林区（树葬区）",
        type = ParkPointType.HUMAN_ZONE,
        x = 0.27f,
        y = 0.30f,
        description = "人类树葬纪念林，林地统一养护，不设硬化墓体。",
        routeText = "由接待中心沿北侧林间步道步行约 6 分钟到达。",
    ),
    ParkPoint(
        id = "flower-zone",
        name = "花田区（花葬区）",
        type = ParkPointType.HUMAN_ZONE,
        x = 0.50f,
        y = 0.24f,
        description = "人类花葬花田，按节令轮作养护。",
        routeText = "由树葬区向东沿步道直行约 4 分钟到达。",
    ),
    ParkPoint(
        id = "lawn-zone",
        name = "草坪区（草坪葬区）",
        type = ParkPointType.HUMAN_ZONE,
        x = 0.74f,
        y = 0.30f,
        description = "人类草坪葬开阔绿地，统一草坪养护。",
        routeText = "由花田区沿东侧步道步行约 4 分钟到达。",
    ),
    ParkPoint(
        id = "pet-zone",
        name = "宠物独立园区",
        type = ParkPointType.PET_ZONE,
        x = 0.78f,
        y = 0.76f,
        description = "宠物安葬独立园区，与人类安葬区以绿植隔离带物理分隔。",
        routeText = "由接待中心东侧岔路绕行约 8 分钟到达独立园区入口。",
    ),
)

private val PARK_LEGEND: List<ParkPointType> = ParkPointType.entries

private val OPEN_TIME_TEXT = "园区开放时间：每日 08:30 - 17:00（节假日以园区公告为准）"

private val VISIT_NOTICES = listOf(
    "倡导鲜花祭扫，园区内禁止焚烧祭品与燃放爆竹。",
    "宠物安葬区仅限宠物服务家属按指引到访，人宠分区通行。",
    "请爱护林木与花田，不采摘、不刻画，按指定步道通行。",
    "访客请在开放时间内入园，如需帮助请联系接待中心。",
)

/** 园区导览（navigate）：静态底图 + Compose 点位、图例、点选说明与路线文本。 */
@Composable
fun BurialNavigateScreen(navController: NavHostController) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = PARK_POINTS.firstOrNull { it.id == selectedId }

    AppScaffold(
        title = "园区导览",
        onBack = { navController.popBackStack() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(12.dp))

            ParkMapBlock(
                points = PARK_POINTS,
                selectedId = selectedId,
                onSelect = { selectedId = it },
            )

            BurialSectionTitle("图例")
            ParkLegend()

            BurialSectionTitle("点位说明")
            BurialCard {
                if (selected == null) {
                    Text(
                        text = "轻点上方地图中的点位，或从下方列表选择，可查看该区域说明与路线。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                } else {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(selected.type.color),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = selected.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = selected.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "路线：${selected.routeText}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                        )
                    }
                }
            }

            BurialSectionTitle("点位列表")
            ParkPointList(
                points = PARK_POINTS,
                selectedId = selectedId,
                onSelect = { selectedId = it },
            )

            BurialSectionTitle("开放时间")
            BurialCard {
                Text(
                    text = OPEN_TIME_TEXT,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                )
            }

            BurialSectionTitle("祭扫须知")
            BurialCheckList(items = VISIT_NOTICES, accent = QingLanGreenDark)
            Spacer(Modifier.height(6.dp))
            ReferenceNote(text = "园区信息仅供参考，具体开放安排以园区当日公告为准。")
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ParkMapBlock(
    points: List<ParkPoint>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(MAP_ASPECT_RATIO)
            .clip(RoundedCornerShape(AppDimensions.CardRadius)),
    ) {
        Image(
            painter = painterResource(BurialArtwork.parkMapRes()),
            contentDescription = "园区导览示意图",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )
        points.forEach { point ->
            val dx = maxWidth * point.x
            val dy = maxHeight * point.y
            val index = points.indexOf(point) + 1
            val isSelected = point.id == selectedId
            Box(
                modifier = Modifier
                    .offset(x = dx - 22.dp, y = dy - 22.dp)
                    .size(if (isSelected) 48.dp else 44.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White else point.type.color.copy(alpha = 0.92f))
                    .clickable { onSelect(point.id) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 40.dp else 36.dp)
                        .clip(CircleShape)
                        .background(point.type.color),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$index",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun ParkLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PARK_LEGEND.forEach { type ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(type.color),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = type.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun ParkPointList(
    points: List<ParkPoint>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        points.forEach { point ->
            val isSelected = point.id == selectedId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppDimensions.CompactRadius))
                    .background(if (isSelected) point.type.color.copy(alpha = 0.14f) else SurfaceCard)
                    .clickable { onSelect(point.id) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(point.type.color),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = point.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = point.type.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
        }
    }
}
