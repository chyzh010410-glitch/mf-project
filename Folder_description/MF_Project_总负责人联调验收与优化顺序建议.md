# MF Project 总负责人联调验收与优化顺序建议

更新时间：2026-07-07  
角色声明：MF_Project 总负责人 / 联调负责人

## 1. 总负责人说明

我是当前 `MF_Project` 的总负责人，负责统筹以下项目的边界、端口、启动、联调和下一阶段优化顺序：

```text
MF_EP
MF_DataCenter
MF_AgentService
MF_Pet
MF_Website
MF_Logo
```

当前项目已经不再是几个互相独立的 demo，而是进入了完整体系联调阶段。

核心产品定位是：

> MF 是一个以苗木肥料垂直电商为业务底座，融合品牌官网、苗丰精灵、智能客服 Agent、数据中台和后续 RAG / 工具调用能力的 AI 应用型电商平台。

## 2. 当前联调验收结论

截至本次整理，以下本地联调端口已经完成有序固定，并可通过总控脚本统一启动：

| 项目 | 服务 | 端口 | 当前结论 |
| --- | --- | --- | --- |
| `MF_EP` | 后端 API | `8080` | 已可启动，作为业务底座 |
| `MF_EP` | 管理端 | `5173` | 已固定端口 |
| `MF_EP` | 客户端 | `5174` | 已固定端口 |
| `MF_EP` | 商家端 | `5175` | 已固定端口 |
| `MF_DataCenter` | 后端 API | `8091` | 已可启动，系统状态接口正常 |
| `MF_DataCenter` | 前端 Web | `5176` | 已可访问 |
| `MF_Website` | 官网 | `5178` | 已从 `5173` 调整到独立端口，避免和管理端冲突 |
| `MF_Pet` | Web Demo | `5179` | 已可访问 |
| `MF_AgentService` | Agent API | `8092` | 已可启动，健康检查正常 |

统一启动命令：

```powershell
cd F:\20260518-xiangmu\MF_Project
.\scripts\start-mf-closed-loop.ps1 -WithWebsite -WithPet -WithEpFrontends
```

统一端口检查命令：

```powershell
.\scripts\check-mf-ports.ps1
```

统一停止命令：

```powershell
.\scripts\stop-mf-local.ps1
```

## 3. 已打通的关键闭环

当前已完成最小可用闭环：

```text
MF_Website / MF_Pet
  -> MF_AgentService /api/agent/chat
  -> MF_EP 商品等业务能力
  -> MF_DataCenter 咨询日志沉淀
```

已验证行为：

- 商品咨询可以进入 `MF_AgentService`。
- Agent 可以调用 `product.search` 查询 MF_EP 业务能力。
- Agent 可以调用 `datacenter.logConversation` 写入 MF_DataCenter。
- DataCenter 可以看到 AI 咨询沉淀。
- 无 token 查询订单时，不直接泄露订单隐私，只提示需要登录或补充订单信息。

这说明当前体系已经具备演示基础：

```text
业务底座可查
Agent 服务可答
数据中台可沉淀
官网和苗丰精灵入口可继续接入
```

## 4. 总体完善顺序建议

下一阶段不要优先大改底层业务逻辑，也不要一开始就扩展很多新功能。

建议按以下顺序完善：

```text
第一优先级：MF_Pet
第二优先级：MF_Website
第三优先级：MF_AgentService
第四优先级：MF_DataCenter
第五优先级：MF_EP
```

理由：

- `MF_EP` 和 `MF_DataCenter` 已经具备可运行基础，当前更需要稳定。
- 当前最缺的是用户能感受到的完整体验。
- 把 `MF_Website -> MF_Pet -> MF_AgentService -> MF_EP / MF_DataCenter` 串顺后，项目展示效果提升最大。
- 智能能力和数据沉淀已经具备雏形，应该先形成可演示产品闭环，再逐步增强。

## 5. 各项目优化建议

### 5.1 MF_Pet

定位：

```text
苗丰精灵前端交互层
```

下一步目标：

让苗丰精灵从单独 demo 变成真实可用的 AI 咨询入口。

建议优化：

- 接入 `MF_AgentService /api/agent/chat`。
- 聊天面板支持输入问题、发送、loading、展示回答。
- 根据 Agent 返回结果切换状态：
  - `thinking`
  - `working`
  - `success`
  - `error`
  - `doubt`
  - `idle`
- 网络失败、Agent 失败、后端未启动时提供友好提示。
- 保持职责边界：MF_Pet 不直接查数据库，不直接访问 MF_EP 业务接口。
- 后续替换 clawd GIF 占位素材，形成正式 MF 视觉资产。

验收标准：

- 用户能在 Pet 面板里输入问题并看到回答。
- 商品问题能触发 Agent 调用。
- 失败时 Pet 状态和提示清晰。
- DataCenter 能看到对应咨询记录。

### 5.2 MF_Website

定位：

```text
品牌官网和苗丰精灵入口
```

下一步目标：

让官网成为完整体验入口，而不是单纯展示页。

建议优化：

- 导航中的“苗丰精灵”按钮接入 MF_Pet runtime。
- 官网页面能唤出 Pet 聊天面板。
- 官网入口承接以下方向：
  - 苗木商城
  - 树木百科
  - 商家入驻
  - 智能咨询
- 保持官网的生态品牌调性，但不要脱离主业务。
- 不复制 MF_Pet runtime 源码，应通过组件或接入 API 使用。

验收标准：

- 打开官网后，用户能找到“苗丰精灵”入口。
- 点击入口能唤出 Pet。
- Pet 能在官网上下文中调用 Agent。
- 官网仍保持品牌入口定位，不变成普通环保展示页。

### 5.3 MF_AgentService

定位：

```text
智能客服 Agent 和工具调用层
```

下一步目标：

让 Agent 回答更自然、更贴近苗丰业务，并保持安全边界。

建议优化：

- 优化意图识别：
  - 商品推荐
  - 树木百科
  - 商家入驻
  - 订单查询
  - 未解决问题
- 优化回答文案，让它像“苗丰智能客服”，而不是接口测试返回。
- 保持真实 LLM 默认关闭，开启前必须明确环境变量和成本边界。
- 继续保持高风险动作禁止：
  - 不退款
  - 不改订单
  - 不取消订单
  - 不确认收货
  - 不审核商家
- 无 token 时不返回订单隐私。
- 后续再将 RAG 数据源从本地轻量知识扩展到 MF_EP 百科、FAQ、文章。

验收标准：

- 商品、百科、商家入驻问题能给出明确回答。
- 订单类问题在无 token 时不泄露隐私。
- 每次咨询都能按规则写入 DataCenter。
- `usedTools` 能清楚反映实际工具调用。

### 5.4 MF_DataCenter

定位：

```text
数据中台、治理观察台、AI 数据沉淀池
```

下一步目标：

让 DataCenter 成为联调和运营观察的可信后台。

建议优化：

- 优化 AI 咨询分析页面。
- 优化问题池和样本池展示。
- 处理 `GovernanceStatus = risk` 中的快照过期问题。
- 保持源表契约、数据质量、指标治理能力稳定。
- 后续补基础登录权限，避免直接暴露公网。
- 增加更清楚的“最近一次 Agent 写入”可视化。

验收标准：

- `/api/system/status` 正常。
- `/api/dashboard/overview` 正常。
- 源表失败数为 `0`。
- Agent 咨询记录、问题池、样本池可追踪。
- 治理状态风险原因能被用户看懂。

### 5.5 MF_EP

定位：

```text
主业务系统和交易闭环底座
```

下一步目标：

保持稳定，给 Agent 和 DataCenter 提供可靠业务能力。

建议优化：

- 稳定商品、百科、订单、商家、用户等核心接口。
- 保持 `fertilizer` 是 MF_EP 自有业务库。
- 不允许 DataCenter 反向修改业务表。
- 不建议此阶段重构订单、支付、商家审核、权限等高风险逻辑。
- 可以继续优化三端前端体验：
  - 管理端
  - 客户端
  - 商家端
- 保持官方 logo 和统一登录页风格。

验收标准：

- 后端 `8080` 可启动。
- 管理端、客户端、商家端固定端口可访问。
- `admin / admin123` 在 Redis 和后端正常时可登录。
- Agent 能稳定查询商品等低风险业务能力。

### 5.6 MF_Logo

定位：

```text
统一品牌资产来源
```

建议优化：

- 保持 `MF_Logo` 目录为官方 logo 源。
- 各前端项目复制 logo 到自己的 `src/assets/brand` 目录。
- 不跨项目直接引用 logo 源路径。
- 后续 MF_Pet 正式素材也应和品牌视觉保持一致。

## 6. 项目层级定义

建议后续所有负责人按以下层级理解 MF_Project：

```text
第 0 层：基础运行层
端口、启动脚本、MySQL、Redis、健康检查

第 1 层：业务底座层
MF_EP：商品、百科、订单、用户、商家

第 2 层：数据治理层
MF_DataCenter：指标、质量、日志、问题池、样本池

第 3 层：智能编排层
MF_AgentService：意图识别、工具调用、回答生成、日志沉淀

第 4 层：交互角色层
MF_Pet：苗丰精灵、聊天面板、状态反馈

第 5 层：品牌入口层
MF_Website：官网、苗丰精灵入口、商城/百科/商家入口
```

优化时应从低层稳定性和高层体验之间取得平衡：

- 底层不随便大改。
- 高层优先打磨体验。
- Agent 层连接业务和数据。
- DataCenter 负责观察和沉淀。

## 7. 下一阶段推荐执行计划

### 阶段一：Pet 接入 Agent

目标：

```text
MF_Pet -> MF_AgentService -> MF_DataCenter
```

完成内容：

- Pet 聊天面板调用 `/api/agent/chat`。
- 展示 Agent 回答。
- 显示 loading / success / error。
- DataCenter 看到咨询记录。

### 阶段二：Website 接入 Pet

目标：

```text
MF_Website -> MF_Pet -> MF_AgentService
```

完成内容：

- 官网导航唤出 Pet。
- 官网上下文中可以进行智能咨询。
- 保持官网品牌体验。

### 阶段三：Agent 回答质量优化

目标：

```text
更像真实苗丰智能客服
```

完成内容：

- 优化商品推荐话术。
- 优化百科回答。
- 优化商家入驻说明。
- 订单安全边界继续保持。

### 阶段四：DataCenter 观察台增强

目标：

```text
让联调结果看得见、讲得清
```

完成内容：

- AI 咨询分析页面增强。
- 问题池和样本池增强。
- 治理风险原因清晰化。
- 处理快照过期导致的 `risk`。

### 阶段五：MF_EP 三端体验整理

目标：

```text
业务底座稳定、三端体验统一
```

完成内容：

- 管理端、客户端、商家端体验打磨。
- 保持品牌一致。
- 不做高风险业务重构。

## 8. 总负责人最终建议

下一步最应该做的是：

```text
先完善 MF_Pet 接 AgentService，
再把 MF_Pet 接进 MF_Website，
然后优化 Agent 回答质量和 DataCenter 观察台。
```

不要先拆 `MF_MCP_Server`，不要先大改 `MF_EP` 订单支付权限，也不要把 DataCenter 当成 MF_EP 管理后台替代品。

当前最有价值的目标是把这条链路做成可演示、可体验、可沉淀的数据闭环：

```text
官网入口
  -> 苗丰精灵
  -> 智能客服 Agent
  -> 业务能力调用
  -> 数据中台沉淀
```

这条链路完善后，MF_Project 才真正从“多个能运行的项目”变成“一个完整的 AI 应用型电商系统”。
