# 渝安青澜 Android Demo v11 开发交接

## 1. 接手入口

| 项目 | 地址或版本 |
| --- | --- |
| GitHub | <https://github.com/WuWingKit/yu-an-qinglan-android-demo> |
| 参考 Demo v11 | <https://1926e0d2e0b94540aaebdfef3ce96faa.app.workbuddy.link/> |
| Figma 交接文件 | <https://www.figma.com/design/kadmc6qv1AbxxzUMoTh15m> |
| 设计基线 | `docs/demo-v11-alignment.md` |
| 路由与实现路线 | `docs/roadmap.md` |
| 素材清单 | `assets/README.md` |
| 当前基线提交 | 以 `main` 最新提交为准；提交身份必须是 `WuWingKit <hurongjie@qianban.online>` |

Figma 文件已经建立并写入 `00 交接总览`。Starter 方案触发 MCP 调用上限后，参考界面、素材落位和分工细节改由本仓库的可版本化文档承载；不要把 Figma 当前只有封面误判为产品范围缺失。17 张验收截图位于 `docs/reference/demo-v11/`，是开发和视觉回归的直接证据。

## 2. 不可变产品边界

1. 这是纯前端演示 APP，不建设后端 API，不连接真实支付、地图、AI、政务或直播系统。
2. 所有姓名、手机号、金额、预约号、审核结果和纪念内容必须明确是虚构演示数据。
3. 人类与宠物服务、纪念空间、树洞内容池必须强类型隔离，禁止用一个可空字段勉强复用业务对象。
4. AI 追忆只演示授权、素材选择、生成状态和销毁，不模拟逝者实时对话。
5. 树洞不提供点赞计数、粉丝、私信、楼中楼或热度排行；游客不可发布。
6. 不展示重庆内河撒江入口，不做忌日强提醒，不量化思念行为。
7. Demo 中残留的 `">` 标题字符、Emoji 图标、过弱灰字和过重删除按钮属于已知缺陷，不得复制。

## 3. 视觉实现基线

- 参考画布：`375 x 812dp`；Pixel 7 API 36；主内容水平边距 `14dp`。
- 页面背景 `#F7F5EF`；卡片 `#FFFFFF`；主色 `#4F7A45`；深主色 `#3A5C32`。
- 正文 `#2B3330`；次文本 `#5A6562`；小字号不要直接使用对比不足的 `#8A908B`。
- 卡片默认圆角 `16dp`，紧凑控件 `10dp`，大场景 `22dp`；嵌套卡片禁止出现。
- 页面标题 `22sp / SemiBold`，区块标题 `17sp / SemiBold`，正文 `13-14sp`。
- 点击热区至少 `48dp`；底栏视觉高度约 `78dp`，必须叠加系统导航安全区。
- 所有 UI 图标统一使用 Material Symbols 或 Material Icons；图片中不烘焙按钮、标签或正文。
- 普通模式与适老模式都要检查文字换行、卡片高度、图片裁切和底栏遮挡。

## 4. 素材落位表

### 首页与服务聚合

| 文件 | 页面/组件 | 裁切与叠字 |
| --- | --- | --- |
| `assets/generated/splash_chongqing_dawn.webp` | `splash` 全屏背景 | `ContentScale.Crop`；品牌文字和按钮由 Compose 绘制 |
| `assets/generated/v11/home/home_carousel_tree.webp` | `home` 首页轮播：树葬 | `2:1`；文字放左侧低细节区；焦点保留右侧树群 |
| `assets/generated/v11/home/home_carousel_flower.webp` | `home` 首页轮播：花葬 | `2:1`；左侧叠标题；避免将花园裁成私人墓位观感 |
| `assets/generated/v11/home/home_carousel_lawn.webp` | `home` 首页轮播：草坪葬 | `2:1`；中央草坪允许叠短文案；保留右侧路径 |

### 活动与生命教育

| 文件 | 页面/组件 | 裁切与无障碍描述 |
| --- | --- | --- |
| `assets/generated/v11/activities/activity_collective_memorial.webp` | `activities` 集体纪念活动卡、`news-detail` 头图 | `4:3` 中心裁切；描述“市民在生态纪念园敬献鲜花” |
| `assets/generated/v11/activities/activity_life_education.webp` | `life-ed` 活动卡或课程详情头图 | `4:3`；描述“成人与儿童共同栽种树苗” |

### 安葬详情

| 文件 | 页面/组件 |
| --- | --- |
| `assets/generated/burial_tree_grove.webp` | `tree` 详情头图、树葬场景卡 |
| `assets/generated/burial_flower_garden.webp` | `flower` 详情头图、花葬场景卡 |
| `assets/generated/burial_lawn.webp` | `grass` 详情头图、草坪葬场景卡 |
| `assets/generated/burial_pet_tree.webp` | `pet-tree` 与 `pet-park` 场景卡 |
| `assets/generated/v11/utility/park_overview_map.webp` | `navigate` 园区底图；Compose 在上层绘制点位、图例和路线 |

### 追忆与 AI 演示

| 文件 | 页面/组件 | 使用约束 |
| --- | --- | --- |
| `assets/generated/memorial_human_portrait.webp` | 人类纪念空间头像 | 必须显示“演示人物”语义 |
| `assets/generated/memorial_pet_portrait.webp` | 宠物纪念空间头像 | 必须显示“演示宠物”语义 |
| `assets/generated/v11/memorial/memorial_gallery_family_tea.webp` | `memorial-detail` / `memorial-main` 相册格 | 虚构家庭照片，不作为真实案例宣传 |
| `assets/generated/v11/memorial/memorial_gallery_pet_park.webp` | `pet-memorial` 相册格 | 虚构宠物照片，不暗示真实宠物档案 |
| `assets/generated/v11/utility/ai_restore_sample_faded.webp` | `ai-upload` 选择器示例与修复前预览 | 只作为本地输入样本；结果页可用同图的轻度色彩处理演示 |

Android 接入时建议复制到 `app/src/main/res/drawable-nodpi/`，资源名保持不变。大图不要放入密度目录，避免系统二次缩放；轮播图固定 `aspectRatio(2f)`，活动/相册图固定 `aspectRatio(4f / 3f)`。人物、宠物和活动照片设置明确的 `contentDescription`，纯氛围背景设置为 `null`。

## 5. 推荐 Agent 分工

为降低合并冲突，先由基础 Agent 建立工程和公共契约，再并行启动页面 Agent。任何 Agent 不得自行改写另一工作包拥有的导航、主题或演示数据协议。

| 工作包 | 对应 Issues | 独占目录建议 | 交付内容 |
| --- | --- | --- | --- |
| A 基础与设计系统 | #1、#2、#3 | `app/src/main/java/.../core/`、`ui/theme/`、`navigation/`、`data/demo/` | Compose 工程、主题、公共组件、46 路由、强类型本地数据 |
| B 首页与政策 | #4 | `feature/home/`、`feature/policy/` | 启动、首页聚合、生命教育、活动、资讯、政策预审 9 路由 |
| C 安葬双轨 | #5、#6 | `feature/burial/` | 人类三种生态葬、宠物园区、套餐、预约、订单、园区导航 |
| D 追忆与树洞 | #7、#8 | `feature/memorial/`、`feature/treehole/` | 人宠纪念空间、故事、信件、AI 伦理与素材、双池树洞 |
| E 我的与质量 | #9、#10、#11、#12 | `feature/profile/`、`androidTest/`、`test/`、`docs/release/` | 设置、适老、隐私、全路由巡检、APK 与演示脚本 |

### 合并顺序

1. A 合并 #1 后，建立 `AppRoute`、主题令牌、公共组件接口和假仓库接口。
2. A 完成 #2/#3 的可编译骨架后，B/C/D/E 从同一 `main` 提交点分支。
3. B/C/D 只通过公共接口取数据；需要新增接口时先提交小型契约 PR，再继续页面实现。
4. E 在功能分支合并后补跨模块测试；不要让测试分支反向承担功能开发。
5. 最后按 #12 生成 APK、截图和 5-8 分钟演示脚本。

### 分支与提交约定

- 分支：`codex/issue-<编号>-<简短主题>`，例如 `codex/issue-4-home-policy`。
- 一个 PR 只关闭明确列出的 Issue；公共契约变化必须在 PR 描述中列出影响方。
- 提交身份：`WuWingKit <hurongjie@qianban.online>`。
- 不提交 `local.properties`、SDK、模拟器文件、构建产物、真实联系人或原始调研文档。

## 6. 公共代码契约建议

基础 Agent 应优先冻结以下结构，让页面 Agent 可以并行：

```text
core/model/
  AudienceTrack.kt       // HUMAN / PET
  DemoState.kt           // normal/loading/empty/success/error
  DemoDisclosure.kt      // 演示数据标识
core/ui/
  AppScaffold.kt
  NoticeBanner.kt
  AudienceSegment.kt
  ServiceSceneCard.kt
  DemoDataBadge.kt
  ConfirmDangerDialog.kt
navigation/
  AppRoute.kt             // 46 个稳定 route key
  TopLevelDestination.kt  // 5 个一级 Tab
data/demo/
  DemoRepository.kt
  ResetDemoDataUseCase.kt
```

`AudienceTrack` 必须是非空类型；人类和宠物仓库对外返回各自模型，不允许页面层通过字符串过滤同一列表。导航参数只传稳定 ID，具体对象由假仓库读取，避免 Bundle 中塞入可变模型。

## 7. 每个 Agent 的完成定义

1. 所属路由全部可从 UI 到达，返回键与 Tab 回退栈行为正确。
2. 加载、成功、空状态和关键失败状态使用可重放本地数据。
3. 页面不出现真实网络请求、真实手机号、真实定位或真实上传。
4. 所有主要按钮、开关、分段选择和表单字段可交互；结果状态能重置。
5. 普通与适老模式均无截断、重叠、横向滚动或底栏遮挡。
6. TalkBack 能读出图片意义、选中状态、错误信息和危险操作。
7. 新增逻辑有单元测试；关键演示流有 Compose UI 测试。
8. PR 描述列出截图、测试命令、演示数据说明和已知限制。

## 8. 验证命令与截图矩阵

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat assembleDebug
adb -s emulator-5560 install -r app\build\outputs\apk\debug\app-debug.apk
```

首轮截图至少覆盖：`splash`、`home`、`life-ed`、`policy`、`presult`、`zangshi`、`tree`、`plan-form`、`memorial-home`、`memorial-detail`、`ai-upload`、`shudong-select`、`shudong-ren`、`me`、`elder`、`privacy`。参考图逐一位于 `docs/reference/demo-v11/`。

验收时把同一 `375 x 812dp` 状态的参考图和 APP 截图并排比较，检查背景、卡片尺寸、圆角、间距、标题层级、图片焦点、选中态、系统栏和安全区；“看起来接近”不等于通过。

## 9. 接手前检查清单

- [ ] 已阅读 `README.md`、`docs/product-baseline.md`、`docs/demo-v11-alignment.md` 和本文件。
- [ ] 已确认领取 Issue、独占目录和依赖提交点。
- [ ] 已配置 Git 提交邮箱为 `hurongjie@qianban.online`。
- [ ] 已在 Pixel 7 API 36 / `emulator-5560` 上完成空工程构建验证。
- [ ] 已核对素材用途，不把图片中的视觉元素当成可点击 UI。
- [ ] 已在 PR 中声明所有演示数据和未实现的真实能力。
