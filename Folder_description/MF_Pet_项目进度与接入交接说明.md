# MF_Pet 项目进度与接入交接说明

## 1. 文档目的

本文用于给项目负责人快速了解 `MF_Pet` 当前完成进度、运行方式、项目边界，以及后续如何把它接入 `MF_Website`、商城或其他 MF 项目。

当前开发策略是：

- `MF_Pet` 负责宠物 runtime、主题、动画资源、桌面壳和通用交互能力。
- `MF_Website`、商城等业务项目负责在运行时加载 `MF_Pet`，并通过 API 发送业务事件。
- 第一阶段先套用 `clawd-on-desk` 的 clawd 动画和 GIF 做桌宠复刻验证。
- 后续正式发布时，再替换为 MF 自己的宠物模组和素材。

## 2. 当前完成进度

整体完成度约 `80%`。

桌面壳方向完成度约 `70%`，已经有第一版可运行 Electron 桌面壳。

已完成内容：

- 通用 Web 宠物组件 `initForestPet()`。
- 状态机 runtime，可处理 idle、working、thinking、success、error、resting、mini 等状态。
- clawd 风格开发主题 `clawdDevTheme`，当前直接套用 `MF_Pet/clawd-on-desk/assets/gif`。
- MF 正式主题 `mfSproutTheme`，可加载 `web-assets/pet` 下的 MF GIF 资源。
- 任务 HUD、任务面板、通知气泡、权限气泡、聊天气泡。
- 右键菜单，已按 clawd 风格做分组。
- mini 贴边模式和 hover 探头动画。
- 桌面模式标记：`shellMode=desktop`。
- 桌面壳能力声明：`shellCapabilities`。
- Electron 桌面壳第一版：
  - 透明无边框窗口。
  - always-on-top。
  - 跳过任务栏。
  - 托盘菜单 `Show / Hide / Quit`。
  - 默认加载 clawd GIF。
  - 接入现有 MF_Pet runtime。

暂未完成内容：

- 完整复刻 `clawd-on-desk` 的原生窗口管理。
- 多屏幕 workArea 边界夹取。
- Windows 顶层丢失后的 watchdog 恢复。
- 系统级点击穿透和输入路由。
- 独立 Settings 窗口。
- 独立 Dashboard 窗口。
- 完整 agent hook / AI 工具调用链路。
- MF 正式宠物素材替换。
- 统一静态验证、行为验证和自动化测试。

## 3. 项目位置

主项目目录：

```text
F:\20260518-xiangmu\MF_Project\MF_Pet
```

关键目录：

```text
MF_Pet/
  desktop/                  Electron 桌面壳
  src/                      通用 runtime、组件、主题
  src/core/                 状态机、事件桥、主题加载、资源解析
  src/themes/               clawd 开发主题和 MF 正式主题
  web-assets/pet/           MF 宠物 GIF / PNG 资源
  clawd-on-desk/            参考项目和当前开发占位 GIF
  demo/clawd-style-demo.html Web demo
  package.json              npm 脚本和模块导出
```

## 4. 如何运行桌面壳

进入目录：

```bash
cd /d F:\20260518-xiangmu\MF_Project\MF_Pet
```

启动桌面壳：

```bash
npm run desktop
```

说明：

- 当前脚本为 `node desktop/launch.cjs`。
- 如果 `MF_Pet/node_modules/electron` 不存在，会复用：

```text
MF_Pet/clawd-on-desk/node_modules/electron
```

启动后应看到：

- 桌面右下附近出现透明桌宠窗口。
- 系统托盘出现 `MF_Pet Desktop`。
- 托盘菜单支持 `Show Pet`、`Hide Pet`、`Quit`。

桌面壳当前交互：

- 普通点击宠物：展开/收起面板。
- 右键宠物：打开菜单。
- 拖到屏幕边缘：进入 mini 贴边模式。
- hover mini：触发 clawd mini peek。
- `Alt + 拖拽宠物`：移动原生透明窗口。

## 5. 如何运行 Web Demo

进入目录：

```bash
cd /d F:\20260518-xiangmu\MF_Project\MF_Pet
```

启动静态服务：

```bash
python -m http.server 5179
```

打开：

```text
http://127.0.0.1:5179/demo/clawd-style-demo.html
```

Web demo 用于验证：

- clawd GIF 是否能加载。
- runtime 状态切换是否正常。
- 任务 HUD、Dashboard、气泡、聊天面板是否正常。
- mini、右键菜单、桌面模式 CSS 是否正常。

## 6. 如何接入 MF_Website / 商城

推荐接入方式是：业务项目只依赖 `MF_Pet` 的通用 API，不直接复制桌面壳。

示例：

```js
import { initForestPet, mfSproutTheme } from "@mf/pet";
import "@mf/pet/styles";

const pet = initForestPet({
  theme: mfSproutTheme,
  assetsBaseUrl: "/assets/pet",
  initiallyVisible: true,
  initiallyExpanded: false,
});

pet.startSession("website-main", "working", {
  title: "正在处理官网咨询...",
  agentId: "mf-website",
  agentLabel: "MF Website",
});

pet.updateSession("website-main", {
  state: "success",
  title: "官网咨询已完成",
});
```

业务侧建议只做三件事：

1. 引入 `MF_Pet` 组件和样式。
2. 把宠物静态资源发布到业务项目可访问的 public 目录。
3. 在业务流程中调用 `pet.startSession()`、`pet.updateSession()`、`pet.endSession()`、`pet.addNotification()`。

## 7. 推荐业务事件映射

官网：

```text
用户打开 AI 咨询        -> pet.startSession()
AI 正在生成回答         -> state = working / thinking
AI 流式返回             -> pet.openChat().streamAssistantMessage()
回答完成               -> state = success
请求失败               -> state = error
```

商城：

```text
商品推荐生成中          -> state = working
库存/价格工具调用       -> PermissionRequest / notification
推荐完成               -> state = success
推荐失败               -> state = error
```

桌面壳：

```text
后台任务进行中          -> task HUD 展示
多任务并发              -> Dashboard 展示 session group
用户需要注意            -> notification / attention
长时间无操作            -> resting / mini
```

## 8. API 简表

常用方法：

```js
pet.show()
pet.hide()
pet.expand()
pet.collapse()
pet.toggle()
pet.emit(eventName, payload)
pet.emitAgentEvent(eventName, payload)
pet.startSession(id, state, metadata)
pet.updateSession(id, patch)
pet.endSession(id)
pet.openChat()
pet.closeChat()
pet.streamAssistantMessage(text, options)
pet.addNotification(notification)
pet.enterMini({ edge: "left" | "right" })
pet.exitMini()
pet.setTheme(theme, options)
pet.setShellCapabilities(capabilities)
pet.getSnapshot()
pet.destroy()
```

桌面壳能力：

```js
pet.setShellCapabilities({
  shellMode: "desktop",
  transparentWindow: true,
  alwaysOnTop: true,
  tray: true,
  nativeFocus: true,
  multiScreen: false,
});
```

## 9. 负责人需要知道的边界

当前 `MF_Pet` 已经能作为一个可运行原型演示：

- Web 端可以嵌入。
- Electron 桌面壳可以启动。
- clawd 风格动画可以跑起来。
- 任务、气泡、mini、右键菜单都有基础形态。

但当前还不是最终产品级桌宠：

- clawd GIF 只是临时开发占位，不应作为 MF 正式发布素材。
- Electron 壳是第一版轻量实现，不等价于完整 clawd-on-desk 主进程。
- 与官网、商城、AI 后端之间还没有做真实业务联调。
- 语法、行为、静态验证还未统一收口。

## 10. 下一步建议

建议负责人按下面顺序安排后续工作：

1. 先让 `MF_Website` 接入 `MF_Pet` Web runtime。
2. 让官网 AI 咨询流程把真实事件传给 `pet.startSession()` / `pet.updateSession()`。
3. 同步让商城推荐流程接入同一套 API。
4. 再继续完善 Electron 桌面壳：
   - 多屏支持。
   - 原生窗口贴边。
   - 置顶恢复。
   - 设置窗口。
   - 独立 Dashboard。
5. 最后统一替换 MF 正式宠物素材，并做静态验证、行为验证和打包发布。

## 11. 当前验证记录

已做过轻量验证：

```bash
node --check desktop/main.cjs
node --check desktop/preload.cjs
node --check desktop/launch.cjs
```

已确认：

- `npm run desktop` 脚本存在。
- Electron 可从 `clawd-on-desk/node_modules/electron` 复用。
- `clawd-on-desk/assets/icon.png` 存在。
- `clawd-on-desk/assets/gif/clawd-typing.gif` 存在。
- 桌面壳启动后能看到 Electron 进程。
- Web demo 曾验证返回 HTTP 200。

## 12. 重点文件清单

```text
MF_Pet/package.json
MF_Pet/desktop/main.cjs
MF_Pet/desktop/preload.cjs
MF_Pet/desktop/index.html
MF_Pet/desktop/launch.cjs
MF_Pet/src/forestPet.js
MF_Pet/src/forestPet.css
MF_Pet/src/core/petRuntime.js
MF_Pet/src/core/petEventBridge.js
MF_Pet/src/themes/clawdDevTheme.js
MF_Pet/src/themes/mfSproutTheme.js
MF_Pet/web-assets/pet/theme.json
MF_Pet/demo/clawd-style-demo.html
```

## 13. 交接结论

`MF_Pet` 当前已经具备“可演示、可接入、可继续扩展”的基础。

负责人如果要把项目连起来，建议先把 `MF_Pet` 当成一个通用前端/桌面宠物 runtime：

- 官网和商城只负责发业务事件。
- `MF_Pet` 负责展示宠物、任务状态、气泡、mini、桌面壳。
- 正式素材和完整原生桌面能力放到下一阶段继续收口。
