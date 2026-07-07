# MF_Website 接入 MF_Pet 说明

本文档给负责 `MF_Website` 的开发者使用。当前阶段不要在 `MF_Pet` 内直接改官网代码；官网只消费 `MF_Pet` 输出的通用组件与 Web 静态资源。

## 1. 目标

在 `MF_Website` 中接入苗丰精灵：

- 顶部导航点击 `苗丰精灵` 后唤出精灵。
- 精灵常驻右下角或移动端底部偏右。
- 支持展开/收起快捷面板。
- 支持中英文文案。
- 快捷入口包括商城、百科、AI 咨询、商家入驻。
- 不破坏现有 preloader、location hero、三卡翻转、client panels 和滚动动画。

## 2. 先读文件

请先阅读：

```text
F:\20260518-xiangmu\MF_Project\Folder_description\MF_Pet_开发任务文档.md
F:\20260518-xiangmu\MF_Project\Folder_description\MF_Website_设计方针.md
F:\20260518-xiangmu\MF_Project\MF_Pet\README.md
F:\20260518-xiangmu\MF_Project\MF_Pet\src\forestPet.js
F:\20260518-xiangmu\MF_Project\MF_Pet\src\petManifest.js
F:\20260518-xiangmu\MF_Project\MF_Pet\src\forestPet.css
```

官网项目位置：

```text
F:\20260518-xiangmu\MF_Project\MF_Website\mf_Website
```

## 3. MF_Pet 当前输出

`MF_Pet` 已提供：

```text
MF_Pet/src/forestPet.js       原生 JS 组件入口
MF_Pet/src/petManifest.js     状态、资源、文案、链接默认配置
MF_Pet/src/forestPet.css      通用精灵样式
MF_Pet/web-assets/pet/        Web 可用 GIF 与透明 PNG 帧
```

已导出状态：

```text
doubt
move
rest
sad
work
idle
watering
```

每个状态包含：

```text
{state}/{state}.gif
{state}/frames/01.png ... 08.png
```

规格：

- 8 帧。
- 每帧 `288x288`。
- PNG 为透明背景。
- GIF 循环播放。
- GIF 单帧 `200ms`。

## 4. 资源复制

官网运行时不要直接引用：

```text
MF_Pet/Pet_Design_Drawing/Status
```

请把：

```text
F:\20260518-xiangmu\MF_Project\MF_Pet\web-assets\pet
```

复制到：

```text
F:\20260518-xiangmu\MF_Project\MF_Website\mf_Website\public\assets\pet
```

这样组件可以通过以下路径访问资源：

```text
/assets/pet/idle/idle.gif
/assets/pet/move/move.gif
/assets/pet/rest/rest.gif
/assets/pet/work/work.gif
/assets/pet/watering/watering.gif
/assets/pet/doubt/doubt.gif
/assets/pet/sad/sad.gif
```

## 5. 官网建议新增文件

建议在官网内新增一个轻薄适配层，而不是把所有精灵逻辑重新写一遍：

```text
MF_Website/mf_Website/src/sections/forestPetAdapter.js
MF_Website/mf_Website/src/styles/forestPetAdapter.css
```

适配层职责：

- 从 `MF_Pet` 组件初始化精灵。
- 根据官网语言传入 `zh` 或 `en`。
- 传入统一链接配置。
- 暴露 `show({ expand: true })` 给导航按钮调用。
- 如需官网局部样式微调，写在 `forestPetAdapter.css`。

不要重构：

```text
src/sections/locationScroll.js
src/sections/splitCards.js
src/sections/clientPanels.js
src/sections/preloader.js
```

## 6. 组件初始化示例

可把 `MF_Pet/src` 中的组件复制到官网 `src/shared/mfPet/`，或后续改成 npm/workspace 包引用。V1 最简单方式是复制组件源码与 CSS。

示例：

```js
import { initForestPet } from "../shared/mfPet/forestPet.js";
import "../shared/mfPet/forestPet.css";

const MF_LINKS = {
    mall: "#",
    encyclopedia: "#",
    ai: "#",
    merchant: "#",
};

export function initForestPetAdapter() {
    const lang = localStorage.getItem("forest-site-lang") === "en" ? "en" : "zh";
    const pet = initForestPet({
        mount: document.body,
        assetsBaseUrl: "/assets/pet",
        lang,
        links: MF_LINKS,
        storageKey: "mf-website-forest-pet",
        initiallyVisible: false,
        initiallyExpanded: false,
        onAskAi() {
            // V1 可先跳转 AI 页面，或打开官网预留对话面板。
        },
    });

    document.querySelectorAll("[data-forest-pet-trigger]").forEach((trigger) => {
        trigger.addEventListener("click", () => {
            pet.show({ expand: true });
        });
    });

    return pet;
}
```

然后在官网 `src/main.js` 中注册：

```js
import { initForestPetAdapter } from "./sections/forestPetAdapter.js";

// initLanguageSystem() 后执行即可
initLanguageSystem();
const forestPet = initForestPetAdapter();
```

## 7. 导航联动

桌面端导航目标：

```text
[logo] 苗丰 MF        生态故事   树木百科      [小树图标 苗丰精灵]      苗木商城   商家入驻        进入商城   EN
```

移动端导航目标：

```text
[logo] 苗丰 MF                    精灵   商城   EN   菜单
```

导航里的精灵按钮请加：

```html
<button type="button" data-forest-pet-trigger>
  <img src="/assets/pet/idle/idle.gif" alt="">
  <span>苗丰精灵</span>
</button>
```

移动端可显示短文案：

```html
<button type="button" data-forest-pet-trigger>精灵</button>
```

导航图标可用 `32px`，但右下角常驻精灵建议使用组件默认 `72px - 96px` 展示。

## 8. i18n 接入

`MF_Website` 目前通过 `src/shared/i18n.js` 做 selector 文本替换。官网接入时需要：

- 给导航文案补充中英文 selector。
- 点击语言切换后，当前官网会 reload，精灵会从 localStorage 恢复状态。
- 如果后续改成不刷新切语言，调用 `pet.setLanguage("zh" | "en")` 即可。

组件内部已经包含精灵面板中英文文案：

```text
你好，我是苗丰精灵。 / Hi, I am the MF Forest Pet.
进入商城 / Enter Mall
查看树木百科 / Tree Encyclopedia
问苗丰精灵 / Ask MF Pet
商家入驻 / Merchant Entry
```

## 9. 状态映射

官网可以按以下状态调用：

```text
appearing -> move.gif
idle      -> idle.gif
expanded  -> idle.gif
working   -> work.gif
success   -> watering.gif
unclear   -> doubt.gif
error     -> sad.gif
resting   -> rest.gif
```

示例：

```js
pet.setState("working");
pet.setState("success");
pet.setState("resting");
```

## 10. 验证命令

在官网目录执行：

```bash
cd F:\20260518-xiangmu\MF_Project\MF_Website\mf_Website
npm run build
```

手动验证：

- 桌面端第一屏导航正常显示。
- 点击 `苗丰精灵` 后精灵出现。
- 精灵 GIF 正常播放。
- 展开/收起面板正常。
- 快捷入口按钮可点击。
- 切换中英文后文案同步。
- 移动端默认能看到 `精灵`、`商城`、`EN`、菜单。
- 移动端面板高度不超过视口约 `60%`。
- 原有 preloader、location hero、三卡翻转、client panels 正常。

## 11. 非目标

本次官网接入不要做：

- 宠物养成系统。
- 等级、经验、喂养、换装。
- 用户账号绑定。
- 后端数据库表。
- 真实 AI 流式对话。
- 大范围重构现有滚动动画。

AI 咨询在 V1 只做入口级集成。
