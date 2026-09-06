# 变更记录

本文件记录渝安青澜 Android APP 的可交付里程碑。提交作者统一为 `WuWingKit <hurongjie@qianban.online>`。

## [0.2.0] - 2026-09-06（开发中，未发布）

### 新增（wave 1 + wave 2：45 路由全部可达）

- **工程与设计系统**：单 `app` 模块、Kotlin + Compose + Material 3、SDK 36/26；暖白青绿设计令牌；老年模式（1.25× 排版、高对比、52dp 热区、全局持久化）；`core/ui` 14 个公共组件（AppScaffold / NoticeBanner / AudienceSegment / ServiceSceneCard / SectionHeader / ReferenceNote / ConfirmDangerDialog / EmptyState / ErrorRetry / LoadingState / 按钮 / FormTextField / InfoRow）。
- **本地数据层**：`SettingsRepository`（DataStore：老年模式/树洞总开关/昵称/头像/隐私确认/resetAll）、`DemoAssetLoader`（kotlinx.serialization 读取 `assets/demo/**` JSON，固定延迟可复现加载/成功/空/失败/重试）、`FileStorage`（私有目录图片/音频）、`AppContainer` 服务定位器。
- **首页与政策（9 路由）**：home（5 轮播、常用服务、生命教育、活动、资讯随机 4 + 换一换、老年模式开关）、life-ed、activities、match、policy（38 区县）、county-detail、presult（表单校验 + 补贴拆分 + 下一步）、sea-detail（公益海葬，无内河江葬）、news-detail（8 篇全文）。
- **安葬双轨（10 路由）**：zangshi（人/宠切换）、tree/flower/grass（共享人类详情模板）、pet-tree?mode=（宠物三模式参数化，不复制三卡同跳缺陷）、pet-park、plan、plan-form（校验 + 本地订单号）、order（四步进度时间轴）、navigate（静态底图 + Compose 点位，无定位权限）；宠物固定合规说明（无害化处理前置/无民政补贴/独立园区/场地隔离）；人宠强类型隔离。
- **追忆（15 路由）**：memorial-home（双轨列表）、memorial-create、memorial-detail（主页/相册/寄语/AI追忆/祭扫延伸 5 Tab）、memorial-main、pet-memorial、memorial-story（时间排序 + 导出含新增节点）、story-add、jisi-time、memorial-diary（图片/音频附件）、letter-write/letter-view、ai-ethics（不可跳过伦理门）、ai-upload（素材工作台 + 永久销毁）、daiji（代祭，与共祭独立）、jiti-history（线上共祭）；人/宠纪念数据强隔离。
- **树洞（3 路由）**：shudong-select（游客确认门 + 总开关联动）、shudong-ren/shudong-sheng（人间/生灵双池隔离；图片 ≤10MB、音频 ≤5MB；发布待审核 → 自动切拾信；拾信随机/换一封/轻回应无计数/举报；本人信件二次确认删除；无点赞/热度/粉丝/私信/楼中楼）。
- **我的（7 路由）**：me（头像相册选择、昵称 1-12 字、订单/纪念空间/素材/树洞开关/系统设置/恢复默认）、elder、privacy、pwd-edit、phone-edit、about（无负责人/竞赛/版本号）、feedback。
- **导航外壳**：MainShell 5 Tab（各自回退栈）+ 六个 feature NavGraph 扩展接线；RECORD_AUDIO 权限（本地录音）。

### 修复

- 追忆列表初始化：`MemorialTrackStore.spaces()/observe()` 冷流收集时触发种子/快照载入，避免首页永远停在加载态。
- 树洞总开关联动：`TreeholeSelectScreen` 依据 `SettingsRepository.treeholeEnabled` 展示"已关闭"状态，关闭时双池入口不可达（验证关闭/开启往返）。
- 单元测试计数（wave 2 集成后）：28 套件 / 175 用例全绿。

### 质量

- `testDebugUnitTest`：28 套件 / 175 用例 / 0 失败 / 0 错误。
- `assembleDebug`：BUILD SUCCESSFUL，APK 输出至 `E:\Gradle\project-builds\yuanqinglan\app\outputs\apk\debug\app-debug.apk`。
- `connectedDebugAndroidTest`：Compose UI 测试（核心组件 + 跨模块 5-Tab/个人中心关键链路）。
- Pixel_7_API_36（emulator-5560）冷启动成功；关键链路手动巡检无崩溃。
- 全部项目自有源码带版权头；对外文案无"演示/假数据/原型"字样；人宠/双池数据强隔离；AI 追忆不做实时对话、可永久销毁。

### 已知限制

- 订单/素材跨模块聚合：个人中心订单入口为诚实空态并跳转安葬 Tab（burial 订单为模块内内存态）。
- 音频/相册大图解码、录音与播放需真机复核；运行快照为尽力而为（模型变更后旧快照回退种子）。
- APK 未签名发布；无后端、无真实支付/定位/政务提交。
