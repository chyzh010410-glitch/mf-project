# 三张卡片翻转区问题交接文档

## 背景

项目位置：`F:\20260518-xiangmu\xmu\mf_Website`

目标区域是首页中段的三张卡片翻转区：

- HTML：`index.html` 中 `.sticky` / `.card-container` / `#card-1` `#card-2` `#card-3`
- 样式：`src/styles/splitCards.css`
- 动画：`src/sections/splitCards.js`

当前希望保留的体验：

- 三张卡片初始阶段要能合成一张完整照片。
- 滚动后卡片翻转到背面。
- 翻转完成后卡片可以轻微 CSS 浮动。
- 后续进入 `Keep scrolling - it gets good` 区域的滚动衔接不要被破坏。

## 当前主要 bug

### 1. 中间卡片和第三张卡片之间有细缝

现象：

- 在三张卡片还显示正面照片时，中间卡片和第三张卡片交界处出现一条细暗线。
- 这条线在截图中像 1px 黑色竖线。
- 用户明确要求：不能靠简单左右出血 `1px` 来遮，因为那会破坏三张卡片合成一张完整照片的准确性。

可能原因：

- 当前正面照片由三个独立 `.card-front` 背景层拼接。
- flex 宽度、3D transform、GPU 合成、圆角抗锯齿会产生亚像素误差。
- 即使动态设置背景坐标，也可能在独立 DOM 图层边界出现渲染缝。

### 2. 上下滚动时三张卡片有轻微抖动

现象：

- 用户在照片阶段上下慢速滚动时，卡片可能出现轻微上下抖动。
- 有时看起来三张卡片高度不一致。

可能原因：

- CSS 浮动 `.sticky.is-floating .card-float` 与 GSAP scroll scrub 控制同一组卡片 transform。
- 浮动启动时间如果过早，会在照片还没完全翻走时影响正面阶段。
- `scrub` 动画滞后滚动位置，也可能让视觉状态短暂不同步。

### 3. 回滚时卡片状态可能抽动

现象：

- 从 `Keep scrolling - it gets good` 页面往上回滚到 `Protect a Living Forest` 顶部时，三张卡片会像跳频一样抽一下。

可能原因：

- `split-cards` 和 `split-cards-exit` 两个 ScrollTrigger 都会影响同一批卡片的 transform。
- `split-cards-exit` 的 `onLeaveBack` 会瞬间 reset `xPercent / yPercent / scale / opacity`，可能与上一个 pinned ScrollTrigger 的状态交接冲突。

### 4. 快速上下滚动时可能卡在中间态

现象：

- 卡片正面已经回来了，但左右两张卡还保持扇形展开。
- 或卡片翻转状态和展开状态不同步。

可能原因：

- `rotationY` 翻转和左右卡片的 `rotationZ / y` 展开在时间轴中部分重叠。
- 回滚时 scrub 停在中间进度，会出现“正面图 + 已展开”的违和状态。

## 当前代码状态

当前代码已经回退到用户觉得“项目很好”的版本附近：

- `FLOAT_START_PROGRESS = 0.72`
- CSS 浮动已恢复，由 `.sticky.is-floating .card-float` 控制。
- `.card-front` 使用共享背景变量：
  - `--card-bg-width`
  - `--card-bg-x`
- `syncCardFrontBackground()` 仍在 `src/sections/splitCards.js` 中，用于同步三张卡片正面的背景位置。
- 没有使用单独的 `card-photo-composite` 整图层；之前尝试过，但过渡到翻转时违和，已撤回。

## 已尝试但不理想的方案

### 1. `.card-front` 左右各做 `1px` 出血

结果：

- 能遮住黑缝。
- 但会让每张卡的背景区域变宽，破坏三张卡合成完整照片的准确性。

结论：不要使用这个方案。

### 2. 用 `getBoundingClientRect()` 小数像素同步背景

结果：

- 理论上比 `offsetLeft / offsetWidth` 准确。
- 但实际截图中细缝仍可能出现。

结论：只能作为辅助，不是根治方案。

### 3. 单独添加 `card-photo-composite` 整图层

结果：

- 静态照片阶段确实能彻底去掉缝。
- 但整图层和三张卡片正面切片交接到翻转时容易出现半透明叠加、画面变脏、过渡违和。

结论：如果重新使用这个方案，必须重点设计“整图层到三张卡片翻转”的交接，不能简单淡入淡出。

## 建议修复方向

优先建议从动画阶段分离入手：

1. 正面照片阶段必须稳定
   - 不启动 CSS 浮动。
   - 不提前打开 gap。
   - 不提前让内部边缘圆角。
   - 三张卡片高度和 transform 必须保持一致。

2. 翻转阶段再拆卡
   - 照片阶段先保持无缝整图。
   - 快到翻转时再执行 gap / border-radius / rotationY。
   - 左右卡片的 `rotationZ / y` 展开最好在翻转完成后再开始。

3. 避免多个 ScrollTrigger 同时抢同一 transform 属性
   - 主时间轴负责：宽度、gap、翻转、扇形展开。
   - exit 时间轴负责：下移、缩放、透明度。
   - 尽量不要两个时间轴都控制同一个属性。

4. 如果继续用三张背景拼图
   - 保持共享背景坐标方案。
   - 内部边缘不要圆角。
   - 避免 `filter`、`opacity`、`translateZ` 造成额外合成层边界。

5. 如果改用单图层整图方案
   - 整图层不要和三张正面切片长时间半透明叠加。
   - 可以尝试在翻转开始的一瞬间硬切换，而不是淡入叠加。
   - 或者让整图层本身被裁成三片并参与翻转，但不要再由三个独立背景重新拼接。

## 需要保持不变的范围

不要改：

- preloader 开场水波纹。
- location hero 滚动段。
- `Keep scrolling - it gets good` 文案和 client panels 内容。
- 三张卡片背面的文字和颜色风格。
- 当前整体视觉风格、字体和暗绿色背景。

## 验收标准

桌面端必须检查：

- 初始照片阶段，中间卡片和第三张卡片之间没有细黑缝。
- 三张卡片正面能自然组成一张完整照片。
- 上下慢速滚动时，照片阶段没有高度不一致和轻微抖动。
- 翻转到背面时不卡顿、不闪烁、不出现正面图残影。
- 翻转完成后 CSS 浮动正常。
- 从 `Keep scrolling - it gets good` 回滚到卡片区时，没有跳频式抽动。

移动端检查：

- 移动端仍按当前纵向卡片布局显示。
- 不出现桌面端专用定位或 transform 残留。

构建验证：

```bash
npm run build
```

