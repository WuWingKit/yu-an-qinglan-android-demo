# 渝安青澜视觉素材清单

本目录保存用于 Android 演示 APP 的生成式视觉素材。全部图像由内置 `imagegen` 生成，人物与宠物均为虚构对象；画面不含品牌文字、标志、水印或真实业务信息，界面文案应由 Compose 原生组件叠加。

| 文件 | 建议用途 | 内容描述 |
| --- | --- | --- |
| `generated/splash_chongqing_dawn.webp` | 启动页全屏背景 | 重庆山城、江面、山林与晨光的竖版插画 |
| `generated/burial_tree_grove.webp` | 树葬卡片与详情头图 | 山地纪念林、公共纪念墙和献花平台 |
| `generated/burial_flower_garden.webp` | 花葬卡片与详情头图 | 杜鹃、白菊、桂花与公共纪念花园 |
| `generated/burial_lawn.webp` | 草坪葬卡片与详情头图 | 开阔草坪、低矮公共纪念墙和微型卧石 |
| `generated/burial_pet_tree.webp` | 宠物树葬卡片与详情头图 | 独立花园中的纪念树、爪印木牌和花束 |
| `generated/memorial_human_portrait.webp` | 人类云端纪念馆演示头像 | 虚构重庆老年男性家庭肖像 |
| `generated/memorial_pet_portrait.webp` | 宠物纪念空间演示头像 | 虚构金色小型犬家庭肖像 |

## Demo v11 补充素材

| 分类 | 文件 | 规格 | 主要落位 |
| --- | --- | --- | --- |
| 首页 | `generated/v11/home/home_carousel_tree.webp` | `1774 x 887`，2:1 | 首页树葬轮播，左侧叠 Compose 文案 |
| 首页 | `generated/v11/home/home_carousel_flower.webp` | `1774 x 887`，2:1 | 首页花葬轮播，左侧叠 Compose 文案 |
| 首页 | `generated/v11/home/home_carousel_lawn.webp` | `1774 x 887`，2:1 | 首页草坪葬轮播，中央低细节区叠短文案 |
| 活动 | `generated/v11/activities/activity_collective_memorial.webp` | `1448 x 1086`，4:3 | 集体纪念活动卡、资讯头图 |
| 活动 | `generated/v11/activities/activity_life_education.webp` | `1448 x 1086`，4:3 | 生命教育活动卡或详情头图 |
| 纪念 | `generated/v11/memorial/memorial_gallery_family_tea.webp` | `1448 x 1086`，4:3 | 人类纪念空间相册格 |
| 纪念 | `generated/v11/memorial/memorial_gallery_pet_park.webp` | `1448 x 1086`，4:3 | 宠物纪念空间相册格 |
| 工具 | `generated/v11/utility/park_overview_map.webp` | `1536 x 1024`，3:2 | 园区导航底图，上层叠 Compose 点位 |
| 工具 | `generated/v11/utility/ai_restore_sample_faded.webp` | `1122 x 1402`，4:5 | AI 修复选择器与修复前预览 |

详细落位、裁切、无障碍和 Agent 分工见 [`docs/agent-handoff.md`](../docs/agent-handoff.md)，本批提示词见 [`prompts-v11.md`](prompts-v11.md)。

## 使用约束

- 图片只用于当前演示项目，不应暗示真实陵园、人物或宠物案例。
- 人像页面必须显示“演示人物”或等价提示，避免用户误认。
- 启动页文字和按钮使用 Compose 绘制，确保可访问、可适配和可本地化。
- 详情页使用中心裁切时应检查公共纪念设施和主要树木没有被截断。
- 发布 APK 前应再次检查 WebP 解码、深色遮罩下对比度和适老字号布局。
- 补充素材已经统一转换为 WebP，建议放入 `drawable-nodpi` 并保持文件名不变。

## 生成提示词

### 启动页

```text
Use case: stylized-concept
Asset type: Android app splash background, portrait
Primary request: a serene Chongqing mountain-city memorial landscape at dawn, combining distant layered hills, subtle city silhouettes, and a quiet commemorative forest
Scene/backdrop: misty Chongqing ridgelines and native trees, calm open sky
Style/medium: premium painterly editorial illustration with natural textures, polished but restrained
Composition/framing: vertical 9:16 composition, clear calm negative space in the center and lower third for app title and button overlays
Lighting/mood: soft early morning light, peaceful, warm, reassuring
Color palette: warm ivory, fresh green, muted teal, small touches of stone gray
Constraints: no people, no graves, no religious symbols, no text, no logos, no watermark, no gradient-only background, dignified and non-morbid
```

### 树葬林地

```text
Use case: photorealistic-natural
Asset type: Android app service card and tree-burial detail hero
Primary request: a dignified ecological tree memorial grove in Chongqing using native trees, with a discreet shared stone memorial wall and a simple public flower-offering platform
Scene/backdrop: gently sloped forest garden, clean walking path, healthy native trees, subtle mountain terrain
Style/medium: photorealistic architectural landscape photography, believable public memorial park
Composition/framing: wide landscape, eye level, clear focal tree and generous crop-safe margins
Lighting/mood: soft overcast morning light, quiet, respectful, welcoming
Color palette: natural greens, warm gray stone, muted earth, small ivory flower accents
Materials/textures: real bark, moss, stone grain, bamboo and linen biodegradable memorial details
Constraints: no human remains, no funeral in progress, no large private tombstones, no religious symbols, no readable text, no logos, no watermark, no gloomy darkness
```

### 花葬花园

```text
Use case: photorealistic-natural
Asset type: Android app service card and flower-burial detail hero
Primary request: a serene ecological memorial flower garden in Chongqing with broad beds of azaleas, white chrysanthemums, and osmanthus, plus a low shared remembrance wall
Scene/backdrop: accessible urban garden with open paths and layered planting, no private grave plots
Style/medium: photorealistic landscape photography, credible civic memorial garden
Composition/framing: wide landscape, clear garden depth, crop-safe center
Lighting/mood: soft spring daylight, peaceful and gently hopeful
Color palette: restrained pink, white, muted purple, natural green, warm ivory stone
Materials/textures: real petals, leaves, fine stone, subtle weathering
Constraints: no people, no human remains, no funeral in progress, no large tombstones, no religious symbols, no readable text, no logos, no watermark, no oversaturated flowers
```

### 草坪葬

```text
Use case: photorealistic-natural
Asset type: Android app service card and lawn-burial detail hero
Primary request: an open and tranquil ecological memorial lawn in a Chongqing urban park, with a low shared memorial wall, a small public flower platform, and only a few flush ground markers
Scene/backdrop: broad maintained grass, distant tree line, gentle hill contours, accessible stone path
Style/medium: photorealistic public landscape photography, believable and understated
Composition/framing: wide landscape with open central lawn and crop-safe margins
Lighting/mood: clear soft morning after rain, quiet, spacious, reassuring
Color palette: grass green, warm ivory, natural gray, balanced sky blue
Materials/textures: wet grass detail, matte stone, natural trees
Constraints: no people, no human remains, no funeral, no upright private tombstones, no religious symbols, no readable text, no logos, no watermark, no dramatic cemetery mood
```

### 宠物纪念树

```text
Use case: photorealistic-natural
Asset type: Android app pet memorial service card and detail hero
Primary request: a gentle pet remembrance tree garden in a separate landscaped area, with one healthy young tree, a small blank wooden paw-shaped keepsake tag, and a simple bouquet
Scene/backdrop: peaceful enclosed garden edge with soft grass and native shrubs, clearly intimate rather than a human cemetery
Style/medium: photorealistic lifestyle landscape photography, respectful and warm
Composition/framing: wide landscape, focal tree slightly off center, generous crop-safe space
Lighting/mood: soft late-afternoon natural light, tender, calm, comforting
Color palette: natural green, warm wood, ivory flowers, muted teal accents
Materials/textures: real bark, wood grain, grass, linen ribbon
Constraints: no animals, no people, no urns, no human graves, no religious symbols, no readable text, no logos, no watermark, no excessive sadness, no cartoon style
```

### 纪念馆人物头像

```text
Use case: photorealistic-natural
Asset type: fictional profile portrait for an Android memorial-space demo
Primary request: a warm respectful portrait of a fictional elderly Chinese man from Chongqing, around age 72, with a gentle natural expression
Scene/backdrop: quiet home balcony with soft greenery and distant blurred hills
Subject: one fictional elderly man, chest-up, ordinary neat clothing, natural wrinkles and skin texture
Style/medium: photorealistic family archival portrait, candid rather than studio glamour
Composition/framing: vertical 4:5 portrait, face centered with comfortable margin
Lighting/mood: soft window daylight, affectionate, calm, dignified
Color palette: natural skin tones, muted green, warm gray, ivory
Constraints: entirely fictional person, no resemblance to a known person, no text, no logos, no watermark, no funeral clothing, no excessive retouching, no melodrama
```

### 纪念馆宠物头像

```text
Use case: photorealistic-natural
Asset type: fictional pet profile portrait for an Android memorial-space demo
Primary request: a warm candid portrait of a fictional small golden mixed-breed dog with a gentle expression
Scene/backdrop: home garden with soft green foliage and a hint of Chongqing hillside atmosphere
Subject: one fictional dog, chest-up, natural fur detail, simple teal fabric collar with no tag
Style/medium: photorealistic family photo, candid and authentic
Composition/framing: vertical 4:5 portrait, face centered with comfortable margin
Lighting/mood: soft natural daylight, affectionate, calm, dignified
Color palette: warm golden fur, muted green, teal accent, ivory highlights
Constraints: fictional animal, no text, no logos, no watermark, no costume, no funeral objects, no exaggerated sadness, no cartoon style
```
