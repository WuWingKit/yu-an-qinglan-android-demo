# 任务书：memorial_treehole 子代理

你是「渝安青澜」Android APP 多子代理开发中的 `memorial_treehole` 子代理。你负责追忆模块（纪念空间、生命故事、祭扫时光、信件、AI 伦理与素材、代祭）与树洞模块（双内容池）。wave-1 的 foundation/home/burial 已完成并合并，公共组件与数据层已就绪。

请完整阅读本文件并**从第一项开始顺序执行**。全部完成后按文末「交付报告」格式汇报。

## 工程位置与环境

- 工程根目录：`E:\WorkSpace\生态殡葬`（Windows，路径含中文）。
- 当前工作分支 `codex/issue-2-12-app-implementation`。**不要执行任何 git 命令**，只编辑文件，git 由主 Agent 统一处理。
- 包命名空间：`com.yuanqinglan.app`。Kotlin + Jetpack Compose + Material 3。

构建/测试命令（PowerShell，每次运行前设置环境变量）：
```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; $env:ANDROID_HOME="E:\Android\Sdk"; $env:ANDROID_SDK_ROOT="E:\Android\Sdk"; $env:GRADLE_USER_HOME="E:\Gradle"
.\gradlew.bat testDebugUnitTest assembleDebug --console=plain
```
若内存不足（hs_err / daemon disappeared）：`Get-Process java | Stop-Process -Force` 后改用
`.\gradlew.bat testDebugUnitTest assembleDebug --no-daemon "-Dorg.gradle.jvmargs=-Xmx1536m -XX:MaxMetaspaceSize=512m" "-Dkotlin.compiler.execution.strategy=in-process"`
中文路径导致构建输出在 `E:\Gradle\project-builds\yuanqinglan\app`，是预期行为。

## 必须先阅读

- `docs/contracts/feature-development.md`（公共契约，最重要）
- `docs/iteration-2026-09-05-alignment.md`（追忆/树洞增量要求）
- `docs/demo-v11-alignment.md`（视觉令牌）
- `docs/agent-handoff.md`（素材落位）
- 现有代码：`navigation/AppRoute.kt`、`core/**`、`data/local/**`、`app/**`（wave-1 已合并）、`feature/home/**`、`feature/burial/**`（参考其 NavGraph 扩展函数写法）

## 你的独占目录（只准改这些）

- `app/src/main/java/com/yuanqinglan/app/feature/memorial/**`
- `app/src/main/java/com/yuanqinglan/app/feature/treehole/**`
- `app/src/main/assets/demo/memorial/**` 与 `app/src/main/assets/demo/treehole/**`
- `app/src/test/java/.../feature/memorial/**` 与 `.../feature/treehole/**`

禁止改动：`core/**`、`data/local/**`、`app/**`、`navigation/**`、`feature/home/**`、`feature/policy/**`、`feature/burial/**`、`res/drawable-nodpi/**`。如发现契约缺失，在你的交付报告中向主 Agent 提出变更建议，不要自行改 core/。

## 追忆模块：15 个路由

你负责：`memorial-home`、`memorial-create`、`memorial-detail`、`memorial-main`、`pet-memorial`、`memorial-story`、`story-add`、`jisi-time`、`memorial-diary`、`letter-write`、`letter-view`、`ai-ethics`、`ai-upload`、`daiji`、`jiti-history`。

NavHost 接入：`fun NavGraphBuilder.memorialNavGraph(navController: NavHostController)`（feature/memorial 包内），注册上述 15 个路由。参数遵循契约 §2（`memorial-detail/{memorialId}`、`memorial-main/{memorialId}`、`pet-memorial/{memorialId}`、`memorial-story/{memorialId}`、`story-add/{memorialId}`、`memorial-diary/{memorialId}`、`letter-write/{memorialId}`、`letter-view/{letterId}`、`ai-upload/{memorialId}`、`daiji/{memorialId}`；NavType.StringType）。

### 数据模型（人宠强隔离，硬性）
- `HumanMemorial` 与 `PetMemorial` 独立数据类（id、名称、肖像 drawable、关系/简介、相册列表、寄语、故事节点列表、信件列表、祭扫时间线等），各自 Repository 入口；切换轨道绝不串数据。纪念空间可创建（`memorial-create` 表单：类型=人/宠、名称、关系/简介、肖像选择）、可管理（列表、编辑、删除带二次确认）。
- 数据来自 `assets/demo/memorial/memorials.json` 等本地 JSON（`DemoAssetLoader` 解析，kotlinx.serialization）；运行时新建/修改的纪念空间保存在内存 + 序列化文件（`FileStorage` 私有目录）中，重启后可选保留。
- 肖像素材：`R.drawable.memorial_human_portrait`（人类，需显示"演示人物"语义）、`R.drawable.memorial_pet_portrait`（宠物，"演示宠物"语义）；相册格 `R.drawable.memorial_gallery_family_tea`（人类）、`memorial_gallery_pet_park`（宠物）。

### 页面要求
1. **memorial-home**：追忆 Tab 根。顶部 `AudienceSegment` 人/宠切换；人类轨显示人类纪念空间列表 + "新建纪念空间"；宠物轨显示宠物纪念空间 + 新建。空状态用 `EmptyState`（有动作）。点击进入 `memorial-detail/{id}`（人类）或 `pet-memorial/{id}`（宠物）。入口含：生命故事、祭扫时光、信件、AI 追忆、异地代祭、共祭历史。
2. **memorial-create**：创建表单（人/宠类型、名称、关系/简介、肖像）；本地校验（必填、长度）；成功后进入新空间详情。可"重新填写"。
3. **memorial-detail/{memorialId}**（人类）与 **pet-memorial/{memorialId}**（宠物）：纪念空间详情，含 **5 个页内 Tab**：主页（肖像、寄语、基本信息）、相册（多选、三列网格、放大查看、删除+二次确认）、寄语（留言列表，本地新增）、AI 追忆（入口 → ai-ethics）、祭扫延伸（共祭/代祭/祭扫时光入口）。标题无脏字符。
4. **memorial-main/{memorialId}**：纪念空间"主页"独立展示页（可作相册/寄语集中管理，三列网格、放大、删除二次确认）。
5. **memorial-story/{memorialId}**：生命故事时间线；节点按时间排序展示（新增节点立即插入正确时间位置）。可导出纪念册（导出内容必须包含新增节点，导出为本地文件/文本）。
6. **story-add/{memorialId}**：新增故事节点（标题、时间、正文、可选图片附件）；保存后按时间排序。
7. **jisi-time**：祭扫时光——祭扫记录时间轴（日期、地点/方式、寄语），可新增本地记录。
8. **memorial-diary/{memorialId}**：思念日记——列表+详情；日记支持图片与音频附件（新增、编辑回填、查看、播放、删除）；附件保存到私有目录（`FileStorage`）。录音用系统能力，权限拒绝时显示明确反馈。
9. **letter-write/{memorialId}** 与 **letter-view/{letterId}**：写信与未寄/已存信件查看。信纸样式可选；本地保存。
10. **ai-ethics**：AI 追忆伦理前置页——强制先阅读并确认：授权范围、私人访问、用途透明、永久销毁；"我已知晓并同意"按钮不可跳过；不同意则返回。不制作逝者实时对话。
11. **ai-upload/{memorialId}**：AI 素材选择工作台——相册/文件/录音/视频素材选择（系统选择器 + 录音权限）、修复前预览（用 `R.drawable.ai_restore_sample_faded`）、生成进度动画（固定预置结果）、生成结果可预览与"永久销毁"。所有流程本地状态可重复操作；销毁高优先级二次确认。
12. **daiji/{memorialId}**：异地代祭——套餐选择（付费套餐，本地状态）、预约信息、订单/履约状态、影像归档到该纪念空间。**与线上集体共祭（jiti-history）彻底分开，不混成同一付费流程**。
13. **jiti-history**：线上集体共祭——公益活动列表与参与历史（免费），与代祭分开展示；报名本地状态。

## 树洞模块：3 个路由

你负责：`shudong-select`、`shudong-ren`、`shudong-sheng`。

NavHost 接入：`fun NavGraphBuilder.treeholeNavGraph(navController: NavHostController)`（feature/treehole 包内）。

1. **shudong-select**：树洞 Tab 根。双内容池入口（人间/生灵），`AudienceSegment` 或两张入口卡。进入任一池前显示"游客不可使用"提示（本地身份确认按钮"我已了解，继续"，避免建账号系统；确认后本地状态允许进入发布）。
2. **shudong-ren** 与 **shudong-sheng**：两套**独立内容池与本地存储**（`assets/demo/treehole/human-letters.json` 与 `pet-letters.json`；信纸样式、分类、附件不可跨池）。功能：
   - 寄信（信纸选择、分类、正文、可选图片/音频附件；图片≤10MB、音频≤5MB，超限明确提示）；发布后进入"待审核"状态并自动切到拾信模式。
   - 拾信（随机拾取一封、换一封、轻回应——仅"点灯/叶片/花朵"表达关怀，**不显示计数**）、举报（本地状态反馈）。
   - 本人内容列表可查看与删除（二次确认）。
   - **不显示**点赞计数、地域、热度、粉丝、私信、楼中楼评论。
   - 卡片不展示地域，不建立社交关系。

## 硬性要求

1. 版权头：每个新建项目自有 Kotlin/XML 源文件顶部加：
```
/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */
```
2. 对外文案禁止"演示""假数据""原型"等字样；AI 追忆强调授权/私人访问/用途透明/永久销毁，不模拟实时对话；代祭与共祭分开；人宠数据强隔离；树洞无社交计数。
3. 单向数据流、不可变 UI 状态、ViewModel + Coroutines + StateFlow；导航只传稳定 ID/枚举。
4. 图标统一 Material Icons，禁止 Emoji/文字符号；附件图片/音频存私有目录（FileStorage），不上传任何外部。
5. 所有主要控件真实响应且可重复操作；加载/成功/空/失败/重试状态可复现。
6. 不运行任何 git 命令；不启动模拟器（主 Agent 统一管理设备）。

## 验证门禁（交付前必须完成并记录）

1. 运行 `.\gradlew.bat testDebugUnitTest assembleDebug`，记录结果（若因其他 wave 未合并而失败，如实说明，不伪造通过）。
2. 单元测试至少覆盖：人宠纪念空间数据隔离；故事节点按时间排序与导出包含新增节点；相册多选/删除状态；日记附件增删改；树洞双池内容互不串池；附件大小上限校验（10MB 图片 / 5MB 音频）；AI 伦理页不可跳过；代祭与共祭状态独立。
3. `rg -L "Copyright \(c\) 2026" app/src/main/java/com/yuanqinglan/app/feature/memorial app/src/main/java/com/yuanqinglan/app/feature/treehole` 自查版权头并补齐。

## 交付报告（最终消息必须包含）

1. 修改/新增文件清单。
2. 18 个路由完成情况逐项说明。
3. 人宠隔离、双池隔离方案与测试结果。
4. 实际构建/测试命令与结果。
5. 与契约偏离、待主 Agent 处理的变更建议、剩余风险。
