# 渝安青澜 Android APP

渝安青澜是一款面向重庆生态安葬与生命纪念场景的 Android APP。应用以本地数据与本地状态完整呈现产品流程，不建设后端 API，不承接真实殡葬预约、支付或政务申报。

## 当前阶段

当前版本为 **2.1.0**，在 46 个路由全部可达的基础上完成树洞内容、宠物影像修复、首页轮播素材、轻回应动画及版权授权详情优化（见 [2.1.0 发布说明](docs/release-notes-v2.1.0.md)、[开发交接](docs/agent-handoff.md) 与 [公共契约](docs/contracts/feature-development.md)），包括：

- 5 Tab 外壳与 46 路由（45 既有 + 新增版权详情路由）：首页/政策（9）、安葬双轨（10）、追忆（15）、树洞（3）、我的（8）+ 启动页。
- 树洞"拾一封信"双池扩充至各 10 条原创温和留言，阅读卡展示虚构昵称/非实名 ID/头像/时间；点灯、叶片、花朵三种克制轻回应动画（低动态偏好下降级，不记录计数）。
- 人类/宠物纪念档案支持出生与离世日期（仅年份/年月/完整三档精度，旧 JSON 与旧快照兼容）。
- 首页轮播每页可配遮罩（按实测亮度推导）、园区导览出入口高辨识颜色+图标+形状（不只靠颜色）。
- 个人中心版权及授权收纳为单行入口，详情页与中英文授权书查看器完整可访问。
- 老年模式（全局字号/热区/对比度增强，DataStore 持久化）；本地数据层（DataStore 设置、JSON 本地内容、私有目录文件）；人宠双轨、树洞双池强类型隔离。
- 质量门禁（截至 2.0 发布）：`testDebugUnitTest` 38 套件 / 246 用例全绿；`lintDebug`、`assembleRelease` 通过；Pixel 7 API 36 上 15 个 Compose UI 测试全绿。

里程碑与变更见 [变更记录](CHANGELOG.md)。

## 产品基线

底部导航为：首页、安葬、追忆、树洞、我的。安葬、追忆和树洞均支持人类与宠物场景，但必须维持场地、数据和树洞内容池隔离。

应用明确遵循以下边界：

- 不接入真实后端、支付、地图、直播、AI 生成或政务系统。
- 所有业务数据均为本地虚构数据。
- 不提供公开社交关系、点赞排名、私信或楼中楼评论。
- AI 追忆仅展示私人、授权、可销毁的流程，不模拟逝者实时对话。
- 不展示重庆内河江葬服务，不进行忌日强提醒或思念行为量化。

详细范围见 [产品基线](docs/product-baseline.md)，最新 Word 与在线 Demo 的权威顺序和冲突处理见 [2026.09.05 迭代对齐](docs/iteration-2026-09-05-alignment.md)，同款视觉见 [Demo v11 对齐规范](docs/demo-v11-alignment.md)，技术决策见 [技术方案](docs/technical-plan.md)，实施拆分见 [路线图](docs/roadmap.md)。供其他开发 Agent 直接接手的目录边界、合并顺序、素材落位和验收清单见 [开发交接](docs/agent-handoff.md)。

设计交接文件：<https://www.figma.com/design/kadmc6qv1AbxxzUMoTh15m>。45 个路由与桌面总览的全量截图保存在 [`docs/reference/demo-v11-full/`](docs/reference/demo-v11-full/)，快速回归子集保存在 [`docs/reference/demo-v11/`](docs/reference/demo-v11/)，新增素材目录和生成提示词见 [`assets/README.md`](assets/README.md)。

## 开发环境

- Windows 11
- Android Studio 2026.1
- Kotlin + Jetpack Compose + Material 3
- Compile / Target SDK 36
- Android 16 模拟器：`Pixel_7_API_36`，建议固定使用端口 `5560`
- 项目构建使用 Gradle Wrapper，SDK 路径由未提交的 `local.properties` 提供

## 资料来源

- 本地 `渝安青澜.docx`：详细功能与 UI 方案。
- 本地 `超干货总结.docx`：早期产品想法与视觉偏好。
- 本地 `渝安青澜——重庆花、林、江生态殡葬创新.pdf`：项目背景、调研和商业依据。
- [参考 Demo v11](https://1926e0d2e0b94540aaebdfef3ce96faa.app.workbuddy.link/)：5-Tab、人宠双轨与完整演示流程，作为最新交互和视觉基线。

三份原始文档包含个人联系信息，仅作为本地参考资料使用，已加入 `.gitignore`，不会上传到公开仓库。仓库中的 `docs/` 是去除个人信息后的工程基线。

## 许可与版权

Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）。保留所有权利。

本项目不是开源软件。未经权利人事先明确的书面授权，禁止以任何目的利用本项目的全部或任何部分，包括商用、非商用、个人、教育、研究、竞赛、公益或其他用途。公开仓库仅保留 GitHub 服务条款要求的平台内浏览和 Fork 权限；这不构成使用、修改、部署、传播或商业化授权。完整条款见 [专有软件许可协议](LICENSE)。

当前有效的特定书面授权、适用对象、比赛用途及签名凭证见 [软件授权记录](docs/AUTHORIZATIONS.md)。公开授权记录不构成对其他个人或组织的许可。
