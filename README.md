# 渝安青澜 Android APP

渝安青澜是一款面向重庆生态安葬与生命纪念场景的 Android APP。应用以本地数据与本地状态完整呈现产品流程，不建设后端 API，不承接真实殡葬预约、支付或政务申报。

## 当前阶段

已完成 45 个路由的全部功能开发（多子代理并行实现，见 [开发交接](docs/agent-handoff.md) 与 [公共契约](docs/contracts/feature-development.md)），包括：

- 5 Tab 外壳与 45 路由全部可达：首页/政策（9）、安葬双轨（10）、追忆（15）、树洞（3）、我的（7）+ 启动页。
- 老年模式（全局字号/热区/对比度增强，DataStore 持久化）；本地数据层（DataStore 设置、JSON 本地内容、私有目录文件）。
- 人类与宠物服务/纪念空间/树洞内容池强类型隔离。
- 质量门禁：`testDebugUnitTest` 28 套件 / 175 用例全绿；`assembleDebug` 通过；`connectedDebugAndroidTest` 12 个 Compose UI 测试全绿；Pixel 7 API 36（emulator-5560）冷启动与关键链路巡检通过。

里程碑与变更见 [变更记录](CHANGELOG.md)，最新开发分支为 `codex/issue-2-12-app-implementation`（Wave 1 + Wave 2 已合并，PR 待审）。

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
