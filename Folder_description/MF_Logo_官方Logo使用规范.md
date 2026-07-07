# MF 官方 Logo 使用规范

本文档定义 MF 系列项目的官方 Logo 使用规则。后续所有 MF 项目在登录页、导航栏、侧边栏、品牌入口、文档页和对外展示页中使用 Logo 时，应以本文档为准。

## 1. 官方资产

官方 Logo 源文件：

```text
F:\20260518-xiangmu\MF_Project\MF_Logo\logo.png
```

当前官方资产说明：

- 文件名：`logo.png`
- 类型：PNG 位图
- 角色：MF 系列统一品牌主标识
- 状态：当前唯一官方主 Logo

各项目接入时，应从 `MF_Logo` 目录复制官方源文件到本项目静态资源目录，不要直接引用跨项目绝对路径。

不要使用旧目录或临时目录中的 Logo 作为主来源，例如：

```text
F:\20260518-xiangmu\MF_Project\logo
```

后续如新增 SVG、深色版、浅色版、favicon、小图标版，也必须统一放入：

```text
F:\20260518-xiangmu\MF_Project\MF_Logo
```

## 2. 品牌定位

MF Logo 代表整个苗丰项目矩阵，不只属于单一前端项目。

适用范围：

- `MF_Website`：苗丰生态官网
- `MF_EP`：电商平台，包括管理端、消费者端、商家端
- `MF_DataCenter`：数据中台
- `MF_AgentService`：客服 Agent 与 MCP 工具层
- `MF_Pet`：苗丰精灵
- 后续新增的 MF 系列项目

使用目标：

- 统一项目矩阵的品牌识别。
- 让管理端、数据中台、商家端、消费者端看起来属于同一套官方系统。
- 在不同产品形态中保持一致的 Logo 图形和基础呈现规范。

## 3. 基本使用原则

必须遵守：

- Logo 图形本身不得重绘、改色、拉伸、裁切。
- 不得给 Logo 加重滤镜、过重阴影、描边或变形效果。
- 不得使用低清截图替代官方源文件。
- 不得每个项目自行设计不同的 MF Logo。
- 不得同时混用旧 Logo 和官方 Logo。
- 在复杂背景上必须保证 Logo 清晰可读。

允许调整：

- 展示尺寸。
- Logo 外层容器大小。
- 容器背景色。
- Logo 与文字的组合排版。
- 根据页面类型选择横向、纵向或单独图形展示。

## 4. 推荐复制路径

### 4.1 MF_EP 管理端

推荐路径：

```text
MF_EP\projects\mf\mf-frontend\src\assets\brand\logo.png
```

推荐使用位置：

- 登录页品牌区。
- Layout 侧边栏顶部。
- 顶部栏品牌识别区，如有。

当前管理端已优先对齐 `MF_DataCenter` 的中台视觉风格。后续如果继续调整，应在侧边栏顶部品牌块和登录页表单卡片附近接入官方 Logo。

### 4.2 MF_EP 消费者端

推荐路径：

```text
MF_EP\projects\mf\mf-frontend-client\src\assets\brand\logo.png
```

推荐使用位置：

- 首页顶部导航左侧。
- 登录/注册页品牌区。
- 用户中心顶部品牌入口。
- 订单页或个人中心中需要体现平台归属的位置。

消费者端可以比管理端更温和、更亲和，但 Logo 必须使用同一官方源文件。不要为了前台风格单独改 Logo 颜色。

### 4.3 MF_EP 商家端

推荐路径：

```text
MF_EP\projects\mf\mf-frontend-merchant\src\assets\brand\logo.png
```

推荐使用位置：

- 商家登录页。
- 商家入驻/注册页。
- 商家端 Layout 侧边栏或顶部栏。
- 店铺资料页中说明平台归属的位置。

商家端后续如果和管理端统一主题，应优先在登录页和 Layout 品牌区加入官方 Logo，避免只用文字 `MF` 或纯文本标题。

### 4.4 MF_DataCenter

推荐路径：

```text
MF_DataCenter\datacenter-web\src\assets\brand\logo.png
```

推荐使用位置：

- 数据中台侧边栏顶部。
- 数据中台登录页，如后续新增。
- 数据中台顶部系统标识。

推荐组合：

```text
[logo] MF DataCenter
```

或：

```text
[logo] 苗丰数据中台
```

### 4.5 MF_Website

推荐路径：

```text
MF_Website\mf_Website\public\assets\brand\logo.png
```

推荐使用位置：

- 官网顶部导航左侧。
- 品牌首屏区域。
- 页脚品牌区。
- favicon 或启动图标的后续衍生素材。

官网导航推荐轻量组合：

```text
[logo] 苗丰 MF
```

### 4.6 MF_Pet

推荐路径：

```text
MF_Pet\assets\brand\logo.png
```

或在官网接入时使用：

```text
MF_Website\mf_Website\public\assets\brand\logo.png
```

推荐使用位置：

- 苗丰精灵欢迎面板。
- 精灵说明页。
- 与 MF 主平台产生关系说明的位置。

## 5. 尺寸规范

### 5.1 侧边栏

适用于管理端、商家端、数据中台：

- Logo 图形尺寸：`28px - 36px`
- 品牌块高度：`56px - 72px`
- 圆角容器：`8px`
- Logo 与文字间距：`8px - 12px`

推荐结构：

```text
[logo]  MF_EP
        苗丰运营后台
```

或：

```text
[logo]  MF 商家端
        平台审核商家工作台
```

### 5.2 顶部导航

适用于官网、消费者端：

- 桌面端 Logo 图形尺寸：`36px - 44px`
- 移动端 Logo 图形尺寸：`30px - 34px`
- 图文间距：`8px - 10px`

顶部导航不宜放过多副标题。建议保持：

```text
[logo] 苗丰 MF
```

### 5.3 登录页

适用于管理端、消费者端、商家端、数据中台：

- Logo 图形尺寸：`56px - 88px`
- 标题字号：`22px - 28px`
- 副标题字号：`13px - 15px`
- Logo 与标题间距：`12px - 18px`

登录页推荐组合：

```text
[logo]
苗丰运营后台
```

或：

```text
[logo]
苗丰商家端
```

### 5.4 Favicon 与小图标

如需 favicon，不要直接压缩完整大 Logo。应后续从官方源文件派生专用小图标：

```text
favicon.png
logo-mark.png
```

派生文件仍应放在：

```text
F:\20260518-xiangmu\MF_Project\MF_Logo
```

## 6. 安全区与留白

Logo 周围必须保留安全区，避免和文字、按钮、边框挤在一起。

推荐规则：

- Logo 外侧最小留白：不小于 Logo 图形宽度的 `25%`。
- 侧边栏品牌块内边距：`12px - 16px`。
- 登录页 Logo 上下留白：不小于 `12px`。
- 顶部导航 Logo 左侧距离窗口或容器边缘：不小于 `16px`。

不要让 Logo 贴边、压线或和文字重叠。

## 7. 背景与可读性

当前官方 Logo 为绿色系图形，在绿色、森林、植物或复杂背景上可能不够清晰。

推荐做法：

- 浅色后台页面：直接使用 Logo，外层可加浅绿色或白色容器。
- 深色背景：为 Logo 加浅色半透明底。
- 复杂图片背景：必须加白色或半透明白色底。
- 绿色背景：避免裸放绿色 Logo。

推荐容器：

```css
.brand-logo-box {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.86);
}
```

中后台推荐背景：

```css
background: #f7fbf6;
border: 1px solid #d4e2d6;
```

## 8. 图文组合规则

### 8.1 官方推荐组合

MF_EP 管理端：

```text
[logo] MF_EP
       苗丰运营后台
```

MF_EP 商家端：

```text
[logo] MF 商家端
       平台审核商家工作台
```

MF_EP 消费者端：

```text
[logo] 苗丰 MF
```

MF_DataCenter：

```text
[logo] DataCenter
       苗丰数据中台
```

MF_Website：

```text
[logo] 苗丰 MF
```

### 8.2 不推荐组合

不要使用：

```text
[logo] 苗丰施肥管控平台后台管理系统超级管理控制台
```

原因：品牌区文字过长，会导致侧边栏、移动端和登录页视觉拥挤。

## 9. MF_EP 后续主题统一要求

当后续将 MF_EP 消费者端和商家端主题样式调整为与管理端一致时，必须同步处理 Logo 接入。

### 9.1 管理端

管理端已经建立中后台统一主题变量和 Layout 风格。后续补 Logo 时优先放在：

- 登录页右侧表单卡片顶部，或左侧品牌动画区标题附近。
- Layout 侧边栏顶部品牌块。

要求：

- 不改变管理端路由、权限、接口。
- 不重构业务页面。
- 只做品牌图形替换和样式微调。

### 9.2 商家端

商家端主题统一时，必须加入官方 Logo。

优先位置：

1. 登录页标题区。
2. 注册/入驻申请页标题区。
3. 商家端 Layout 侧边栏顶部。

推荐实现：

```text
src\assets\brand\logo.png
```

```vue
<img src="@/assets/brand/logo.png" alt="苗丰" class="brand-logo" />
```

### 9.3 消费者端

消费者端主题统一时，必须加入官方 Logo，但风格可以比后台更轻。

优先位置：

1. 首页顶部导航左侧。
2. 登录/注册页。
3. 用户中心或个人页顶部。

注意：

- 不要让消费者端变成纯后台感。
- Logo 保持官方一致，页面氛围可以更温和。
- 不改购物、订单、支付、用户资料等业务逻辑。

## 10. 接入流程

每个项目接入 Logo 时按以下步骤执行：

1. 从官方目录复制：

```text
F:\20260518-xiangmu\MF_Project\MF_Logo\logo.png
```

2. 放入当前项目的品牌资源目录。

3. 在登录页、导航栏或侧边栏引入。

4. 根据背景决定是否加浅色容器。

5. 检查移动端和桌面端尺寸。

6. 运行对应构建命令。

7. 最终报告中说明：

- 复制到了哪个路径。
- 使用在哪些页面或组件。
- 是否加了背景容器。
- 构建是否通过。
- 是否存在需要人工确认的视觉差异。

## 11. 禁止事项清单

禁止：

- 拉伸 Logo。
- 压扁 Logo。
- 裁切主体图形。
- 改变 Logo 主体颜色。
- 使用模糊截图。
- 自行重画近似 Logo。
- 给不同项目设计不同 MF Logo。
- 只在某些端使用新 Logo，其他端继续使用旧 Logo。
- 在复杂背景上裸放导致不可读。
- 把 Logo 当作普通装饰图随意旋转、透明化或加动画。

## 12. 后续资产扩展

建议后续补充以下官方资产：

- `logo.svg`：矢量主标识。
- `logo.png`：当前主标识。
- `logo-light.png`：深色背景专用版。
- `logo-dark.png`：浅色背景专用版。
- `logo-mark.png`：只含图形的小图标版。
- `favicon.png`：浏览器图标。
- `mf-pet-mark.png`：苗丰精灵专用图标。

所有扩展资产必须统一放在：

```text
F:\20260518-xiangmu\MF_Project\MF_Logo
```

各项目只能复制使用，不应自行维护品牌源文件。

