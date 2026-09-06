# 渝安青澜 多子代理开发契约（冻结版 v1）

本文档由主协调 Agent 冻结，作为所有子代理并行开发的公共契约。任何子代理不得单方面修改本契约；需要变更时必须向主 Agent 提交变更建议，经裁决后更新本文档再实施。

## 1. 目录所有权（禁止越界）

| 目录 | 所有者 | 内容 |
| --- | --- | --- |
| `app/src/main/java/.../navigation/` | 主 Agent（冻结） | `AppRoute.kt`、`TopLevelDestination.kt`，45 路由键不可增删改 |
| `app/src/main/java/.../app/` | foundation_navigation | AppState、顶层 Scaffold、AppContainer、启动流程、NavHost 集成（feature 以扩展函数接入） |
| `app/src/main/java/.../core/` | foundation_navigation | designsystem、ui、model 全部公共组件与稳定模型 |
| `app/src/main/java/.../data/local/` | foundation_navigation | 设置 DataStore、本地 JSON 加载、私有文件存储 |
| `app/src/main/res/drawable-nodpi/` | foundation_navigation | 全部素材复制落位（所有权，feature 只读引用 `R.drawable.*`） |
| `app/src/main/java/.../feature/home/` + `feature/policy/` | home_policy | 首页 9 路由 + 政策链路 + 所属本地 JSON |
| `app/src/main/java/.../feature/burial/` | burial_services | 安葬双轨 10 路由 + 所属本地 JSON |
| `app/src/main/java/.../feature/memorial/` + `feature/treehole/` | memorial_treehole（第二批） | 追忆 15 路由 + 树洞 3 路由 |
| `app/src/main/java/.../feature/profile/` | profile_quality（第二批） | 我的 8 路由 + 全量质量 |
| `app/src/main/assets/demo/` | 按子目录隔离 | `demo/home/`、`demo/burial/`、`demo/memorial/`、`demo/treehole/`、`demo/profile/` |
| `docs/`、`README.md`、`build.gradle.kts`、`gradle/libs.versions.toml` | 主 Agent 裁决 | 变更需建议 |

NavHost 集成规则：feature 各自提供 `fun NavGraphBuilder.xxxNavGraph(navController: NavHostController, ...)` 扩展函数（位于自己目录内）；主 Agent 负责在顶层 NavHost 中调用这些扩展。feature 之间禁止互相引用对方文件。

## 2. 冻结导航参数

导航只传稳定 ID 或受控枚举，不传大对象、不传自由文本。`AppRoute` 键保持 45 个不变，路由参数以 `{}` 或 `?arg=` 形式挂在对应键上：

| 路由 | 参数 | 说明 |
| --- | --- | --- |
| `county-detail/{countyId}` | String | 区县详情 |
| `news-detail/{newsId}` | String | 资讯详情 |
| `tree`、`flower`、`grass` | 无 | 人类葬式详情（内容各自独立） |
| `pet-tree` | `?mode=TREE\|FLOWER\|LAWN` | 宠物三葬式共享参数化详情（受控枚举，非字符串过滤） |
| `pet-park` | `?mode=TREE\|FLOWER\|LAWN` | 宠物独立园区 |
| `plan` | 无 | 套餐列表 |
| `plan-form/{planId}` | String | 预约表单 |
| `order/{orderId}` | String | 订单进度 |
| `navigate` | 无 | 园区导航 |
| `memorial-detail/{memorialId}`、`memorial-main/{memorialId}`、`pet-memorial/{memorialId}` | String | 纪念空间详情/主页 |
| `memorial-story/{memorialId}`、`story-add/{memorialId}`、`memorial-diary/{memorialId}` | String | 故事/日记 |
| `letter-write/{memorialId}`、`letter-view/{letterId}` | String | 信件 |
| `ai-upload/{memorialId}`、`daiji/{memorialId}` | String | AI 素材/代祭 |
| `shudong-ren`、`shudong-sheng` | 无 | 树洞双池 |
| 其余 | 无 | 保持无参 |

实现时用 `navArgument { type = NavType.StringType }` + `arguments`，从仓库按 ID 读取对象。

## 3. 公共 UI 组件（core/ui，foundation 实现，feature 只调用）

```
AppScaffold(title, onBack: (() -> Unit)?, actions: RowScope.() -> Unit = {}, content)
NoticeBanner(text, tone: NoticeTone = INFO)        // 合规/提示条，左色条
AudienceSegment(selected: AudienceTrack, onSelect)
ServiceSceneCard(imageRes, title, subtitle?, price?, onClick, aspectRatio = 1.5f)
SectionHeader(title, actionLabel?, onAction?)       // 17sp SemiBold 区块标题 + 可选动作
ReferenceNote(text)                                  // “信息参考”合规说明（禁止“演示”字样）
ConfirmDangerDialog(title, message, confirmLabel, onConfirm, onDismiss)
EmptyState(title, description?, actionLabel?, onAction?)
ErrorRetry(message, onRetry)
LoadingState()
PrimaryButton(text, onClick, enabled = true)
SecondaryButton(text, onClick)
FormTextField(label, value, onValueChange, isError?, supportingText?)
InfoRow(label, value)                                // 键值行
```

- 图标一律 `Icons.Outlined.*` / `Icons.Filled.*`（Material Icons），禁止 Emoji、文字符号、手绘 SVG。
- 点击热区 ≥ 48dp；卡片圆角 16dp；紧凑控件 10dp；大场景图 22dp；主内容水平边距 14dp。

## 4. 数据层契约（data/local，foundation 实现）

```
interface SettingsRepository {
  val elderMode: Flow<Boolean>; suspend fun setElderMode(Boolean)
  val treeholeEnabled: Flow<Boolean>; suspend fun setTreeholeEnabled(Boolean)
  val nickname: Flow<String>; suspend fun setNickname(String)
  val avatarUri: Flow<String?>; suspend fun setAvatarUri(String?)
  val privacyAccepted: Flow<Boolean>; suspend fun setPrivacyAccepted(Boolean)
  suspend fun resetAll()          // 恢复默认（供个人中心重置）
}
DemoAssetLoader   // 读取 assets/demo/**/**.json，kotlinx.serialization 解析
FileStorage       // 应用私有目录保存/读取图片、音频，返回可复用 URI
AppContainer      // 简单 ServiceLocator：懒加载 SettingsRepository / FeatureRepositories
```

- 新增依赖：`kotlinx-serialization-json`、`datastore-preferences`（foundation 负责加进 `libs.versions.toml` + `build.gradle.kts`）。
- 所有仓库接口保持异步（Flow / suspend），注入固定短延迟，使加载/成功/空/失败/重试状态可复现。

## 5. 老年模式契约

- `core/designsystem` 提供 `LocalElderMode: CompositionLocal<Boolean>` 与 `ProvideElderMode(Boolean, content)`，在 App 根部提供。
- 开启后：全局字号放大（约 ×1.25）、点击热区提高、次要动效简化、对比度增强。
- 首页右上角开关 + 个人中心“老年模式”页共用 `SettingsRepository.elderMode`，全局生效并持久化。
- 普通与老年模式都不得出现文字截断、重叠、横向滚动或底栏遮挡。

## 6. 强类型隔离（硬性）

- 人类与宠物服务、纪念空间、树洞内容池：各自独立 sealed 类型/数据类/Repository 入口，禁止用可空字段或字符串过滤共享同一列表。
- 例如 `feature/burial`：`BurialMode(enum TREE/FLOWER/LAWN)`、`HumanBurialService`、`PetBurialService` 分离；`feature/memorial`：`HumanMemorial`/`PetMemorial` 分离；树洞：`HumanLetter`/`PetLetter` 分离。
- 顶部人/宠切换使用 `AudienceSegment`，切换后数据源随之切换，互不串联。

## 7. 对外文案口径（硬性）

- 界面、应用名、关于页、按钮、提示语、交付说明中**不得出现**“演示”“纯前端演示”“原型”“假数据”等字样；不得在普通业务页显示版本号。
- 政策、价格、补贴、预约、机构、区县办理结果统一用克制合规说明，例如：“相关信息仅供参考，具体政策、费用与办理结果以主管机构和服务机构最终公布为准。”
- 不伪造官方背书、不接入真实支付、不自动提交真实个人信息、不暗示已完成政务审批。头像/昵称/图片/录音/纪念内容只存应用私有目录或本地设置。

## 8. 版权声明（硬性）

每个新建或大幅修改的项目自有 Kotlin/Gradle/XML 源文件顶部加：

```kotlin
/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */
```

XML 用等价 `<!-- ... -->`。不写入第三方文件、Wrapper、自动生成文件。提交前用 `rg` 自查新文件覆盖。

## 9. 视觉基线

- 画布 375 x 812dp；背景 `#F7F5EF`；主色 `#4F7A45`；深主色 `#3A5C32`；正文 `#2B3330`。
- 页面标题 22sp/SemiBold、区块标题 17sp/SemiBold、正文 13-14sp。
- 底栏视觉高约 78dp，叠加系统导航安全区；所有图片提供 contentDescription（氛围背景为 null）。
- 大图放 `drawable-nodpi`，轮播图 `aspectRatio(2f)`、活动/相册图 `aspectRatio(4/3f)`。
- 禁止卡片套卡片。

## 10. 质量门槛（每个工作包交付）

1. 所属路由全部可从界面进入；返回键与 Tab 回退栈正确。
2. 加载/成功/空/失败/重试状态可稳定复现。
3. 所有主要控件真实响应（按钮、Tab、表单、开关、筛选、上传选择、确认、删除、重置、结果页可重复操作）。
4. 人/宠数据隔离有单元测试。
5. 新增逻辑有单元测试；关键链路有 Compose UI 测试。
6. 构建命令：`.\gradlew.bat testDebugUnitTest assembleDebug`（含内存压力时用 `--no-daemon -Dorg.gradle.jvmargs=-Xmx1536m`）。
7. 不在提交中出现 `hs_err_*.log`、`replay_*.log`、`local.properties`、构建产物。

## 11. 交付报告模板

每个子代理交付时报告：修改文件清单、完成路由、单元/UI 测试结果（实际命令与输出）、截图状态、与契约的偏离、剩余风险。主 Agent 将审阅实际 diff。
