# 渝安青澜 Android APP 多 Agent 开发总提示词

以下内容可直接作为后续主 Agent 的任务提示词。主 Agent 必须先阅读完整提示词，再组织多个子代理实施，不得在未核对现有工作区的情况下重新初始化或覆盖工程。

---

你现在负责继续完成“渝安青澜”Android APP。项目位于：

```text
E:\WorkSpace\生态殡葬
```

GitHub 仓库：

```text
https://github.com/WuWingKit/yu-an-qinglan-android-demo
```

## 一、工作方式：必须采用多子代理协作

你是主协调 Agent，必须使用多个子代理并行开发，自己负责架构裁决、公共契约、合并、回归测试和最终交付。不要把整个 APP 独自串行完成。

开始后先检查当前可用并发槽位，再至少创建 3 个有明确边界的子代理。子代理允许在完成一个工作包后继续领取下一工作包。推荐编排如下：

1. `foundation_navigation`：检查并完善工程基础、设计系统、公共组件、导航契约、本地数据接口和测试基础。
2. `home_policy`：负责首页、生命教育、活动、资讯、智能匹配、政策与区县预审链路。
3. `burial_services`：负责人类与宠物生态安葬、套餐、预约、订单和园区导航。
4. 第一批任务完成后，将空闲子代理重新分配为 `memorial_treehole` 和 `profile_quality`，完成追忆、树洞、个人中心及全量质量验证。

协作规则：

- 主 Agent 必须先定义公共接口和目录所有权，再让子代理开始编辑。
- 不允许两个 Agent 同时修改 `AppRoute.kt`、顶层 `Scaffold`、主题令牌或同一功能目录。
- 子代理只能编辑分配给自己的目录；需要修改公共契约时，先向主 Agent 提交变更建议。
- 每个子代理交付时必须报告修改文件、完成路由、测试结果、截图状态和剩余风险。
- 主 Agent 必须审阅每个子代理的实际 diff，不能只接受文字结论。
- 发现其他 Agent 或用户已有改动时必须保留并与之协作，禁止擅自回退、覆盖或清理。
- 先合并基础契约，再合并各业务功能，最后执行跨模块回归与视觉验收。

## 二、开始前必须完成的检查

依次阅读以下文件：

```text
README.md
docs/iteration-2026-09-05-alignment.md
docs/product-baseline.md
docs/demo-v11-alignment.md
docs/technical-plan.md
docs/roadmap.md
docs/agent-handoff.md
assets/README.md
```

随后执行：

```powershell
git status --short
git branch --show-current
git log -5 --oneline
git config user.name
git config user.email
```

当前工程基础位于分支：

```text
codex/issue-1-compose-foundation
```

这批工程文件目前可能仍是未提交状态。必须先阅读、构建和确认它们，不得删除后重新创建。提交身份必须保持：

```text
WuWingKit <hurongjie@qianban.online>
```

## 三、需求与设计权威顺序

发生冲突时按以下顺序裁决：

1. 用户最新明确指令。
2. `docs/iteration-2026-09-05-alignment.md` 中记录的 2026.09.05 Word 迭代要求。
3. 在线参考界面的布局、视觉气质、信息密度和可见交互。
4. `docs/product-baseline.md`、`docs/demo-v11-alignment.md` 等较早资料。

参考界面：

```text
https://1926e0d2e0b94540aaebdfef3ce96faa.app.workbuddy.link/
```

全量视觉基线：

```text
docs/reference/demo-v11-full/
```

快速回归基线：

```text
docs/reference/demo-v11/
```

Figma：

```text
https://www.figma.com/design/kadmc6qv1AbxxzUMoTh15m
```

注意：Figma 封面仍有旧统计数字。正确基线是 **45 个路由、46 张参考截图（含桌面总览）、22 张分类素材**，以仓库文档为准。

## 四、产品定位与对外文案

将产品定位为面向重庆地区的生态安葬、生命纪念、情绪陪伴与个人服务 Android APP。界面、应用名称、关于页面、按钮、提示语和交付说明中不得出现“演示”“纯前端演示”“原型”“假数据”等字样，也不要把版本号写进普通业务页面。

当前不建设远程后端，业务数据、状态变化和主要流程先在本地完成。不要因为没有后端而制作空壳页面：所有主要按钮、Tab、表单、筛选、开关、上传选择、确认、删除、重置、空状态、错误状态和结果页都必须可交互并能重复操作。

政策、价格、补贴、预约、机构、区县办理结果等可能被理解为真实承诺的内容，统一使用克制的合规说明，例如：

```text
相关信息仅供参考，具体政策、费用与办理结果以主管机构和服务机构最终公布为准。
```

不得伪造官方背书，不接入真实支付，不自动提交真实个人信息，不暗示已经完成政务审批。用户选择的头像、昵称、图片、录音和纪念内容只保存在应用私有目录或本地设置中。

## 五、版权与源代码标注要求

项目自有源代码的著作权归属必须清晰可见。所有新建或大幅修改的项目自有 Kotlin、Java、Gradle 脚本及其他核心源码文件，应在文件顶部加入以下声明；关键架构文件、公共组件和业务实现文件不得遗漏：

```kotlin
/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */
```

XML 文件使用等价的 XML 注释：

```xml
<!--
  Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
  本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
  未经书面授权禁止另做他用（包括商用和非商用）。
-->
```

版权执行规则：

- 对项目自主编写的源码保持高覆盖率，新增源码必须带声明。
- 对复杂核心逻辑，可在文件内关键架构段落附近补充简短的著作权归属注释，但不要在每个函数上机械重复，避免损害可读性。
- 不得把该声明写入 Gradle Wrapper、第三方库、外部复制代码、自动生成文件或第三方许可证文件，也不得删除或覆盖第三方原有版权信息。
- 引入第三方代码前确认许可证兼容性，并在必要时保留来源与许可证声明。
- 提交前用 `rg` 检查新增项目源码的声明覆盖情况。

## 六、现有工程状态

基础工程已经完成以下内容，必须在此基础上继续：

- 单 `app` 模块，包名与命名空间为 `com.yuanqinglan.app`。
- Kotlin、Jetpack Compose、Material 3、Navigation Compose。
- Compile SDK 36、Target SDK 36、Min SDK 26。
- Gradle 9.1.0、AGP 9.0.1、Kotlin 2.2.10。
- 暖白与青绿色设计令牌、基础排版和尺寸令牌。
- `AudienceTrack`、`DemoState`、`DemoDisclosure` 等基础模型。
- 45 个稳定 `AppRoute` 键和 5 个顶层导航目的地。
- 启动页、基础首页、五栏底部导航及其他模块的临时入口页。
- 启动背景与首页五张 Banner 已放入 `drawable-nodpi`。
- 路由数量与唯一性单元测试已经通过。
- Debug APK 已构建并在 `Pixel_7_API_36 / emulator-5560` 上冷启动成功。

现有临时界面中仍可能出现“进入演示”“演示数据”等旧文字，后续开发必须按本提示词第四节改成正式产品口径。

## 七、工程架构与目录边界

保持单 Activity、单 `app` 模块，不引入没有明确必要性的依赖注入框架、数据库、网络层或多模块拆分。

推荐结构：

```text
app/src/main/java/com/yuanqinglan/app/
  MainActivity.kt
  app/                    # AppState、顶层 Scaffold、启动流程
  navigation/             # AppRoute、NavHost、顶层导航
  core/designsystem/      # 颜色、排版、尺寸、主题
  core/model/             # 跨功能稳定模型
  core/ui/                # 公共 Compose 组件
  data/local/             # 本地 JSON、文件与设置存储
  feature/home/
  feature/policy/
  feature/burial/
  feature/memorial/
  feature/treehole/
  feature/profile/
```

架构约束：

- 使用单向数据流、不可变 UI 状态、ViewModel、Coroutines 和 StateFlow。
- 人类与宠物服务、纪念空间、树洞内容必须使用强类型隔离，不能依赖可空字段或字符串临时过滤。
- 导航只传稳定 ID 或受控枚举，不传大对象和自由文本。
- 共享模板用于葬式详情、纪念空间、表单和设置页，禁止复制 45 套近似页面。
- 少量设置使用 DataStore；图片和音频使用应用私有目录；结构化内容使用本地 JSON 或明确的序列化模型。
- 上传通过系统选择器；录音使用系统能力并正确处理权限拒绝。
- 不建设真实支付、定位、直播、AI 推理或政务提交能力；需要这些能力的流程采用可交互的本地状态实现。

## 八、45 个路由范围

必须保持以下路由键稳定，并保证最终全部可以从界面到达：

```text
splash
home, life-ed, activities, match, policy, county-detail, presult, sea-detail, news-detail
zangshi, tree, flower, grass, pet-tree, pet-park, plan, plan-form, order, navigate
memorial-home, memorial-create, memorial-detail, memorial-main, pet-memorial,
memorial-story, story-add, jisi-time, memorial-diary, letter-write, letter-view,
ai-ethics, ai-upload, daiji, jiti-history
shudong-select, shudong-ren, shudong-sheng
me, elder, privacy, pwd-edit, phone-edit, about, feedback
```

宠物树葬、花葬、草坪葬使用共享参数化详情模板，但三种配置、图片与内容必须正确区分。不要复制参考网页中三张宠物卡都进入 `pet-tree` 的缺陷。

## 九、核心功能要求

### 首页与政策

- 首页包含重庆山城、纪念林地、花海、草坪和宠物草地 5 张轮播图。
- 包含常用服务、生命教育、近期活动、资讯和政策入口。
- 资讯准备 8 篇完整正文，首页随机显示 4 条并支持换一换。
- 区县查询覆盖重庆 38 个区县。
- 政策预审包含区县选择、表单校验、结果拆分和下一步提示。
- 首页右上角提供老年模式开关并持久化。

### 生态安葬

- 人类与宠物分别支持树葬、花葬和草坪葬。
- 宠物服务明确无害化处理前置、无民政补贴、独立园区及场地隔离。
- 套餐、预约表单、订单进度和园区导航均可操作。
- 园区导航使用静态底图与 Compose 点位，不申请真实定位。

### 追忆

- 纪念空间支持创建、管理及人类/宠物数据隔离。
- 详情包含主页、相册、寄语、AI 追忆、祭扫延伸 5 个页内 Tab。
- 相册支持多选、三列网格、放大、删除与二次确认。
- 日记图片和音频附件可新增、编辑回填、查看、播放及删除。
- 生命故事保存后按时间排序，导出内容包含新增节点。
- 线上集体共祭与异地代祭必须保持独立，不能混成同一付费流程。
- AI 追忆强调授权、私人访问、用途透明和永久销毁，不制作逝者实时对话。

### 树洞

- 人间与生灵使用两套独立内容池和本地存储。
- 图片不超过 10MB，音频不超过 5MB。
- 发布后进入待审核状态并切换到拾信模式。
- 本人内容支持二次确认删除。
- 不显示点赞计数、地域、热度、粉丝、私信或楼中楼评论。

### 我的

- 头像从系统相册选择并圆形展示；昵称限制 1 至 12 个字并持久化。
- 包含订单、纪念空间、素材管理、老年模式、隐私、密码、手机号、关于和反馈。
- 关于页不显示项目负责人、竞赛标签或页面内版本号。
- AI 素材和生成内容提供高优先级永久销毁入口。

## 十、视觉要求

- 以 `375 x 812dp` 为主要比对画布，适配 Pixel 7 API 36。
- 页面背景 `#F7F5EF`，主色 `#4F7A45`，深主色 `#3A5C32`，正文 `#2B3330`。
- 主内容水平边距 `14dp`；卡片圆角 `16dp`；紧凑控件 `10dp`；大场景图 `22dp`。
- 页面标题约 `22sp / SemiBold`，区块标题 `17sp / SemiBold`，正文 `13-14sp`。
- 点击热区至少 `48dp`，底栏需要处理系统导航安全区。
- 图标统一使用 Material Icons，不使用 Emoji、文字符号、手绘 SVG 或 CSS 式占位图。
- 使用仓库已有真实 WebP 素材，遵循 `assets/README.md` 的裁切与页面落位要求。
- 禁止卡片套卡片；页面区块保持清晰、克制、适合重复操作。
- 普通与老年模式均不得出现文字截断、重叠、横向滚动、图片拉伸或底栏遮挡。
- 每个阶段把 APP 截图与同尺寸参考截图并排检查，不能只凭主观印象验收。

## 十一、开发环境

系统与工具：

```text
Windows 11
Android Studio 2026.1.4
Android Studio JBR: C:\Program Files\Android\Android Studio\jbr
Java: OpenJDK 25.0.3
Android SDK: E:\Android\Sdk
Android AVD: E:\Android\Avd
Gradle User Home: E:\Gradle
Android API 36 / Build Tools 36.0.0
ADB: E:\Android\Sdk\platform-tools\adb.exe
Emulator: E:\Android\Sdk\emulator\emulator.exe
AVD: Pixel_7_API_36
固定端口: emulator-5560
```

PowerShell 构建前建议显式设置：

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "E:\Android\Sdk"
$env:ANDROID_SDK_ROOT = "E:\Android\Sdk"
$env:ANDROID_AVD_HOME = "E:\Android\Avd"
$env:GRADLE_USER_HOME = "E:\Gradle"
```

由于工作目录包含中文：

- `gradle.properties` 已设置 `android.overridePathCheck=true`。
- `app/build.gradle.kts` 会在检测到非 ASCII 工程路径时，将临时构建输出改到 `E:\Gradle\project-builds\yuanqinglan\app`，避免 Windows JUnit 类路径编码问题。
- 中文仓库路径不需要迁移，也不要删除这一兼容逻辑。
- 在英文路径环境中仍使用标准模块构建目录。

Google 仓库在本机可能连接超时，`settings.gradle.kts` 已配置阿里云只读镜像并保留官方仓库后备。不要随意移除镜像或改成不受信任的动态依赖版本。

## 十二、构建、设备与测试命令

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat connectedDebugAndroidTest
```

启动 Pixel 模拟器：

```powershell
& "E:\Android\Sdk\emulator\emulator.exe" `
  -avd Pixel_7_API_36 `
  -port 5560 `
  -no-snapshot-load `
  -gpu auto
```

检查设备时必须明确指定 `emulator-5560`。本机已有 ROG Phone 环境占用 5554/5555，禁止误装、关闭、重置或清除其数据。

```powershell
& "E:\Android\Sdk\platform-tools\adb.exe" devices -l
& "E:\Android\Sdk\platform-tools\adb.exe" -s emulator-5560 shell getprop sys.boot_completed
```

中文目录环境的 APK 当前位于：

```text
E:\Gradle\project-builds\yuanqinglan\app\outputs\apk\debug\app-debug.apk
```

安装与启动：

```powershell
& "E:\Android\Sdk\platform-tools\adb.exe" `
  -s emulator-5560 install -r `
  "E:\Gradle\project-builds\yuanqinglan\app\outputs\apk\debug\app-debug.apk"

& "E:\Android\Sdk\platform-tools\adb.exe" `
  -s emulator-5560 shell am start -W `
  -n com.yuanqinglan.app/.MainActivity
```

## 十三、质量门槛

每个工作包完成时至少满足：

1. 所属路由可从界面进入，系统返回键与底部 Tab 行为正确。
2. 加载、成功、空状态、失败及重试状态可以稳定复现。
3. 所有主要控件真实响应，不留下不可点击的主要按钮。
4. 人类与宠物数据隔离测试通过。
5. 普通与老年模式完成布局检查和 TalkBack 基本检查。
6. 新增逻辑具有单元测试；关键用户链路具有 Compose UI 测试。
7. `testDebugUnitTest`、`assembleDebug` 通过；最终里程碑执行 `connectedDebugAndroidTest`。
8. 检查 Logcat，不允许本应用崩溃、ANR 或持续错误日志。
9. 截图与 `docs/reference/demo-v11-full/` 同状态并排比较并修正明显差异。
10. 项目自有新增源码已加入指定版权声明。

## 十四、Git 与交付规则

- 不直接覆盖用户或其他 Agent 的未提交改动。
- 分支使用 `codex/issue-<编号>-<主题>`。
- 提交作者必须是 `WuWingKit <hurongjie@qianban.online>`。
- 不提交 `local.properties`、`.gradle/`、`.idea/`、构建产物、APK、密钥、真实个人资料或本机缓存。
- 每个提交保持单一目的，提交前运行相关测试并检查 `git diff --check`。
- 每个 PR 对应明确 Issue，并在描述中列出完成路由、截图、测试命令、数据存储方式与剩余限制。
- 主 Agent 在最终合并前执行 45 路由巡检，不以“能够编译”代替功能验收。

## 十五、主 Agent 最终汇报格式

最终向用户汇报：

1. 已完成模块与 45 路由覆盖数量。
2. 参与的子代理、各自工作包和合并结果。
3. APK 路径、测试结果及 Pixel 7 安装运行结果。
4. 关键截图和与参考界面的差异修正情况。
5. 数据、权限、隐私及人宠隔离的验证结果。
6. Git 分支、提交 SHA、PR 或远端同步状态。
7. 尚未完成的事项和明确风险，不得把未验证内容描述为已完成。

开始执行时，先汇报对现有工作区、未提交工程和权威文档的理解，再建立多子代理任务树。完成基础审计后立即进入实现，不要只输出计划。

---
