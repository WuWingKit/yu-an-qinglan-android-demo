# 渝安青澜 Android 技术方案

## 实现目标

构建一个稳定、流畅、可重复演示的单机 Android APP。界面和交互需要像真实产品，但所有数据与流程均在本地完成。首要目标是演示可靠性、视觉完成度与伦理边界可见性，不追求生产后端能力。

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- Gradle Kotlin DSL 与 Gradle Wrapper
- 单 Activity + Navigation Compose
- Kotlin Coroutines 与 StateFlow 管理界面状态
- kotlinx.serialization 读取本地 JSON 演示数据
- Coil 加载本地图片资源
- DataStore 保存适老模式、隐私开关等少量本地设置
- Compile SDK 36，Target SDK 36，建议 Min SDK 26

除非实现阶段发现明确必要性，不引入依赖注入框架、数据库、网络层、账号系统或多模块工程。

## 工程结构

```text
app/src/main/java/.../
  MainActivity.kt
  app/                 # AppState、导航和顶层 Scaffold
  core/designsystem/   # 色彩、排版、尺寸、公共组件
  core/model/          # 演示模型
  core/data/           # 本地 JSON、假仓库和状态
  feature/home/
  feature/burial/
  feature/memorial/
  feature/treehole/
  feature/profile/
```

这是单一 `app` Gradle 模块内的包结构，避免为演示项目制造额外构建复杂度。

## 状态与数据策略

- 所有列表、详情、政策、订单、点位、纪念内容和树洞信件来自 `assets/demo/*.json`。
- 人类与宠物使用不同数据集合与 sealed 类型，避免切换时串数据。
- 仓库接口保留异步形态并注入固定短延迟，让加载、成功、空状态和错误重试看起来真实。
- 上传、预约、生成、销毁和举报只改变内存或 DataStore 中的演示状态，并显示明确的演示反馈。
- 重启 APP 时只保留主题与设置；业务流程可一键恢复到演示初始状态。

## 导航方案

顶层为 5 个 Tab，各 Tab 保留自己的回退栈。详情页使用类型安全路由参数或受控枚举 ID，不传递自由文本和大对象。启动页不显示底部导航；进入首页后显示统一 Scaffold。

Android 系统返回键应先返回当前 Tab 内上一层；位于 Tab 根页面时，再次返回触发退出确认。重复点击当前 Tab 回到该 Tab 根页面。

## 设计系统

- 以参考 Demo v11 的 `375 x 812dp` 手机画布为视觉基准，完整令牌和组件映射见 [Demo v11 对齐规范](demo-v11-alignment.md)。
- 页面背景 `#F7F5EF`，主色 `#4F7A45`，深主色 `#3A5C32`，正文 `#2B3330`；花粉、浅蓝和浅黄只用于场景区分。
- 卡片主圆角 `16dp`，紧凑控件 `10dp`，场景大图 `22dp`；这是 v11 已建立的组件规则，不套用 Material 3 默认形状。
- 树葬、花葬、草坪葬分别使用对应场景图与浅色主题，但结构和交互模板保持一致。
- 操作图标统一使用 Material Symbols，不复制 Demo 中的 Emoji、内联 SVG 或文本符号。
- 适老模式放大字号、点击热区和对比度，同时简化次要动效；所有点击热区至少 `48dp`。
- 所有图片提供内容描述；小号弱文本不得直接照搬 `#8A908B`，需通过 WCAG 对比度检查。
- 动画仅使用淡入、轻微位移与交叉淡化，并尊重系统减少动态效果设置。

## 演示替身

| 生产能力 | 演示实现 |
| --- | --- |
| 地图与定位 | 固定园区示意图、点位高亮和路线步骤 |
| 二维码扫码 | 预置示例码入口，直接打开固定纪念馆 |
| 材料上传 | `ActivityResultContracts` 调用系统照片/文件选择器，录音使用显式权限；结果仅保存在本地演示目录 |
| AI 生成 | 固定进度动画与预置音视频结果，不调用模型 |
| 直播 | 预置封面和短视频演示资源 |
| 客服 | 本地 FAQ 与模拟对话气泡，不发送消息 |
| 预约和补贴预审 | 本地表单校验与固定规则结果，不提交真实信息 |

## 测试与验收

- ViewModel 和演示仓库的单元测试覆盖双轨隔离、流程状态与设置持久化。
- Compose UI 测试覆盖 5-Tab 导航、45 个路由可达性、关键链路和适老模式。
- 在 `Pixel_7_API_36`、端口 `5560` 上完成冷启动、旋转、返回键和完整演示链路检查。
- 输出 Debug APK、演示脚本和关键页面截图；不配置签名发布或商店上架。
