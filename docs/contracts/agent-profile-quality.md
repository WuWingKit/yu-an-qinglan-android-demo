# 任务书：profile_quality 子代理

你是「渝安青澜」Android APP 多子代理开发中的 `profile_quality` 子代理。你负责个人中心（我的）模块与跨模块质量加固。wave-1/wave-2 其他子代理并行推进；公共契约已冻结，你只准改自己的目录。

请完整阅读本文件并**从第一项开始顺序执行**。全部完成后按文末「交付报告」格式汇报。

## 工程位置与环境

- 工程根目录：`E:\WorkSpace\生态殡葬`（Windows，路径含中文）。
- 当前工作分支 `codex/issue-2-12-app-implementation`。**不要执行任何 git 命令**，只编辑文件。
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
- `docs/iteration-2026-09-05-alignment.md`（我的/适老增量）
- `docs/demo-v11-alignment.md`（视觉令牌）
- 现有代码：`navigation/AppRoute.kt`、`core/**`、`data/local/**`、`app/**`、其他 feature 的 NavGraph 扩展写法

## 你的独占目录（只准改这些）

- `app/src/main/java/com/yuanqinglan/app/feature/profile/**`
- `app/src/main/assets/demo/profile/**`
- `app/src/test/java/.../feature/profile/**`
- 跨模块测试：`app/src/test/java/.../cross/` 与 `app/src/androidTest/.../cross/`（可补充，但不得改其他 feature 的源码）

禁止改动：`core/**`、`data/local/**`、`app/**`、`navigation/**`、其他 feature 源码、`res/drawable-nodpi/**`。如发现契约缺失，在交付报告中向主 Agent 提出变更建议，不要自行改 core/。

## 个人中心模块：8 个路由

你负责：`me`、`elder`、`privacy`、`pwd-edit`、`phone-edit`、`about`、`feedback`。（其中 `me` 为 5 Tab 之一的根。）

NavHost 接入：`fun NavGraphBuilder.profileNavGraph(navController: NavHostController)`（feature/profile 包内），注册上述路由。

### me（我的 Tab 根）
- 顶部个人卡：头像（系统相册选择，圆形裁切展示；用 `ActivityResultContracts.PickVisualMedia` 调用系统相册）、昵称（1-12 字，本地校验与持久化，点击可编辑）；头像/昵称存 `SettingsRepository`（avatarUri / nickname）。
- 分组设置列表（Material Icons，禁止 Emoji）：
  - 业务：我的订单（→ 展示本地订单入口，复用 burial 订单数据或本地摘要）、我的纪念空间（→ memorial 入口）、素材管理（本地相册/音频/生成内容列表，可删除）
  - 树洞：树洞总开关（Switch，`SettingsRepository.treeholeEnabled`，关闭后树洞 Tab 同步不可用）
  - 系统：老年模式（→ `elder`）、账号与隐私（→ `privacy`）、修改密码（→ `pwd-edit`）、更换手机号（→ `phone-edit`）、关于渝安青澜（→ `about`）、意见反馈（→ `feedback`）
  - 数据：恢复默认设置（`SettingsRepository.resetAll()`，二次确认）
- 危险操作（删除素材/销毁内容/重置）使用 `ConfirmDangerDialog` 二次确认并反馈结果。

### elder（老年模式设置）
- 展示当前状态（Switch，读写 `SettingsRepository.elderMode`，全局生效，首页右上角开关与本页共用同一状态）。
- 说明开启后的变化（字号、触达、对比度、减少动效）。

### privacy（账号与隐私）
- 隐私开关列表（DataStore 持久化）：如"允许保存浏览偏好""树洞匿名展示"等；权限说明（相机/相册/麦克风仅用于本地选择与录音，不对外传输）；`privacyAccepted` 状态展示。

### pwd-edit / phone-edit
- 修改密码：原密码、新密码、确认新密码；本地校验（长度、一致、原密码匹配本地预设），成功反馈。
- 更换手机号：旧号校验、新号格式校验、验证码演示输入（本地状态），成功反馈。不自动提交真实个人信息。

### about
- 产品定位介绍、合规句（"相关信息仅供参考…"）。**不显示项目负责人、竞赛标签、页面内版本号**。不要出现"演示/原型"字样。

### feedback
- 意见反馈表单（类型选择、正文、可选图片附件）；本地校验；提交后进入"已收到"结果页（本地状态，可再次提交）。附合规句。

## 跨模块质量加固（在你的测试目录内）

- 为关键设置链路写单元测试：elderMode 持久化与复位、昵称长度边界（1/12/13）、树洞开关状态、隐私开关、密码/手机号表单校验。
- 补充 Compose UI 测试（`app/src/androidTest`）：5-Tab 底部导航切换、老年模式开关全局生效、个人中心关键链路。
- 检查并汇报（不自改）你发现的其他模块与契约不符之处，供主 Agent 整合修复。

## 硬性要求

1. 版权头：每个新建项目自有 Kotlin/XML 源文件顶部加：
```
/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */
```
2. 对外文案禁止"演示""假数据""原型"等字样；关于页无负责人/竞赛/版本号；头像昵称图片只存私有目录或本地设置；不自动提交真实个人信息。
3. 单向数据流、不可变 UI 状态、ViewModel + Coroutines + StateFlow。
4. 图标统一 Material Icons，禁止 Emoji/文字符号。
5. 所有主要控件真实响应且可重复操作；加载/成功/空/失败/重试状态可复现。
6. 不运行任何 git 命令；不启动模拟器（主 Agent 统一管理设备）。

## 验证门禁（交付前必须完成并记录）

1. 运行 `.\gradlew.bat testDebugUnitTest assembleDebug`，记录结果（若因其他 wave 未合并而失败，如实说明，不伪造通过）。
2. 单元测试至少覆盖：设置持久化（elderMode/nickname/treeholeEnabled）、昵称边界、表单校验、resetAll 复位。
3. `rg -L "Copyright \(c\) 2026" app/src/main/java/com/yuanqinglan/app/feature/profile` 自查版权头并补齐。

## 交付报告（最终消息必须包含）

1. 修改/新增文件清单。
2. 8 个路由完成情况逐项说明。
3. 实际构建/测试命令与结果。
4. 发现的跨模块问题清单（供主 Agent 处理）。
5. 与契约偏离、剩余风险。
