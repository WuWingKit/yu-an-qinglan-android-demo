# 任务书：foundation_navigation 子代理

你是「渝安青澜」Android APP 多子代理开发中的 `foundation_navigation` 子代理。你负责工程基础、设计系统、公共组件、导航契约、本地数据接口与测试基础。你的工作被 home_policy、burial_services 两个并行子代理依赖，因此你实现的所有公共契约必须与文档签名完全一致。

请完整阅读本文件并**从第一项开始顺序执行**，不要跳过。全部完成后按文末「交付报告」格式汇报。

## 工程位置与环境

- 工程根目录：`E:\WorkSpace\生态殡葬`（Windows，路径含中文）。
- Git 分支：当前工作分支为 `codex/issue-2-12-app-implementation`。**不要执行任何 git 命令**（不 commit/push/checkout/stash），只编辑文件，git 由主 Agent 统一处理。
- 包命名空间：`com.yuanqinglan.app`。单 app 模块，Kotlin + Jetpack Compose + Material 3，Compile/Target SDK 36，Min SDK 26，Gradle 9.1.0 / AGP 9.0.1 / Kotlin 2.2.10。

构建/测试命令（PowerShell，每次运行前设置环境变量）：
```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; $env:ANDROID_HOME="E:\Android\Sdk"; $env:ANDROID_SDK_ROOT="E:\Android\Sdk"; $env:GRADLE_USER_HOME="E:\Gradle"
.\gradlew.bat testDebugUnitTest assembleDebug --console=plain
```
若内存不足（hs_err / daemon disappeared）：先 `Get-Process java | Stop-Process -Force`，再改用
`.\gradlew.bat testDebugUnitTest assembleDebug --no-daemon "-Dorg.gradle.jvmargs=-Xmx1536m -XX:MaxMetaspaceSize=512m" "-Dkotlin.compiler.execution.strategy=in-process"`
注意：中文路径导致构建输出位于 `E:\Gradle\project-builds\yuanqinglan\app`（非标准 build 目录），这是预期行为，不要改动 `app/build.gradle.kts` 里这段兼容逻辑。

## 必须先阅读

- `docs/contracts/feature-development.md`（公共契约，最重要，必须严格遵循）
- `docs/demo-v11-alignment.md`（视觉令牌与组件映射）
- `docs/agent-handoff.md`（素材落位与分工）
- `assets/README.md`（素材清单与约束）
- `docs/iteration-2026-09-05-alignment.md`（功能增量与权威裁决顺序）
- 现有代码：`app/src/main/java/com/yuanqinglan/app/` 下 `app/YuanQingLanApp.kt`、`navigation/AppRoute.kt`、`navigation/TopLevelDestination.kt`、`core/designsystem/*.kt`、`core/model/*.kt`、`MainActivity.kt`、`app/build.gradle.kts`、`gradle/libs.versions.toml`

## 你的独占目录（只许改这些）

- `app/src/main/java/com/yuanqinglan/app/core/**`（designsystem、ui、model）
- `app/src/main/java/com/yuanqinglan/app/data/local/**`
- `app/src/main/java/com/yuanqinglan/app/app/**`（AppState、顶层 Scaffold、NavHost 集成）
- `app/src/main/res/drawable-nodpi/**`（素材落位）
- `app/build.gradle.kts`、`gradle/libs.versions.toml`（新增依赖）
- `app/src/test/java/.../data/` 与 `app/src/test/java/.../core/` 下的测试

禁止改动：`navigation/AppRoute.kt`、`navigation/TopLevelDestination.kt`、`feature/**`（那是其他子代理目录）、`app/src/main/assets/demo/**` 业务 JSON。

## 工作内容（按顺序完成）

### 1. 素材落位（最先做，解除其他子代理依赖）
把以下仓库素材复制到 `app/src/main/res/drawable-nodpi/`，资源文件名保持不变（已是合法小写下划线名）。来源在 `assets/generated/`：
- 根目录：`burial_tree_grove.webp`、`burial_flower_garden.webp`、`burial_lawn.webp`、`burial_pet_tree.webp`、`memorial_human_portrait.webp`、`memorial_pet_portrait.webp`
- `v11/burial/`：`burial_pet_flower.webp`、`burial_pet_lawn.webp`
- `v11/activities/`：`activity_collective_memorial.webp`、`activity_life_education.webp`
- `v11/news/`：`news_ecoburial_cycle.webp`、`news_bayu_customs.webp`
- `v11/memorial/`：`memorial_gallery_family_tea.webp`、`memorial_gallery_pet_park.webp`
- `v11/utility/`：`ai_restore_sample_faded.webp`、`park_overview_map.webp`

复制后用 `Get-ChildItem app\src\main\res\drawable-nodpi | Measure-Object` 确认共 22 个文件（含已有的 6 张）。

### 2. 新增依赖
在 `gradle/libs.versions.toml` 与 `app/build.gradle.kts` 中加入：
- `org.jetbrains.kotlin.plugin.serialization`（版本同 kotlin 2.2.10）+ `kotlinx-serialization-json`（与 Kotlin 2.2 兼容的稳定版）
- `androidx.datastore:datastore-preferences`（稳定版，如 1.1.x）
- `androidx.lifecycle:lifecycle-viewmodel-compose` 与 `androidx.lifecycle:lifecycle-viewmodel-ktx`（与现有 lifecycle 2.9.4 一致）
本地 drawable 优先用 `painterResource`；如加载文件 URI 需要图片库再评估，不必引入 Coil。

### 3. 老年模式（core/designsystem）
- 新增 `LocalElderMode: CompositionLocal<Boolean>` 与 `@Composable fun ProvideElderMode(enabled: Boolean, content: @Composable () -> Unit)`（默认 false）。
- 开启时全局字号放大（约 1.25 倍）、点击热区 ≥52dp、简化次要动效、增强对比度。字号放大可在 `YuanQingLanTheme` 内根据 LocalElderMode 调整 typography；组件只读取该 CompositionLocal 调节触达尺寸。保持主题令牌结构稳定。

### 4. core/ui 公共组件（签名必须与契约文档 §3 完全一致）
新建文件（每个带版权头）：`AppScaffold`、`NoticeBanner`（含 `NoticeTone` 枚举 INFO/COMPLIANCE/WARNING）、`AudienceSegment`、`ServiceSceneCard`、`SectionHeader`、`ReferenceNote`（对外显示"信息参考"或合规句，禁止"演示"字样）、`ConfirmDangerDialog`、`EmptyState`、`ErrorRetry`、`LoadingState`、`PrimaryButton`、`SecondaryButton`、`FormTextField`、`InfoRow`。
要求：全部用 Material 3 + Material Icons（可用 `androidx.compose.material:material-icons-extended`），禁止 Emoji/文字符号；点击热区≥48dp；卡片圆角 16dp（`AppDimensions.CardRadius`）、紧凑 10dp、大场景 22dp；主内容水平边距 14dp（`AppDimensions.PageHorizontal`）。视觉遵循 `docs/demo-v11-alignment.md`：背景 #F7F5EF、主色 #4F7A45、深主色 #3A5C32、正文 #2B3330、次文本 #5A6562；页面标题 22sp/SemiBold、区块标题 17sp/SemiBold、正文 13-14sp。图片带 contentDescription（纯氛围背景 null），可点击组件带语义标签。

### 5. 数据层（data/local）
- `SettingsRepository`：接口 + `DataStoreSettingsRepository` 实现（DataStore preferences，单文件 `settings`）：`elderMode: Flow<Boolean>`/`setElderMode`；`treeholeEnabled: Flow<Boolean>`/`setTreeholeEnabled`；`nickname: Flow<String>`/`setNickname`；`avatarUri: Flow<String?>`/`setAvatarUri`；`privacyAccepted: Flow<Boolean>`/`setPrivacyAccepted`；`suspend fun resetAll()`。默认：elderMode=false、treeholeEnabled=true、nickname="渝安青澜用户"、avatarUri=null、privacyAccepted=false。
- `DemoAssetLoader`：从 `app/src/main/assets/demo/**/**.json` 读取并用 kotlinx.serialization 解析为任意 @Serializable 类型；提供 `suspend fun <T> load(path: String, deserializer: KSerializer<T>): T`；用 Android Context 初始化。
- `FileStorage`：应用私有目录（`context.filesDir`）保存/删除图片与音频，返回可复用文件 URI。
- `AppContainer`：简单 ServiceLocator（object 或伴生），懒加载 `SettingsRepository`、`DemoAssetLoader`、`FileStorage`。不做 DI 框架。
- 仓库接口保持异步形态（Flow / suspend），注入固定短延迟（300-600ms）让加载/成功/空/失败/重试状态可复现。

### 6. app/ 顶层重构（app/YuanQingLanApp.kt 拆分）
- 保留 SplashScreen（对外文案已改"开始使用"与合规句，不要改回"进入演示"）。
- 顶部提供 `ProvideElderMode`（读取 `SettingsRepository.elderMode`）。
- `MainShell`：Scaffold + 底部 5 Tab（Home/Burial/Memorial/Treehole/Profile），每 Tab 保留回退栈（现有 `popUpTo(HOME){saveState}` + `launchSingleTop` + `restoreState` 模式保留）。
- NavHost：startDestination = home；在 NavHost 中调用各 feature 的 NavGraph 扩展函数（由主 Agent 冻结，feature 子代理按此实现）：
  - `fun NavGraphBuilder.homeNavGraph(navController: NavHostController)`（feature/home）
  - `fun NavGraphBuilder.policyNavGraph(navController: NavHostController)`（feature/policy）
  - `fun NavGraphBuilder.burialNavGraph(navController: NavHostController)`（feature/burial）
  - `memorialNavGraph/treeholeNavGraph/profileNavGraph`（第二批，此时可能不存在）
  你调用 home/policy/burial 三个扩展；memorial/treehole/profile 三个第二批暂用现有占位路由（保证 5 个 Tab 根可到达），文件内留 TODO 说明第二批接入点。
- `YuanQingLanApp.kt` 中现有 HomeScreen 由 home_policy 重写并迁到 feature/home；外壳内保留临时实现并加 TODO 或直接按 homeNavGraph 覆盖，二选一，保证编译通过。

### 7. 测试基础
- `SettingsRepository` 单元测试（内存/临时实现，至少覆盖 elderMode 与 nickname 读写、resetAll 恢复默认）。
- `AudienceSegment`、`NoticeBanner` 等组件的简单测试（能跑 JVM 放 test，需 Compose UI 放 androidTest）。
- 保留并保证 `AppRouteTest`（45 路由唯一性）继续通过；路由测试不得改动。

## 硬性要求

1. 版权头：每个新建或大幅修改的项目自有 Kotlin/Gradle 源文件顶部加：
```
/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */
```
XML 用等价 `<!-- -->`。不写入 Wrapper/第三方/自动生成文件。
2. 对外文案禁止"演示""纯前端演示""原型""假数据"等字样（契约 §7）。
3. 单向数据流、不可变 UI 状态、ViewModel + Coroutines + StateFlow；不强依赖可空字段跨人宠复用。
4. 不运行任何 git 命令；不启动模拟器（主 Agent 统一管理设备）。
5. 新增依赖用阿里云镜像可解析的稳定版本，不引入不受信任的动态版本。

## 验证门禁（交付前必须完成并记录）

1. 运行 `.\gradlew.bat testDebugUnitTest assembleDebug`（命令见上，注意环境变量与内存降级参数），记录 BUILD SUCCESSFUL/FAILED 与测试通过数。
2. 用 `rg -L "Copyright \(c\) 2026" app/src/main/java app/src/test` 列出缺失版权头的自有源码文件并补齐。
3. 确认 drawable-nodpi 共 22 个文件。

## 交付报告（最终消息必须包含）

1. 修改/新增文件清单（分 core、data/local、app、res、gradle 组）。
2. 完成组件与数据层接口清单（对照契约 §3 §4 §5 逐项）。
3. 素材落位确认（drawable-nodpi 文件数）。
4. 实际运行构建/测试命令与结果（BUILD SUCCESSFUL？测试通过数？）。
5. 版权头覆盖自查结果。
6. 与契约偏离（如有）与剩余风险（第二批 NavGraph 占位、依赖版本待主 Agent 复验）。
