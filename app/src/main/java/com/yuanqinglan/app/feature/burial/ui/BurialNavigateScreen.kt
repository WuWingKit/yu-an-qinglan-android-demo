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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoorFront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
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

/**
 * 出入口专用高辨识色（深橙红）：与人类安葬区绿、宠物区棕黄、
 * 服务区蓝灰在普通与色弱视觉下均明显区分（色距验证见单元测试）。
 */
internal val ParkEntryColor = Color(0xFFD84315)

/**
 * 点位类型（图例/颜色/标识）。
 * 除颜色外，出入口 [ENTRY] 额外携带门形图标标识（[iconKey] 非空），
 * 地图、图例与说明语义一致，保证不单靠颜色区分。
 */
enum class ParkPointType(val label: String, val color: Color, val iconKey: String?) {
    ENTRY("出入口", ParkEntryColor, "door"),
    SERVICE("接待服务", Color(0xFF6E93A8), null),
    HUMAN_ZONE("人类安葬区", QingLanGreen, null),
    PET_ZONE("宠物独立园区", Color(0xFFB08A58), null),
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

/**
 * 点位语义描述（无障碍/测试断言）：不单靠颜色——
 * 出入口标注"图标标识"，其余点位标注编号，TalkBack 与测试均可据此区分。
 */
internal fun parkMarkerSemanticsLabel(point: ParkPoint, index: Int): String =
    if (point.type.iconKey != null) {
        "${point.name}（${point.type.label}，图标标识，第 $index 号点位）"
    } else {
        "${point.name}（${point.type.label}，第 $index 号点位）"
    }

/** 地图底图尺寸（素材 1537x1025，约 3:2）；点位相对坐标与此匹配。 */
private val MAP_ASPECT_RATIO: Float = 1537f / 1025f

internal val PARK_POINTS: List<ParkPoint> = listOf(
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

internal val PARK_LEGEND: List<ParkPointType> = ParkPointType.entries

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
                            ParkTypeSwatch(type = selected.type, size = 14.dp)
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
            val isEntry = point.type.iconKey != null
            // 出入口用圆角方形 + 门形图标，其余用圆形 + 编号：形状与图标均为非颜色标识。
            val markerShape: Shape = if (isEntry) RoundedCornerShape(8.dp) else CircleShape
            val outerSize = if (isSelected) 48.dp else 44.dp
            val innerSize = if (isSelected) 40.dp else 36.dp
            Box(
                modifier = Modifier
                    .offset(x = dx - outerSize / 2f, y = dy - outerSize / 2f)
                    .size(outerSize)
                    .clip(markerShape)
                    .background(if (isSelected) Color.White else point.type.color.copy(alpha = 0.92f))
                    .clickable { onSelect(point.id) }
                    .semantics { contentDescription = parkMarkerSemanticsLabel(point, index) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(innerSize)
                        .clip(markerShape)
                        .background(point.type.color),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isEntry) {
                        Icon(
                            imageVector = Icons.Outlined.DoorFront,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(if (isSelected) 24.dp else 22.dp),
                        )
                    } else {
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParkLegend() {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PARK_LEGEND.forEach { type ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                ParkTypeSwatch(type = type)
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

/**
 * 类型标识色块：出入口显示门形图标并采用圆角方形，
 * 其余类型为纯色圆点；图例、点位说明与点位列表共用，保证语义一致。
 */
@Composable
private fun ParkTypeSwatch(type: ParkPointType, size: Dp = 10.dp) {
    val icon = typeIcon(type)
    val shape: Shape = if (icon != null) RoundedCornerShape(size / 3f) else CircleShape
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(type.color),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.72f),
            )
        }
    }
}

/** 类型标识图标：仅出入口提供门形图标，其余类型为 null（以编号/颜色区分）。 */
private fun typeIcon(type: ParkPointType): ImageVector? = when (type.iconKey) {
    "door" -> Icons.Outlined.DoorFront
    else -> null
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
                ParkTypeSwatch(type = point.type)
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
