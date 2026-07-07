# MF_Pet 开发任务文档

## 1. 任务目标

为 `MF_Website` 开发官网常驻互动角色 `苗丰精灵`。

`苗丰精灵` 是苗丰官网的品牌化 AI 入口，不是独立宠物养成游戏。V1 目标是让用户在官网中能看到、唤出、收起并使用这个精灵入口，从而连接商城、百科、商家入驻和 AI 咨询。

## 2. 项目背景

当前产品矩阵：

- `MF_Website`：苗丰生态品牌官网。
- `MF_EP/projects/mf/mf-frontend-client`：消费者商城、百科、文章、订单等 C 端业务。
- `MF_EP/projects/mf/mf-frontend`：平台运营后台。
- `MF_EP/projects/mf/mf-frontend-merchant`：商家经营后台。
- `MF_EP/projects/mf/mf-ai`：AI 咨询与内容生成能力。
- `MF_Pet`：苗丰精灵素材与后续组件开发准备区。

官网导航已确定方向：

```text
[logo] 苗丰 MF        生态故事   树木百科      [小树图标 苗丰精灵]      苗木商城   商家入驻        进入商城   EN
```

移动端默认导航：

```text
[logo] 苗丰 MF                    精灵   商城   EN   菜单
```

详细设计方针请先阅读：

```text
F:\20260518-xiangmu\MF_Project\Folder_description\MF_Website_设计方针.md
```

## 3. 素材位置

宠物设计图位于：

```text
F:\20260518-xiangmu\MF_Project\MF_Pet\Pet_Design_Drawing\Status
```

当前素材：

```text
doubt.png
idle.png
move.png
rest.png
sad.png
status.png
watering.png
work.png
```

建议状态含义：

| 文件 | 建议用途 |
|---|---|
| `idle.png` | 默认待机状态 |
| `move.png` | 出现、轻微移动或引导用户时 |
| `rest.png` | 收起、长时间无操作或安静陪伴状态 |
| `work.png` | 正在处理问题、进入 AI 咨询时 |
| `watering.png` | 积极反馈、推荐种植/养护内容时 |
| `doubt.png` | 用户输入不明确、需要追问时 |
| `sad.png` | 错误、加载失败或无法回答时 |
| `status.png` | 状态面板、欢迎面板或默认卡片头像 |

接手 agent 需要先确认这些图片尺寸、透明背景、视觉重心和文件格式是否适合直接用于 Web。

## 4. 实现范围 V1

### 4.1 必须实现

1. 在 `MF_Website` 中新增 `苗丰精灵` 常驻组件。
2. 点击导航中心的 `[小树图标 苗丰精灵]` 后，精灵出现在官网页面中。
3. 精灵出现后常驻在页面右下角或底部偏右位置，不随滚动 section 消失。
4. 精灵支持展开和收起。
5. 展开面板提供快捷入口：
   - 进入商城
   - 查看树木百科
   - 问苗丰精灵
   - 商家入驻
6. `问苗丰精灵` 进入 AI 咨询入口。V1 可以先跳转到主平台 AI 页面，或打开一个预留对话面板。
7. 支持中英文文案，复用 `MF_Website` 当前 `src/shared/i18n.js` 的语言机制。
8. 支持桌面端和移动端。
9. 通过 localStorage 保存基础状态：
   - 是否已唤出
   - 是否展开
   - 当前状态图
10. 不破坏 `MF_Website` 现有滚动动画、preloader、location hero、三卡翻转区和 client panels。

### 4.2 暂不实现

V1 不做以下内容：

- 宠物养成系统。
- 经验值、等级、喂养、换装。
- 积分任务。
- 复杂聊天记忆。
- 跨多个独立前端项目的无刷新常驻。
- 真实 AI 流式对话。
- 后端数据库表。
- 用户账号绑定。

如果需要 AI 咨询，V1 先做入口级集成，不要引入大范围后端改造。

## 5. 推荐集成位置

目标官网项目：

```text
F:\20260518-xiangmu\MF_Project\MF_Website\mf_Website
```

当前官网技术栈：

- Vite
- 原生 HTML/CSS/JS
- GSAP ScrollTrigger
- Lenis

建议新增文件：

```text
src/sections/forestPet.js
src/styles/forestPet.css
```

并在以下文件注册：

```text
src/main.js
src/styles.css
index.html
src/shared/i18n.js
```

如果需要复制素材，建议复制到：

```text
public/assets/pet/
```

例如：

```text
public/assets/pet/idle.png
public/assets/pet/move.png
public/assets/pet/rest.png
public/assets/pet/work.png
public/assets/pet/watering.png
public/assets/pet/doubt.png
public/assets/pet/sad.png
public/assets/pet/status.png
```

不要直接从 `MF_Pet/Pet_Design_Drawing` 做运行时引用，官网构建产物应依赖 `public/assets` 内的静态资源。

## 6. 交互设计

### 6.1 桌面端

默认状态：

- 页面加载完成后，不强制弹出大面板。
- 可以显示一个小的精灵悬浮入口，或仅在用户点击导航 `苗丰精灵` 后出现。
- 精灵建议位置：右下角，距离边缘 `24px - 32px`。
- 精灵主体不要遮挡主要滚动视觉内容。

点击导航中的 `苗丰精灵`：

1. 若精灵未出现，则播放轻微出现动画。
2. 状态图从 `move.png` 切换到 `idle.png`。
3. 打开或聚焦精灵面板。

点击精灵本体：

- 展开快捷面板。
- 再次点击或点击关闭按钮可收起。

展开面板建议文案：

```text
你好，我是苗丰精灵。
想先做什么？

进入商城
查看树木百科
问苗丰精灵
商家入驻
```

### 6.2 移动端

默认顶部导航中应露出 `精灵`。

点击 `精灵`：

- 精灵以底部抽屉或右下角悬浮卡片出现。
- 面板高度不要超过视口的 `60%`。
- 快捷入口按钮适合触摸，单个按钮高度不小于 `44px`。

移动端不要让宠物一直大面积遮挡画面。可用小头像 + 底部抽屉的方式处理。

## 7. 状态切换建议

最小状态机：

```text
hidden -> appearing -> idle -> expanded -> working -> idle
```

建议映射：

- `hidden`：不显示。
- `appearing`：显示 `move.png`。
- `idle`：显示 `idle.png`。
- `expanded`：显示 `status.png` 或 `idle.png`。
- `working`：显示 `work.png`。
- `success`：显示 `watering.png`。
- `unclear`：显示 `doubt.png`。
- `error`：显示 `sad.png`。
- `resting`：显示 `rest.png`。

V1 可以不用做复杂动画，至少保证状态切换自然、不闪烁、不影响滚动性能。

## 8. 导航联动要求

接手 agent 需要同步调整官网导航：

桌面端：

```text
[logo] 苗丰 MF        生态故事   树木百科      [小树图标 苗丰精灵]      苗木商城   商家入驻        进入商城   EN
```

要求：

- `苗丰精灵` 位于视觉中心。
- 使用小树/幼苗图标，不使用星星符号。
- 点击后唤出精灵组件，不直接跳转。
- `进入商城` 仍作为右侧主 CTA。
- `EN` 保持语言切换功能。

移动端：

```text
[logo] 苗丰 MF                    精灵   商城   EN   菜单
```

要求：

- `精灵` 和 `商城` 默认可见。
- 完整菜单中包含：生态故事、树木百科、苗丰精灵、苗木商城、商家入驻、进入商城。

## 9. 链接目标

如果当前本地没有统一部署地址，V1 可以先使用常量占位，并集中维护。

建议在 `forestPet.js` 或单独配置中定义：

```js
const MF_LINKS = {
  mall: "#",
  encyclopedia: "#",
  merchant: "#",
  ai: "#",
};
```

后续接入真实地址时只改一处。

不要把链接散落写死在多个 DOM 事件中。

## 10. 视觉要求

- 精灵视觉应轻量、友好、自然，不要做成强商业客服弹窗。
- 悬浮层应使用深绿色半透明或浅绿色柔和风格，和官网森林背景一致。
- 面板不能像后台管理卡片，也不能像普通 SaaS 客服插件。
- 动画要克制，避免干扰现有 GSAP 滚动叙事。
- 图片应限制最大宽高，避免大图撑爆布局。
- 所有按钮文字必须在桌面和移动端不溢出。

## 11. 技术要求

- 使用现有 Vite 原生 JS/CSS 项目结构。
- 不引入 Vue/React。
- 不引入大型 UI 框架。
- 不重构现有 `locationScroll.js`、`splitCards.js`、`clientPanels.js`。
- 新增逻辑尽量放在 `forestPet.js`。
- 新增样式尽量放在 `forestPet.css`。
- 必须避免多个组件抢同一滚动 transform。
- 注意移动端真实视口高度和现有 `viewport.js` 机制。
- 图片资源要使用 Web 可访问路径，例如 `/assets/pet/idle.png`。

## 12. 验证命令

在官网目录执行：

```bash
cd F:\20260518-xiangmu\MF_Project\MF_Website\mf_Website
npm run build
```

如需要本地预览：

```bash
npm run dev
```

浏览器手动验证：

- 桌面端第一屏导航正常显示。
- 点击导航 `苗丰精灵` 后宠物出现。
- 精灵出现后滚动页面不会消失。
- 展开/收起面板正常。
- 快捷入口按钮可点击。
- 切换中英文后，苗丰精灵相关文案同步变化。
- 移动端默认能看到 `精灵`、`商城`、`EN`、菜单。
- 移动端点击 `精灵` 后面板不遮挡整屏。
- 原有 preloader、location hero、三卡翻转、client panels 仍正常。

## 13. 最终交付说明

完成后请输出：

1. 改动文件列表。
2. 素材复制位置。
3. 苗丰精灵状态映射说明。
4. 桌面端和移动端交互说明。
5. 链接占位或真实链接配置位置。
6. `npm run build` 结果。
7. 仍未实现的限制和后续建议。

