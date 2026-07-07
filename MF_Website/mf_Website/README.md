# mf_Website

一个基于 Vite、GSAP ScrollTrigger 和 Lenis 的互动滚动页面。

## Scripts

- `npm run dev`：本地开发服务，端口由 `vite.config.js` 固定。
- `npm run build`：生产构建验证。
- `npm run preview`：预览构建产物。

## Project Map

- `index.html`：页面结构、资源预加载和各 section 的 HTML。
- `src/main.js`：应用入口，只负责注册插件、初始化全局系统和各 section。
- `src/sections/`：每个滚动段落的交互逻辑。
  - `preloader.js`：开场加载、按钮描边、点击进入和水波纹过渡。
  - `locationScroll.js`：Jiuzhaigou location hero、遮罩洞、marker 和图片滚动。
  - `splitCards.js`：Protect a Living Forest 三卡分开、翻转和离场。
  - `clientPanels.js`：后半段 KEEP/服务卡片滚动和翻转。
- `src/shared/`：跨 section 复用的小工具。
  - `scroll.js`：Lenis 初始化和回到顶部。
  - `i18n.js`：中英文文案切换。
  - `math.js`：动画插值函数。
  - `viewport.js`：移动端断点、真实可视高度和 resize 重建工具。
- `src/styles/`：按 section 拆分的样式文件，统一由 `src/styles.css` 引入。
- `public/assets/`：图片、logo、网格遮罩等静态资源。

## Maintenance Notes

- 桌面端和移动端动画参数尽量留在对应 section 文件里；跨模块重复的 viewport 判断放在 `src/shared/viewport.js`。
- 修改移动端滚动高度时，优先使用 `getAnimationViewportHeight()`，避免手机浏览器地址栏变化导致 pinned section 抖动。
- 新增 section 时，建议按现有模式拆成一个 `src/sections/*.js` 和一个 `src/styles/*.css`，再在 `src/main.js` 和 `src/styles.css` 注册。
