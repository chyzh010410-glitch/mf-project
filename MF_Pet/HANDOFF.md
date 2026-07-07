# MF_Pet HANDOFF

## 当前完成度

整体约 80%。当前重点目标已收窄为“完成桌面壳复刻”，这一块已完成第一版可运行实现。

桌面壳当前完成度约 70%：
- 已完成：Electron 透明无边框窗口、always-on-top、跳过任务栏、托盘菜单、加载 clawd GIF、接入 MF_Pet runtime。
- 未完成：更完整的 clawd-on-desk 原生窗口行为，例如多屏边界夹取、Windows 顶层恢复 watchdog、真实系统级点击穿透/输入路由、设置窗口、更新气泡、完整 agent hook 集成。

## 如何运行

在 `F:\20260518-xiangmu\MF_Project\MF_Pet` 下运行：

```bash
npm run desktop
```

说明：
- `package.json` 中的 `desktop` 脚本会执行 `node desktop/launch.cjs`。
- 如果 `MF_Pet/node_modules/electron` 不存在，会复用 `clawd-on-desk/node_modules/electron`。
- 启动后应在桌面右下附近看到宠物，并在系统托盘出现 `MF_Pet Desktop`。

Web demo 仍可用：

```bash
python -m http.server 5179
```

然后打开：

```text
http://127.0.0.1:5179/demo/clawd-style-demo.html
```

## 关键文件

- `desktop/main.cjs`
  - Electron 主进程。
  - 创建透明无边框 `BrowserWindow`。
  - 设置 `alwaysOnTop`、`skipTaskbar`、托盘菜单。
  - 暴露 `mf-pet-shell:get-capabilities`、`mf-pet-shell:move-by`、`mf-pet-shell:hide`。

- `desktop/preload.cjs`
  - 通过 `contextBridge` 暴露 `window.mfPetShell`。

- `desktop/index.html`
  - 桌面壳渲染入口。
  - 加载 `../src/forestPet.css` 和 `initForestPet`。
  - 默认使用 `clawdDevTheme` 与 `../clawd-on-desk/assets/gif`。
  - 设置 `shellMode: "desktop"` 和 `shellCapabilities`。
  - `Alt + 拖拽宠物` 可移动原生透明窗口。

- `src/core/petRuntime.js`
  - 已加入 `shellMode` / `shellCapabilities`。
  - 已提供 `setShellCapabilities()`。

- `src/forestPet.js`
  - 根节点已输出 `data-shell-mode`。
  - 右键菜单已分组。
  - mini hover 已正确读取 `theme.miniMode.states["mini-peek"]`。
  - 拖到屏幕边缘进入 mini 时会清掉旧定位，让左右边缘样式接管。

- `src/forestPet.css`
  - 已加入桌面模式样式。
  - mini 贴边默认露出约 `38px`，hover 露出约 `56px`。
  - 桌面模式下菜单层级更高。

- `src/themes/clawdDevTheme.js`
  - 当前开发阶段默认套用 clawd-on-desk 的 GIF。
  - 注意：这只是本地开发占位主题，不应作为 MF 正式发布资源。

## 已完成的 clawd 风格复刻点

- 裸精灵显示，不再放进圆形盒子。
- 桌面模式 `shellMode=desktop`。
- clawd GIF 作为开发占位动画。
- task/session HUD。
- 右键菜单基础项与分组。
- mini 贴边与 hover peek。
- 通知气泡、权限气泡、聊天气泡、任务面板。
- 主题系统和资源解析可在 clawd / MF 主题间切换。

## 未完成 / 后续建议

优先级从高到低：

1. 完善桌面壳原生能力
   - 多屏幕 workArea 夹取。
   - 窗口贴边/恢复位置持久化。
   - 顶层丢失后的恢复逻辑，参考 `clawd-on-desk/src/topmost-runtime.js`。
   - 系统休眠/唤醒恢复，参考 `clawd-on-desk/src/system-wake-recovery.js`。

2. 更贴近 clawd-on-desk 的桌宠输入模型
   - 普通拖拽移动宠物位置。
   - 原生窗口移动和宠物内部拖拽的边界需要进一步统一。
   - 当前临时方案是 `Alt + 拖拽宠物` 移动原生窗口。

3. 补真实设置/任务窗口
   - 当前只有 Web 内 dashboard。
   - clawd-on-desk 是独立 BrowserWindow 的 Dashboard / Settings。

4. MF 正式资源替换
   - 当前 `clawdDevTheme` 只用于开发阶段。
   - 正式交付应切换到 `web-assets/pet` 或新的 MF 宠物模组。

5. 统一验证
   - 用户之前要求语法、行为、静态验证后面统一做。
   - 目前只做了轻量验证：`node --check`、资源存在检查、HTTP 200、实际启动 Electron 进程。

## 最近验证记录

已执行并通过：

```bash
node --check desktop/main.cjs
node --check desktop/preload.cjs
node --check desktop/launch.cjs
```

已确认：
- `package.json` 存在 `desktop` 脚本。
- `clawd-on-desk/node_modules/electron` 可被复用。
- `clawd-on-desk/assets/icon.png` 存在。
- `clawd-on-desk/assets/gif/clawd-typing.gif` 存在。
- 启动后出现 Electron 进程。

## 当前注意事项

- `MF_Project` 根目录不是 git 仓库，不能依赖 `git status`。
- `README.md` 中部分旧文本在 PowerShell 里显示为乱码，但文件仍可继续追加内容。
- 现在桌面壳是轻量第一版，不等价于完整 clawd-on-desk 主进程。
- 后续若继续复刻，应优先蒸馏并迁移：
  - `clawd-on-desk/src/topmost-runtime.js`
  - `clawd-on-desk/src/work-area.js`
  - `clawd-on-desk/src/visible-margins.js`
  - `clawd-on-desk/src/tick.js`
  - `clawd-on-desk/src/dashboard.js`
  - `clawd-on-desk/src/settings-window.js`
