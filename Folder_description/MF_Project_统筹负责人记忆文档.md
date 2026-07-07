# MF Project 统筹负责人记忆文档

更新时间：2026-07-06  
适用对象：新总负责人、后续统筹 Agent、联调负责人

## 1. 这份文档怎么用

这是一份“接手记忆文档”，用于让新的负责人快速进入 `MF_Project` 全局上下文。

不要一上来扫描整个目录。建议先按本文的阅读顺序理解项目边界，再根据当前任务进入具体项目。

项目根目录：

```text
F:\20260518-xiangmu\MF_Project
```

核心说明目录：

```text
F:\20260518-xiangmu\MF_Project\Folder_description
```

## 2. 项目组合总览

当前 MF_Project 不是单一项目，而是一组围绕“苗丰”品牌形成的多项目体系：

| 项目 | 作用 | 当前状态 |
| --- | --- | --- |
| `MF_EP` | 主业务系统：电商、内容、用户、商家、订单、管理端、客户端、商家端 | 已具备运行与构建能力，正在统一视觉与品牌 |
| `MF_DataCenter` | 数据中台：指标、数据质量、源表契约、治理状态、AI 数据沉淀 | 已完成 V1 可运行版本，健康检查通过 |
| `MF_AgentService` | 智能客服 Agent：聊天接口、工具调用、写入 DataCenter | 已完成 V1 可接入状态 |
| `MF_Pet` | 苗丰精灵 runtime / 桌宠 / Web 组件 | 已有可演示原型，待和 Website/AgentService 联调 |
| `MF_Website` | 生态品牌官网，承载苗丰精灵入口 | 已有设计方针和接入说明，待真实接入 MF_Pet |
| `MF_Logo` | 官方 logo 源文件 | 已作为统一品牌资产来源 |

## 3. 总体产品定位

整个体系可以这样对外描述：

> MF 是一个以苗木肥料垂直电商为核心，融合生态品牌官网、苗丰精灵、轻量数据中台、MCP 工具调用和智能客服 Agent 的 AI 应用型电商平台。

不要把它讲成单纯商城，也不要讲成纯 AI Demo。

更准确的层次：

```text
业务底座：MF_EP
品牌入口：MF_Website
交互角色：MF_Pet / 苗丰精灵
AI 大脑：MF_AgentService
数据资产：MF_DataCenter
品牌资产：MF_Logo
```

## 4. 必读文档顺序

新负责人建议按以下顺序阅读：

### 4.1 总览与联调

```text
Folder_description\MF_全项目相互调用与联调总览.md
Folder_description\MF_Project_项目进度与联调使用说明.md
```

### 4.2 数据与 Agent

```text
Folder_description\MF_DataCenter_项目进度与使用交接说明.md
Folder_description\MF_AgentService_项目进度与接入说明.md
Folder_description\MF_DataCenter_企业级架构文档.md
Folder_description\MF_DataCenter_AI_Agent_开发文档.md
```

### 4.3 宠物与官网

```text
Folder_description\MF_Pet_项目进度与接入交接说明.md
Folder_description\MF_Website_接入MF_Pet说明.md
Folder_description\MF_Website_设计方针.md
Folder_description\MF_Pet_开发任务文档.md
```

### 4.4 品牌

```text
Folder_description\MF_Logo_官方Logo使用规范.md
```

### 4.5 主业务系统

```text
MF_EP\projects\mf\项目总结.md
MF_EP\projects\mf\memory\agent-handoff.md
MF_EP\projects\mf\manual-regression-checklist.md
```

## 5. 当前阶段的核心结论

### 5.1 MF_EP

路径：

```text
F:\20260518-xiangmu\MF_Project\MF_EP\projects\mf
```

结论：

- 是主业务系统。
- 后端端口 `8080`。
- 数据库 `fertilizer`。
- Redis 依赖 `127.0.0.1:6379`，密码 `123456`。
- 管理端已向 DataCenter 中台视觉靠拢。
- 客户端和商家端已接入官方 logo，并改造登录页风格。
- `admin / admin123` 在后端和 Redis 正常时可登录。

注意：

- 不要让其他项目直接修改 `fertilizer` 业务表。
- 不要随便重构订单、支付、商家、权限等业务逻辑。
- 构建警告里 VueUse PURE 和大 chunk 警告目前不是阻塞项。

### 5.2 MF_DataCenter

路径：

```text
F:\20260518-xiangmu\MF_Project\MF_DataCenter
```

结论：

- 后端 `8091`，前端 `5176`。
- 自有库 `mf_datacenter`。
- 只读 MF_EP 的 `fertilizer`。
- Flyway 已迁移到 `v8`。
- 已有指标治理、数据质量、源表契约、治理可信状态。
- 健康检查脚本通过时，治理状态应为 `trusted`。

常用命令：

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_DataCenter
.\scripts\start-local.ps1
.\scripts\check-health.ps1
.\scripts\stop-local.ps1
```

注意：

- 当前无独立登录权限。
- 不能直接暴露公网。
- 后续需要确定生产 Web 托管方式。

### 5.3 MF_AgentService

路径：

```text
F:\20260518-xiangmu\MF_Project\MF_AgentService
```

结论：

- 后端端口 `8092`。
- 核心接口 `POST /api/agent/chat`。
- 已实现规则型 Agent 编排。
- 已实现 MCP 风格工具命名。
- 已有商品、百科、订单、商家入驻、DataCenter 写入工具。
- 已通过 `mvn test`，测试数为 6 个。
- 真实 LLM 默认关闭。

启动：

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_AgentService
mvn spring-boot:run
```

健康检查：

```text
http://localhost:8092/actuator/health
```

注意：

- `MF_AGENT_LLM_ENABLED=false` 时，不调用真实模型。
- 订单查询必须传 token。
- Agent 不做高风险业务动作。
- 当前不是完整 Agent 平台，是智能客服 Agent V1。

### 5.4 MF_Pet

路径：

```text
F:\20260518-xiangmu\MF_Project\MF_Pet
```

结论：

- 是苗丰精灵 runtime 和桌宠方向。
- 当前整体约 80%，桌面壳约 70%。
- 已有 `initForestPet()`、状态机、气泡、HUD、mini、右键菜单。
- Electron 桌面壳已有第一版。
- 当前使用 clawd GIF 作为开发占位，不是最终正式素材。

运行桌面壳：

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_Pet
npm run desktop
```

运行 Web Demo：

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_Pet
python -m http.server 5179
```

访问：

```text
http://127.0.0.1:5179/demo/clawd-style-demo.html
```

注意：

- MF_Pet 只负责表现和交互。
- AI 能力来自 MF_AgentService。
- 业务数据来自 MF_EP，经 AgentService 工具层调用。
- 正式素材和完整桌面壳能力仍是下一阶段。

### 5.5 MF_Website

路径：

```text
F:\20260518-xiangmu\MF_Project\MF_Website\mf_Website
```

结论：

- 是生态品牌官网。
- 导航设计围绕“苗丰精灵”居中。
- 下一步重点是接入 MF_Pet runtime。
- 不要把官网做成普通环保站，要承接商城、百科、商家入驻、苗丰精灵入口。

推荐导航：

```text
[logo] 苗丰 MF        生态故事   树木百科      [小树图标 苗丰精灵]      苗木商城   商家入驻        进入商城   EN
```

### 5.6 MF_Logo

路径：

```text
F:\20260518-xiangmu\MF_Project\MF_Logo
```

结论：

- 官方 logo 源文件以该目录为准。
- 各项目负责人应复制到自己项目的 assets/brand 目录。
- 不要使用旧 logo 目录作为主来源。

## 6. 统筹负责人最该盯的事

### 6.1 第一优先级：打通端到端链路

目标链路：

```text
MF_Website
  点击苗丰精灵
MF_Pet
  展开聊天面板
MF_AgentService
  /api/agent/chat
MF_EP
  查询商品/百科/订单
MF_DataCenter
  写入咨询日志/问题池/样本池
```

这条链路打通后，整个体系就从“多个项目并行”变成“一个完整 AI 应用系统”。

### 6.2 第二优先级：统一启动与健康检查

当前各项目都有自己的启动方式，建议总负责人做一个根目录总控脚本：

```text
1. 检查 MySQL
2. 检查 Redis
3. 启动 MF_EP 后端
4. 启动 MF_DataCenter
5. 启动 MF_AgentService
6. 启动需要联调的前端
7. 执行健康检查
```

### 6.3 第三优先级：固定端口

建议固定：

| 系统 | 建议端口 |
| --- | --- |
| MF_EP 后端 | `8080` |
| MF_EP 管理端 | `5173` |
| MF_EP 客户端 | 建议固定，例如 `5174` |
| MF_EP 商家端 | 建议固定，例如 `5175` |
| MF_DataCenter 后端 | `8091` |
| MF_DataCenter 前端 | `5176` |
| MF_AgentService | `8092` |
| MF_Pet Demo | `5179` |

### 6.4 第四优先级：生产边界

需要后续决定：

- DataCenter 是否先加登录权限。
- Website 如何部署并反代 Pet/AgentService。
- AgentService 是否开启真实 LLM。
- OpenAI API Key 如何通过环境变量管理。
- 是否需要统一网关。
- 是否把 MCP 工具层从 AgentService 拆出去。

## 7. 常见误区

1. 不要把 MF_DataCenter 当成 MF_EP 的管理后台替代品。
2. 不要让 DataCenter 直接修改 `fertilizer`。
3. 不要让 MF_Pet 直接查商品、订单或数据库。
4. 不要把 MF_AgentService 说成完整 Agent 集群。
5. 不要一开始就拆独立 `MF_MCP_Server`，当前 MCP 风格工具层在 AgentService 内部即可。
6. 不要把 clawd GIF 当成 MF 正式宠物素材。
7. 不要把官网做成和主业务无关的纯环保展示页。
8. 不要把真实 LLM 开关默认打开到生产环境。

## 8. 当前可讲的架构亮点

用于汇报或简历时，可以这样组织：

- 多端业务系统：管理端、客户端、商家端。
- 数据中台：只读业务库，形成指标、质量、治理、可信状态。
- 智能客服 Agent：规则路由 + 工具调用 + RAG 预留 + 安全边界。
- MCP 风格工具层：商品、百科、订单、商家、DataCenter 工具。
- 苗丰精灵：统一前端交互入口，承载官网/商城 AI 咨询体验。
- 数据闭环：咨询日志、工具调用、未解决问题、样本候选进入 DataCenter，为 RAG 优化和后续微调做准备。

一句话：

> 这是一个从业务系统、数据中台到智能客服 Agent 和前端精灵入口贯通的 AI 应用体系，而不是孤立的 AI 聊天 Demo。

## 9. 新负责人第一天行动清单

1. 阅读本文件。
2. 阅读 `MF_全项目相互调用与联调总览.md`。
3. 启动 MySQL 和 Redis。
4. 启动 MF_DataCenter，并运行 `check-health.ps1`。
5. 启动 MF_AgentService，并访问 `/actuator/health`。
6. 用 PowerShell 调一次 `/api/agent/chat`。
7. 打开 DataCenter，看 AI 数据是否写入。
8. 启动 MF_Pet demo，确认 runtime 可用。
9. 启动 MF_Website，规划接入 MF_Pet。
10. 记录当天发现的端口、配置、数据缺口。

## 10. 交接结论

当前 MF_Project 已经从“几个独立项目”进入“体系联调阶段”。

新负责人不应优先继续堆新功能，而应先把以下主链路打通：

```text
MF_Website -> MF_Pet -> MF_AgentService -> MF_EP / MF_DataCenter
```

打通这条链路后，项目就能完整展示：

- 官网品牌入口。
- 苗丰精灵互动。
- 智能客服 Agent。
- 业务工具调用。
- 数据中台沉淀。
- 后续 RAG/微调/多 Agent 扩展基础。

