# MF_Pet

`MF_Pet` 是苗丰精灵的素材、主题和前端宠物 runtime 项目。当前阶段按 `clawd-on-desk` 的设计思路复刻：状态机驱动动画、主题配置驱动资源、交互层独立、宿主项目只发送语义事件。

## 当前结构

```text
Pet_Design_Drawing/Status/  原始状态设计稿
web-assets/pet/             MF Web 可用 GIF 与透明 PNG 帧
src/core/                   runtime、状态机、主题归一化、视觉解析、storage
src/themes/                 MF 主题与 clawd 开发占位主题
src/forestPet.js            Web DOM 组件
src/forestPet.css           浮层、聊天气泡、mini/drag 样式
tools/extract_status_gif.py 设计稿裁帧与 GIF 生成工具
demo/clawd-style-demo.html  clawd 风格复刻 demo
```

## 主题

### MF 正式主题

```js
import { mfSproutTheme } from "./src/forestPet.js";
```

资源来自：

```text
web-assets/pet/
```

### clawd 开发占位主题

```js
import { clawdDevTheme } from "./src/forestPet.js";
```

资源引用：

```text
clawd-on-desk/assets/gif/
```

注意：`clawdDevTheme` 只用于本地开发原型，不可作为 MF 正式发布素材。

## Demo

在 `MF_Pet` 目录启动静态服务：

```bash
python -m http.server 5179
```

打开：

```text
http://localhost:5179/demo/clawd-style-demo.html
```

## API

```js
import { initForestPet, clawdDevTheme } from "./src/forestPet.js";
import "./src/forestPet.css";

const pet = initForestPet({
  theme: clawdDevTheme,
  assetsBaseUrl: "../clawd-on-desk/assets/gif",
  initiallyVisible: true,
  initiallyExpanded: true,
  onChatSubmit({ text, api }) {
    api.streamAssistantMessage(`收到：${text}`);
    return true;
  },
});

pet.emit("thinking");
pet.emit("ai-working");
pet.emit("ai-success");
pet.openChat();
```

常用方法：

- `show({ expand })`
- `hide()`
- `expand()`
- `collapse()`
- `toggle()`
- `emit(eventName, payload)`
- `setState(state, payload)`
- `setLanguage(lang)`
- `openChat()`
- `closeChat()`
- `streamAssistantMessage(text, options)`
- `enterMini()`
- `exitMini()`
- `getSnapshot()`
- `destroy()`

## 当前交互

- 点击展开/收起。
- 右键进入/退出 mini 模式。
- 拖拽位置。
- 拖拽反应动画。
- 拖到右边缘进入 mini 模式。
- 双击和四连击 reaction。
- mini hover peek。
- MF 风格聊天气泡。
- 聊天输入框。
- 模拟流式返回。
- 状态最短展示与自动回退。
- 长时间无操作进入休息状态。

## MF 状态资源

| 状态 | 资源 |
|---|---|
| `idle` | `idle/idle.gif` |
| `appearing` | `move/move.gif` |
| `working` | `work/work.gif` |
| `success` | `watering/watering.gif` |
| `unclear` | `doubt/doubt.gif` |
| `error` | `sad/sad.gif` |
| `resting` | `rest/rest.gif` |

每个状态包含 8 帧透明 PNG，统一 `288x288`，GIF 单帧 `200ms`。

## 素材生成

```bash
python tools/extract_status_gif.py rest
```

可提取状态见 `tools/extract_status_gif.py` 的 `FRAME_REGIONS`。

## 接入约定

正式项目运行时不要直接引用 `Pet_Design_Drawing/Status` 原始设计稿，也不要发布 `clawdDevTheme` 依赖的 clawd 素材。接入项目应复制或发布 `web-assets/pet` 到自身静态目录，例如：

```text
MF_Website/mf_Website/public/assets/pet/
```

## Desktop Shell

MF_Pet 已提供轻量 Electron 桌面壳：

```bash
npm run desktop
```

桌面壳当前对齐 clawd-on-desk 的基础形态：
- 透明无边框窗口
- always-on-top
- 托盘菜单 Show / Hide / Quit
- 默认套用 `clawd-on-desk/assets/gif` 的 clawd 动画
- 通过 `shellMode=desktop` 和 `shellCapabilities` 向 runtime 声明桌面能力
- `Alt + 拖拽宠物` 可移动原生透明窗口

说明：如果 `MF_Pet/node_modules/electron` 不存在，启动脚本会复用 `clawd-on-desk/node_modules/electron`。
