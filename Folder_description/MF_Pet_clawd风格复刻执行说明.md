# MF_Pet clawd 风格复刻执行说明

## 1. 当前决策

MF_Pet 的目标不是简单放一个 GIF，而是学习 `clawd-on-desk` 的桌宠设计方式：主题驱动、状态机驱动、会话事件驱动、mini 模式、点击/拖拽反应、工作状态分层、聊天气泡。

当前阶段先套用本地 `clawd-on-desk/assets/gif` 里的 clawd GIF 作为开发占位主题，用来验证交互与架构。正式交付时必须切回 MF 自有宠物素材。

边界：

- 保留 `MF_Pet/web-assets/pet` 里的 MF 自有 GIF。
- 新增 `clawd-dev` 开发主题，直接引用本地 `clawd-on-desk/assets/gif`。
- 不把 clawd 素材复制进 MF 正式资源目录。
- 不复制 clawd-on-desk 源码，MF_Pet 使用自己的浏览器 runtime 实现。
- `clawd-dev` 仅用于本地开发和复刻验证。

## 2. 重新蒸馏出的 clawd-on-desk 核心设计

`clawd-on-desk` 值得复刻的不是 Electron 外壳本身，而是以下分层：

```text
theme.json
  定义 states / miniMode / reactions / workingTiers / idleAnimations

theme loader + schema
  校验主题、合并默认值、归一化素材引用

state priority
  多个 session 同时存在时，按 error > notification > sweeping > attention > working > thinking > idle 决定显示状态

session events
  把 UserPromptSubmit / PreToolUse / Stop / PermissionRequest 等 agent 生命周期事件映射成宠物状态

visual resolver
  把 state 转成具体素材；working、juggling 会按活跃 session 数选择分层动画

renderer / mini
  DOM 或窗口层负责展示、拖拽、点击反应、mini edge snap、hover peek、聊天/权限气泡
```

MF_Pet 当前采用 Web SDK 形态复刻这些层，方便以后被 `MF_Website`、商城、AI 服务同时复用。

## 3. 当前 MF_Pet 架构

```text
MF_Pet/src/core/petThemeValidator.js   主题校验
MF_Pet/src/core/petThemeJson.js        JSON 主题解析、加载、导出
MF_Pet/src/core/petThemeHealth.js      主题素材健康检查
MF_Pet/src/core/petThemeRegistry.js    主题注册、列表、按 URL 加载
MF_Pet/src/core/petSessionModel.js     session badge、最近事件、过期清理规则
MF_Pet/src/core/petEventBridge.js      官网/商城/AI 业务事件适配器
MF_Pet/src/core/petThemeLoader.js      主题归一化、默认事件映射、状态优先级
MF_Pet/src/core/petVisualResolver.js   state/session -> asset
MF_Pet/src/core/petStateMachine.js     minDisplay / autoReturn 状态机
MF_Pet/src/core/petRuntime.js          可复用 runtime，管理可见性、session、mini、chat、reaction
MF_Pet/src/core/petStorage.js          localStorage 偏好持久化
MF_Pet/src/themes/clawdDevTheme.js     clawd 本地开发占位主题
MF_Pet/src/themes/mfSproutTheme.js     MF 正式主题
MF_Pet/web-assets/pet/theme.json       MF 正式主题 JSON 包
MF_Pet/src/forestPet.js                Web DOM 组件，对官网/商城暴露 API
MF_Pet/src/forestPet.css               MF 风格悬浮宠物与聊天气泡样式
MF_Pet/demo/clawd-style-demo.html      clawd 风格复刻验证 demo
```

## 4. clawd 设计到 MF_Pet 的对应关系

| clawd-on-desk | MF_Pet 当前复刻 |
|---|---|
| `themes/*/theme.json` | `mfSproutTheme.js` / `clawdDevTheme.js` |
| `theme-schema.js` | `petThemeValidator.js` |
| `theme-loader.js` JSON 入口 | `petThemeJson.js` |
| `theme-loader.js` | `petThemeLoader.js` |
| theme runtime / settings | `petThemeRegistry.js` + runtime `muted` / `lowPower` |
| doctor theme health | `petThemeHealth.js` |
| session badge/stale cleanup | `petSessionModel.js` |
| context usage display | session `contextUsage` + HUD 百分比芯片 |
| quota summary | session `quotaUsage` + Dashboard 额度条 |
| agent adapters | `petEventBridge.js` |
| elapsed session meta | Dashboard 使用 `createdAt` / `updatedAt` / event `at` 显示任务更新时间和事件时间 |
| session snapshot | runtime snapshot 输出 `sessionGroups` / `orderedSessionIds` / `hudTotalNonIdle` / `lastSessionId`，session entry 带 `lastEvent` / `requiresCompletionAck` |
| session alias | `renameSession()` 按 `host|agent|sessionId` 持久化 display alias，7 天 TTL |
| focus handoff | `focusSession()` 记录 `focusHandoff`，接入方通过 `onSessionFocus` / `completeSessionFocus()` 回写结果 |
| `state.js` | `petStateMachine.js` + `petRuntime.js` |
| `state-priority.js` | `theme.statePriority` + runtime session resolution |
| `state-session-events.js` | `runtime.emitAgentEvent()` / `startSession()` |
| `state-visual-resolver.js` | `petVisualResolver.js` |
| `renderer.js` | `forestPet.js` |
| `mini.js` | `enterMini()` / `exitMini()` / edge snap / hover peek |
| context menu | `forestPet.js` MF 风格右键菜单 |
| reactions | `theme.reactions` + `runtime.playReaction()` |
| bubble renderer | MF 风格聊天气泡 + streaming message |
| bubble policy | `permissionBubblesEnabled` / `hideBubbles` / `permissionBubbleAutoCloseSeconds` / `notificationBubbleAutoCloseSeconds`，隐藏气泡不自动代替用户决策 |
| session HUD | MF 风格活跃任务小面板 |
| session HUD elapsed | HUD 行显示 `updatedAt` 相对时间 |
| session HUD focus unavailable | `canFocus === false` 的 HUD 行显示不可聚焦标记 |
| session HUD pinned/reveal | `sessionHudPinned` / `revealSessionHud()` / hot-zone 自动隐藏 |
| session HUD folded rows | HUD 前 3 个 session 展开，剩余任务显示为 “other active” 摘要行 |
| session HUD completion unread | 完成任务在 HUD 显示未读提示，点击后调用 `acknowledgeSession()` |
| sessions dashboard | Web 版任务面板，按 host/agent 分组，支持 badge 筛选、事件、上下文用量、定位/已读/隐藏/结束操作 |
| agent identity | Dashboard session 卡片支持 `agentIcon` 图标、`agentColor` 配色和 fallback 缩写 |
| session metadata snapshot | session `displayTitle` / `agentId` / `agentLabel` / `agentIcon` / `agentColor` / `provider` / `model` / `cwd` / `host` / `platform` / `editor` / `hookSource` |

Electron 多窗口、透明 hit window、多屏 seam clip、系统托盘、权限确认窗口暂不复刻到 Web V1；这些属于桌面端外壳能力，后续如果 MF_Pet 做 Electron/Tauri 桌面版再补。

runtime snapshot 已预留 `shellMode` 和 `shellCapabilities`，用于后续桌面壳声明 `transparentWindow / alwaysOnTop / tray / nativeFocus / multiScreen` 等能力；当前 Web demo 默认 `shellMode=web` 且能力均为 `false`。
桌面壳接入时可调用 `setShellCapabilities()` 更新这些能力。

## 5. 当前事件映射

MF_Pet 已把 clawd 风格 agent 事件映射到状态：

```text
SessionStart        -> idle
SessionEnd          -> idle
UserPromptSubmit    -> thinking
PreToolUse          -> working
PostToolUse         -> working
PostToolUseFailure  -> error
Stop                -> attention
StopFailure         -> error
SubagentStart       -> juggling
SubagentStop        -> working
PreCompact          -> sweeping
PostCompact         -> attention
Notification        -> notification
PermissionRequest   -> notification
WorktreeCreate      -> carrying
```

对外 API：

```js
pet.emitAgentEvent("UserPromptSubmit", { sessionId: "main" });
pet.startSession("main", "working", {
  title: "官网 AI 问答",
  agentId: "mf-website",
  agentLabel: "MF_Website",
  agentColor: "#7ee1a6",
  provider: "MF AI",
  model: "planner",
  cwd: "F:/20260518-xiangmu/MF_Project/MF_Website",
  host: "local",
  platform: "windows",
  editor: "MF_Website",
  hookSource: "web-sdk",
  canFocus: true,
  focusTarget: { type: "website-question", id: "main" }
});
pet.updateSession("main", { state: "working", displayHint: "clawd-typing.gif" });
pet.updateSession("main", { contextUsage: { used: 144000, limit: 200000, source: "MF_AgentService" } });
pet.updateSession("main", { quotaUsage: { label: "MF AI 日额度", usedPercent: 72, remaining: 2800 } });
pet.setPermissionBubbleAutoCloseSeconds(0);
pet.setNotificationBubbleAutoCloseSeconds(6);
pet.renameSession("main", "官网 AI 问答");
pet.openDashboard();
pet.setTheme(mfTheme, { assetsBaseUrl: "/assets/pet" });
pet.endSession("main");
```

多个 session 同时工作时，`workingTiers` 会按活跃 session 数自动切换动画。

业务事件桥 API：

```js
import { createPetEventBridge } from "@mf/pet/event-bridge";

const bridge = createPetEventBridge(pet.runtime);
bridge.websiteAsk({ sessionId: "website-q", message: "用户正在询问苗木养护" });
bridge.mallRecommend({ sessionId: "mall-r" });
bridge.aiPermission({
  sessionId: "agent-perm",
  message: "AI 需要确认工具调用",
  toolName: "queryInventory",
  toolInputDescription: "查询商城库存和价格区间。",
  permissionSuggestions: [
    { id: "allow-inventory", label: "允许库存查询", behavior: "approved", type: "addRules" }
  ]
});
```

主题 JSON API：

```js
import { loadPetThemeFromUrl, parsePetThemeJson, exportPetThemeJson } from "@mf/pet/theme-json";
import { checkPetThemeAssets } from "@mf/pet/theme-health";
import { createPetThemeRegistry } from "@mf/pet/theme-registry";

const theme = await loadPetThemeFromUrl("/assets/pet/theme.json", {
  assetBaseUrl: "/assets/pet"
});
```

MF 正式主题现在已有 JSON 包：

```text
MF_Pet/web-assets/pet/theme.json
```

## 6. clawd-dev 主题素材映射

```text
idle         -> clawd-idle.gif
appearing    -> clawd-mini-enter.gif
expanded     -> clawd-happy.gif
thinking     -> clawd-thinking.gif
working      -> clawd-typing.gif
juggling     -> clawd-juggling.gif
carrying     -> clawd-carrying.gif
sweeping     -> clawd-sweeping.gif
notification -> clawd-notification.gif
attention    -> clawd-happy.gif
success      -> clawd-happy.gif
unclear      -> clawd-thinking.gif
error        -> clawd-error.gif
resting      -> clawd-sleeping.gif
```

工作分层：

```text
1 active session  -> clawd-typing.gif
2 active sessions -> clawd-headphones-groove.gif
3+ sessions       -> clawd-building.gif
```

子任务分层：

```text
1 juggling session  -> clawd-headphones-groove.gif
2+ juggling sessions -> clawd-juggling.gif
```

mini 映射：

```text
mini-idle    -> clawd-mini-idle.gif
mini-peek    -> clawd-mini-peek.gif
mini-alert   -> clawd-mini-alert.gif
mini-happy   -> clawd-mini-happy.gif
mini-working -> clawd-mini-crabwalk.gif
mini-sleep   -> clawd-sleeping.gif
```

## 7. 当前支持的交互

- 点击宠物展开 / 收起面板。
- 右键打开 MF 风格上下文菜单，可展开、聊天、进入/退出 mini、睡眠勿扰、低功耗、静音、重置位置、隐藏。
- 右键菜单可单独切换权限气泡，也可用 `hideBubbles` 一键隐藏/恢复普通通知与权限气泡。
- 拖拽宠物位置。
- 拖拽时播放 drag reaction。
- 拖到右边缘自动进入 mini 模式。
- 双击左右区域播放不同 reaction。
- 四连击播放 double reaction。
- mini 模式 hover 播放 peek。
- mini 支持左/右边缘，进入/退出有轻量过渡动画。
- 无操作一段时间进入 resting。
- 支持 `setDoNotDisturb()` / `sleep()` / `wake()`，睡眠勿扰时使用 `resting` 或 `mini-sleep` 动画，普通通知静默，session 状态继续记录。
- idle 状态定时播放 idleAnimations。
- PermissionRequest、Notification、Stop、error 类事件会显示 MF 风格通知气泡。
- PermissionRequest 会生成 Web 版权限确认卡片，支持工具名、自动输入摘要、suggestions、批准 / 拒绝，并回写 session 状态和 `permissionDecision`。
- 权限气泡会把 MCP 风格工具名格式化为 `server · tool`，并从 `toolInput` 自动提取 command、file path、query、description 等摘要。
- 支持 `setPermissionBubblesEnabled()` / `setHideBubbles()` / `dismissPermission()`；关闭或手动隐藏权限气泡时，权限请求仍进入 runtime/session，但不会自动批准或拒绝。
- 支持 `setPermissionBubbleAutoCloseSeconds()`；默认 `0` 表示权限气泡不自动消失，设置大于 0 时只自动隐藏气泡，不替用户做决策，复刻 clawd 的 defensive auto-dismiss 策略。
- 支持 `setNotificationBubbleAutoCloseSeconds()`；默认 `6` 秒，设置为 `0` 时普通通知不弹出，`force` 通知仍可显示；`hideBubbles` 会同时隐藏普通通知和权限气泡。
- 支持 `setBubbleCategoryEnabled("permission" | "notification", enabled)`，对齐 clawd 的按气泡类别开关模型。
- snapshot 暴露 `allBubblesHidden`，DOM 同步 `data-all-bubbles-hidden`，便于接入方判断全局气泡静默状态。
- snapshot 暴露 `bubblePolicy.permission / bubblePolicy.notification / bubblePolicy.update`，包含 `enabled` 和 `autoCloseMs`；`update` 先作为 Web 版占位。
- 支持 `getBubblePolicy(kind?)`，接入方可读取当前分类气泡策略。
- 活跃 session 会显示 MF 风格 session HUD。
- session HUD 行会显示 `updatedAt` 相对时间，复刻 clawd 的 elapsed 信息区。
- session HUD 对 `canFocus === false` 的任务显示 focus unavailable 标记，避免把远程/不可定位任务误认为可跳转。
- session HUD 支持 `revealSessionHud()` / `hideSessionHud()` / `setSessionHudPinned()`；Web 版用宠物点击、右键菜单、pin 按钮和 hover 热区复刻 clawd 的 reveal/pin/auto-hide 行为。
- session HUD 超过 3 个活跃任务时显示 folded summary row，点击摘要行进入 Dashboard 查看完整列表。
- session HUD 对 `done` 且未 acknowledge 的任务显示 completion unread 标记，点击任务后回写 `acknowledgeSession()`。
- 点击可聚焦 session HUD 行会基于 runtime 当前快照触发 `focusSession()` / `onSessionFocus`；不可聚焦行保留最近事件详情，便于观察 UserPromptSubmit、PreToolUse、Stop 等生命周期。
- 支持 `openDashboard()` / `closeDashboard()` / `toggleDashboard()`，以 Web 面板展示所有非隐藏 session、最近事件、任务更新时间、上下文用量和定位/已读/隐藏/结束按钮。
- Dashboard 支持按 `all / running / waiting / done` 筛选，并按 `host / agent` 维度分组；HUD 仍只显示非 headless 的桌面可见任务。
- runtime snapshot 已下沉 session 分组、排序、HUD 摘要和 last session 信息，官网/商城可直接消费，不必在各自 UI 重复推导。
- session entry 会携带 `lastEvent` 和 `requiresCompletionAck`，Dashboard / HUD 都基于同一份完成未读语义展示已读提示。
- 支持 `focusSession()` / `hideSession()`；Web 版只发出 session focus 动作并隐藏 MF_Pet 视图，不做系统终端聚焦，接入方可用 `onSessionFocus` 跳转到官网问答、商城推荐或 AI 面板。
- `focusSession()` 会在 session 上记录 `focusHandoff.status/source/target/requestedAt`；`onSessionFocus` 返回 `{ status, reason, target }` 或调用 `completeSessionFocus()` 后，Dashboard 会展示最近一次定位结果。
- 支持 `acknowledgeSession()`；用于把已完成、等待确认、阻塞等需要注意的 session 标记为已读，badge 回落为 idle。
- 支持 `renameSession()`；Dashboard 标题可内联编辑，显示优先级为 `displayTitle -> title/sessionTitle -> id`，接入方可用 `onSessionRename` 同步到自己的业务侧。
- `renameSession()` 会写入轻量 session alias，key 为 `host|agent|sessionId`，刷新后同一任务恢复别名；清空标题会删除 alias，过期 alias 7 天后清理。
- session 支持 `agentId / agentLabel / agentIcon / agentColor / provider / model / cwd / host / platform / editor / hookSource / sourcePid / turnId` 元数据，Dashboard 会展示 agent 图标或缩写、来源、模型、目录、主机、系统、编辑器和 hook 信息。
- session 会自动派生 `running / waiting / done / blocked / stale / idle` badge。
- session 可携带 `contextUsage`，HUD 显示紧凑百分比芯片，详情面板显示 `已用 / 上限`；非法输入会被丢弃，缺失输入不清空已有遥测。
- session 可携带 `quotaUsage`，Dashboard 显示额度名称、使用百分比、剩余额度和重置倒计时；同样采用合法输入覆盖、缺失输入保持已有遥测。
- runtime 会按规则清理过期 session，避免长驻页面堆积旧任务。
- `createPetEventBridge()` 让官网、商城、AI 服务用业务语义触发宠物，不必直接依赖底层 hook 名。
- runtime 支持 `setMuted()`、`setLowPower()`、`setDoNotDisturb()`，低功耗会暂停 idle 小动作并清理 reaction。
- `checkPetThemeAssets()` 可检查主题引用的素材是否可访问。
- `createPetThemeRegistry()` 可注册内置主题、从 URL 加载主题并按 id 列出主题能力。
- runtime 支持 `setTheme()`，可在 clawd 开发占位主题和 MF 正式主题之间不中断切换。
- localStorage 会持久化 muted、lowPower、doNotDisturb、permissionBubblesEnabled、mini、miniEdge、position 等偏好。
- 聊天气泡支持用户消息、助手消息和模拟流式返回。
- 官网/商城/AI 可以通过 session/event API 驱动宠物状态。

## 8. Demo 使用

在 `MF_Pet` 目录启动静态服务：

```bash
python -m http.server 5179
```

打开：

```text
http://localhost:5179/demo/clawd-style-demo.html
```

demo 默认使用 `clawdDevTheme`，按钮可模拟 clawd-on-desk 的 agent 事件、多会话工作、上下文用量、任务面板、权限确认、权限气泡策略、通知气泡、session HUD、右键菜单、左右 mini 模式、睡眠勿扰、JSON 主题解析、MF 主题 JSON 加载、素材健康检查、低功耗/静音和聊天流式返回。

## 9. 后续复刻路线

Phase 1 继续补齐 Web 复刻体验：

- 更接近 clawd 的 mini 进入/退出动画，而不仅是位置切换。
- 移动端底部抽屉形态。
- 更完整的桌面端权限确认交互：Web 端已做工具信息、建议决策和批准/拒绝，桌面端再做独立确认窗口与阻塞式响应。
- session HUD 继续扩展成任务列表，用于展示当前官网/商城/AI 的活跃任务耗时、来源和结果。
- 为 MF 正式主题继续补 mini/reaction/idleAnimations 专用素材，减少对 fallback 的依赖。

Phase 2 再换回 MF 自有模组：

- 为 MF 精灵补齐 mini/reaction/idleAnimations 专用素材。
- 用同一套 theme schema 描述 MF 主题。
- `MF_Website`、商城、AI 服务统一接入 `MF_Pet` API。

