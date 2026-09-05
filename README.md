# 渝安青澜 Android 演示 APP

渝安青澜是一款面向重庆生态安葬与生命纪念场景的 Android 演示应用。项目以纯前端原型为目标，通过本地模拟数据呈现完整、可信的产品流程，不建设后端 API，也不承接真实殡葬预约、支付或政务申报。

## 当前阶段

当前仓库处于需求与素材准备阶段，尚未开始 APP 功能开发。已完成的准备内容包括：

- 汇总本地 Word、PDF 和参考 Demo v11，确定最新产品基线与 45 个演示路由。
- 确定 Kotlin、Jetpack Compose、Material 3 技术方向。
- 建立人类与宠物双轨、5 个底部一级导航的页面范围。
- 准备视觉素材清单与工程实施路线图。

## 产品基线

底部导航为：首页、安葬、追忆、树洞、我的。安葬、追忆和树洞均支持人类与宠物场景，但必须维持场地、数据和树洞内容池隔离。

演示应用明确遵循以下边界：

- 不接入真实后端、支付、地图、直播、AI 生成或政务系统。
- 所有业务数据均为本地虚构演示数据。
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
