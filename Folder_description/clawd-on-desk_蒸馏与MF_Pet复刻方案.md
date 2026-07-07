# clawd-on-desk 蒸馏与 MF_Pet 复刻方案

## 1. 结论

`clawd-on-desk` 不是一个简单的 GIF 桌宠项目，而是一个完整的 Electron 桌面宠物系统：

- 透明置顶桌面窗口。
- 独立输入窗口处理拖拽、点击、命中区域。
- Agent hook / plugin / 日志轮询驱动状态。
- 多会话状态机与优先级仲裁。
- 主题系统管理动画资源、hitbox、睡眠、迷你模式、点击反应。
- 设置面板、托盘、权限审批气泡、PWA 手机伴侣、远程 SSH 等外围系统。

对 `MF_Pet` 来说，不应照搬整个项目。后续复刻时应该蒸馏其中 4 个核心思想：

1. **状态机驱动动画**，不要在按钮事件里直接散落改 GIF。
2. **主题/资源配置驱动渲染**，让角色素材和代码分离。
3. **宠物交互层独立**，把拖拽、点击、收起、睡眠等行为集中管理。
4. **宿主项目只发事件**，`MF_Website`、商城、后台不各自实现宠物逻辑。

## 2. 许可证与使用边界

`clawd-on-desk` 源码许可证为 `AGPL-3.0-only`，其美术素材和内置主题素材另有版权说明。

因此 MF 项目后续不要直接复制：

- 原项目源码实现。
- 原项目角色美术。
- `assets/` 或 `themes/` 下的素材。

可以参考：

- 架构分层。
- 状态机设计。
- 主题配置字段。
- 交互流程。
- 资源组织方式。

MF 应该用自己的代码和“苗丰精灵”素材重新实现。

## 3. clawd-on-desk 技术栈

项目位置：

```text
F:\20260518-xiangmu\MF_Project\MF_Pet\clawd-on-desk
```

核心技术：

- Electron `^41`
- CommonJS Node.js
- electron-builder
- 本地 HTTP server
- Electron IPC
- 多窗口透明置顶 UI
- Hook / plugin 脚本集成多个 AI Agent

关键目录：

```text
src/              Electron 主进程、状态机、渲染、设置、气泡、会话等
assets/gif/       文档和默认角色 GIF 预览
assets/svg/       默认 Clawd SVG 动画资源
themes/           内置主题与主题模板
hooks/            Agent hook / plugin 安装与上报脚本
agents/           各 Agent 事件映射和能力声明
docs/guides/      状态映射、主题创建、安装指南
docs/project/     架构说明、主题/状态/UI 说明
```

## 4. 核心架构蒸馏

### 4.1 数据流

原项目典型链路：

```text
Agent 事件
  -> hook/plugin/log monitor
  -> 本地 HTTP POST /state
  -> state.js 状态机
  -> resolveDisplayState()
  -> resolveVisualBinding()
  -> IPC 通知 renderer
  -> renderer 切换 SVG/GIF/APNG/PNG
```

MF 复刻时可简化为：

```text
官网/商城业务事件
  -> MF_Pet event API
  -> MF_Pet 状态机
  -> 资源 resolver
  -> Web DOM 渲染 GIF/PNG/APNG
```

V1 不需要 Electron，也不需要本地 HTTP server。

### 4.2 状态优先级

原项目优先级：

```text
error(8)
notification(7)
sweeping(6)
attention(5)
carrying/juggling(4)
working(3)
thinking(2)
idle/roam(1)
sleeping(0)
```

MF 可改成品牌业务语义：

```text
error(8)        加载失败 / AI 不可用
unclear(7)      用户输入不清楚 / 需要追问
success(6)      推荐成功 / 浇水反馈
notification(5) 新消息 / 引导提醒
working(4)      AI 思考 / 查询中
expanded(3)     面板展开
appearing(2)    被唤出
idle(1)         待机
resting(0)      长时间无操作
hidden(-1)      隐藏
```

### 4.3 最短展示与自动回退

原项目有两类时间控制：

- `minDisplay`：防止状态快速闪切。
- `autoReturn`：一次性状态展示结束后回到当前主状态。

MF 应保留这个思想。建议：

```js
timings: {
  minDisplay: {
    working: 1000,
    success: 1800,
    unclear: 1800,
    error: 2500
  },
  autoReturn: {
    success: 2200,
    unclear: 2600,
    error: 3200
  },
  restingAfter: 60000
}
```

这样不会出现“点击一下按钮，GIF 立刻闪回 idle”的廉价感。

### 4.4 主题系统

原项目 `theme.json` 的核心字段：

```text
schemaVersion
name
version
viewBox
states
timings
hitBoxes
idleAnimations
reactions
miniMode
sounds
objectScale
```

MF 不需要一次性实现全部字段。建议先做一个精简版：

```json
{
  "schemaVersion": 1,
  "id": "mf-sprout",
  "name": "苗丰精灵",
  "version": "0.1.0",
  "canvas": { "width": 288, "height": 288 },
  "states": {
    "idle": ["idle/idle.gif"],
    "appearing": ["move/move.gif"],
    "working": ["work/work.gif"],
    "success": ["watering/watering.gif"],
    "unclear": ["doubt/doubt.gif"],
    "error": ["sad/sad.gif"],
    "resting": ["rest/rest.gif"],
    "expanded": { "fallbackTo": "idle" }
  },
  "timings": {
    "minDisplay": {},
    "autoReturn": {}
  },
  "reactions": {},
  "miniMode": {
    "supported": false
  }
}
```

后续有多个苗丰角色皮肤时，再扩展为真正的主题目录。

### 4.5 渲染层

原项目渲染层要处理：

- SVG `<object>` 通道。
- GIF / APNG / PNG `<img>` 通道。
- 资源切换淡入淡出。
- SVG 眼球追踪。
- 低功耗暂停。
- mini 翻转。

MF 当前素材是 GIF/PNG，Web 场景可以简化为：

```text
<img class="mf-pet-sprite" src="/assets/pet/idle/idle.gif">
```

必要能力：

- 状态切换时替换 `src`。
- 相同 GIF 需要重播时加 cache-bust。
- 切换时做 120ms - 180ms 淡入淡出。
- 所有素材共享 `288x288` 画布，避免跳动。

暂不需要：

- SVG object 通道。
- 眼球追踪。
- 多窗口 click-through。
- Electron hitbox。

### 4.6 交互层

原项目桌面交互：

- 拖拽。
- 双击反应。
- 连点反应。
- 鼠标静止睡眠。
- 拖到屏幕边缘进入 mini mode。

MF Web 复刻建议分阶段：

V1：

- 点击精灵展开 / 收起。
- 点击面板按钮触发状态反馈。
- 长时间无操作进入 `resting`。
- 鼠标移入或点击从 `resting` 回到 `idle`。

V2：

- 拖拽右下角精灵位置。
- 保存位置到 localStorage。
- 双击或多次点击触发反应动画。
- 移动端底部抽屉。

V3：

- 边缘迷你模式。
- 多皮肤主题。
- AI 会话状态真实驱动。

## 5. 与 MF_Pet 现状的对应关系

当前 `MF_Pet` 已有：

```text
src/forestPet.js
src/forestPet.css
src/petManifest.js
web-assets/pet/{state}/{state}.gif
tools/extract_status_gif.py
```

已经具备：

- 原生 JS 组件入口。
- 状态到 GIF 的映射。
- 展开/收起面板。
- localStorage 基础状态。
- 7 个状态 GIF。

下一步应该把它从“简单 manifest”升级成“简化主题 runtime”。

## 6. 推荐 MF_Pet 后续结构

```text
MF_Pet/
  src/
    core/
      petRuntime.js          初始化、销毁、事件入口
      petStateMachine.js     状态优先级、minDisplay、autoReturn
      petThemeLoader.js      读取/校验主题配置
      petVisualResolver.js   state -> asset
      petStorage.js          localStorage 封装
    web/
      forestPet.js           官网/商城可用的 DOM 组件
      forestPet.css
    themes/
      mf-sprout/
        theme.json
        assets/
          idle.gif
          move.gif
          rest.gif
          work.gif
          watering.gif
          doubt.gif
          sad.gif
  tools/
    extract_status_gif.py
  web-assets/
    pet/                     V1 兼容输出，可后续由 themes 生成
```

短期不必大搬家，但后续复刻 clawd 思想时应朝这个方向收敛。

## 7. 推荐 API

对宿主项目暴露统一事件 API：

```js
const pet = initForestPet({
  mount: document.body,
  theme: mfSproutTheme,
  assetsBaseUrl: "/assets/pet",
  lang: "zh",
  links,
});

pet.emit("summon");
pet.emit("expand");
pet.emit("ask-ai");
pet.emit("ai-working");
pet.emit("ai-success");
pet.emit("ai-unclear");
pet.emit("ai-error");
pet.emit("rest");
```

内部映射：

```js
const EVENT_TO_STATE = {
  summon: "appearing",
  expand: "expanded",
  "ask-ai": "working",
  "ai-working": "working",
  "ai-success": "success",
  "ai-unclear": "unclear",
  "ai-error": "error",
  rest: "resting",
};
```

这样 `MF_Website` 和商城只发语义事件，不直接操纵 GIF。

## 8. MF 项目应复刻的能力

### 必须复刻

- 状态机。
- 状态优先级。
- 最短展示时间。
- 一次性状态自动回退。
- 状态到素材的集中映射。
- 主题配置雏形。
- 点击展开/收起。
- 长时间无操作进入休息。
- 中英文文案。
- 宿主事件 API。

### 可以稍后复刻

- idle 随机动画池。
- 点击反应动画。
- 拖拽位置。
- 迷你模式。
- 音效。
- 多主题。
- 主题校验脚本。

### 不建议复刻到 Web V1

- Electron 双窗口模型。
- 系统托盘。
- 本地 HTTP server。
- Agent hooks。
- 权限审批气泡。
- 远程 SSH。
- PWA 手机伴侣。
- 终端聚焦。
- Windows/macOS/Linux 原生适配。

这些属于桌面应用能力，不适合直接塞进官网宠物 V1。

## 9. 实施路线

### Phase 1：把 MF_Pet 做稳

目标：让 MF_Pet 成为“可被多个前端消费的宠物组件”。

任务：

- 新增 `petStateMachine.js`。
- 新增 `petVisualResolver.js`。
- 把 `petManifest.js` 升级为主题配置。
- `forestPet.js` 改为调用 runtime，而不是自己保存全部状态逻辑。
- 增加 `emit(event)` API。
- 保留现有 GIF 资源。

验收：

- 不接官网也能在一个 demo HTML 中独立运行。
- 状态切换不会闪烁。
- `success/error/unclear` 能自动回到 `idle` 或 `expanded`。
- 60 秒无操作进入 `resting`。

### Phase 2：MF_Website 接入

目标：官网作为第一个宿主项目接入。

任务参考：

```text
F:\20260518-xiangmu\MF_Project\Folder_description\MF_Website_接入MF_Pet说明.md
```

原则：

- 官网只初始化组件和发送事件。
- 不把状态机复制到官网。
- 不改现有滚动模块。

### Phase 3：商城接入

目标：商城复用同一套宠物 runtime。

新增事件：

```text
product-view
add-cart
order-created
payment-success
encyclopedia-open
ai-question
ai-answer
```

可以根据业务场景触发：

- 推荐养护内容时 `success`。
- AI 查询中 `working`。
- 加载失败 `error`。
- 用户输入不清楚 `unclear`。

## 10. 对当前 MF_Pet 的具体改造建议

当前已有 `src/forestPet.js` 可以保留，但建议下一次改造成薄封装：

```text
forestPet.js
  -> 创建 DOM
  -> 绑定按钮
  -> 调用 petRuntime.emit()
  -> 根据 runtime snapshot render()
```

新增：

```text
src/core/petStateMachine.js
src/core/petVisualResolver.js
src/core/petRuntime.js
src/core/petThemeLoader.js
src/themes/mfSproutTheme.js
```

这样未来要做“真正像 clawd 一样有生命感”的复杂行为时，不会把 `forestPet.js` 越写越大。

## 11. 状态映射建议

| MF 事件 | 逻辑状态 | 当前素材 |
|---|---|---|
| 初次唤出 | `appearing` | `move/move.gif` |
| 无操作 | `idle` | `idle/idle.gif` |
| 展开面板 | `expanded` | `idle/idle.gif` |
| AI 查询中 | `working` | `work/work.gif` |
| 推荐成功 / 正反馈 | `success` | `watering/watering.gif` |
| 输入不明确 | `unclear` | `doubt/doubt.gif` |
| 错误 / 加载失败 | `error` | `sad/sad.gif` |
| 长时间无操作 | `resting` | `rest/rest.gif` |

## 12. 关键文件索引

clawd-on-desk 中值得参考的文件：

```text
README.zh-CN.md
docs/guides/state-mapping.zh-CN.md
docs/project/agent-runtime-architecture.md
docs/project/theme-state-ui.md
docs/guides/guide-theme-creation.md
src/state-priority.js
src/state-visual-resolver.js
src/theme-schema.js
themes/clawd/theme.json
themes/template/theme.json
src/renderer.js
src/state.js
src/mini.js
src/pet-window-runtime.js
```

注意：参考设计，不复制代码。

## 13. 最小复刻目标

MF 下一步可以先复刻一个“Web 版 clawd 核心”：

```text
业务事件输入
  -> 状态机
  -> 主题资源解析
  -> GIF 渲染
  -> 用户交互
```

先不做桌面系统能力。

这条路线既能吸收 `clawd-on-desk` 的成熟设计，又不会把 MF 官网宠物拖成一个过重的 Electron 工程。
