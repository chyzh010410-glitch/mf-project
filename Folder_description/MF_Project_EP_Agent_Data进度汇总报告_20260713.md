# MF Project 三核心项目进度汇总报告

更新时间：2026-07-14  
汇总范围：`MF_EP`、`MF_AgentService`、`MF_DataCenter`  
适用对象：总负责人、三项目负责人、后续联调负责人、接手 Agent

## 1. 汇总结论

`MF_EP`、`MF_AgentService`、`MF_DataCenter` 三个核心项目已经具备 V1 联调基础：

```text
MF_EP 提供正式业务内容和电商事实源
MF_AgentService 提供智能客服编排、业务工具调用和 AI 数据留存
MF_DataCenter 提供 AI 运营、知识治理、样本审核和草稿编排
```

当前整体状态可判断为：

| 维度 | 当前判断 |
| --- | --- |
| 主业务系统可用性 | 已具备，MF_EP 商品、百科、订单、用户、商家和三端页面可运行。 |
| 客服 Agent V1 接入 | 已具备，聊天接口、意图路由、业务查询、安全边界和数据留存可用。 |
| 数据中台 AI 治理 | 已具备，AI 会话、工具日志、未解决问题、样本候选和知识工作台可运行。 |
| 三项目完整闭环 | P0 主链路已完成本地真实联调验证，发布事件、Agent 自动消费、ack、下线失效和 DataCenter 审计均已通过。 |
| 生产上线准备 | 未完成，仍需权限、审计、配置托管、失败重试、回滚和自动化契约测试。 |

一句话结论：

> 三项目已经从“能启动、能调用”推进到“P0 受控知识闭环本地验收通过”的阶段；下一步重点是把这条链路固化为可重复自动化验收和可生产运行机制。

## 2. 各项目当前完成度

### 2.1 MF_EP

项目路径：

```text
F:\20260518-xiangmu\MF_Project\MF_EP\projects\mf
```

当前定位：

`MF_EP` 是苗丰项目的主业务系统，负责商品、百科、文章、FAQ、订单、用户、商家和三端前端。它是正式业务内容的事实源。

完成度口径：

| 范围 | 当前完成度 | 判断 |
| --- | --- | --- |
| 电商主业务后端与三端页面 | 约 80% | 既有业务可运行，本轮未做高风险业务重构。 |
| 管理端与三端视觉统一 | 约 85% | 管理端已对齐 DataCenter 风格，客户端和商家端已接入品牌化登录与官方 Logo。 |
| AI 客服接入与会话体验 | 约 75% | 客户端已接入 AgentService，支持会话、本地缓存、历史拉取、流式接口和安全边界。 |
| AI 知识补全发布落点 | 约 70% | 已支持草稿创建、更新、发布、下线和发布/下线事件；版本回滚、人工复核仍需完善。 |
| EP、DataCenter、AgentService 知识闭环 | 约 70% | DataCenter 候选到 MF_EP 草稿、发布事件、Agent 自动消费、ack、下线失效已完成本地真实验收；自动化、重试和生产化仍需补齐。 |

已完成重点：

- 后端 API 默认端口 `8080`。
- 管理端 `5173`、客户端 `5174`、商家端 `5175`。
- 客户端 `/ai` 已通过代理调用 `MF_AgentService` 的 `/api/agent/chat`。
- 已删除废弃的原 `mf-ai` 模块，不再作为启动和部署范围。
- 已新增 AI 内容草稿接口，支持 `faq`、`article`、`encyclopedia`。
- 无登录凭据的订单查询不会泄露订单、物流或售后详情。
- 退款、改订单、确认收货等高风险动作不会由 Agent 自动执行。

主要缺口：

- MF_EP 发布或下线内容后，已通过 `ai_content_sync_event` 事件表触发 AgentService 轮询消费和 ack；仍需补自动化验收、失败重试和生产配置固化。
- 草稿有版本字段，但缺少版本快照、回滚记录和回滚接口。
- 高风险内容缺少完整人工审核状态机。
- 缺少覆盖 `DataCenter -> MF_EP 发布 -> AgentService 同步 -> 用户提问命中 -> 下线失效` 的自动化端到端测试。

### 2.2 MF_AgentService

项目路径：

```text
F:\20260518-xiangmu\MF_Project\MF_AgentService
```

当前定位：

`MF_AgentService` 是 Spring Boot 客服 Agent 服务层，负责意图识别、工具调用、回答生成、数据留存和安全降级。当前不是完整多 Agent 平台。

完成度口径：

| 口径 | 当前完成度 | 判断 |
| --- | --- | --- |
| V1 集成与业务闭环 | 约 85% | 聊天接口、意图路由、业务工具、数据留存、安全边界和测试均已具备。 |
| 生产级智能客服 | 约 55% | 真实 LLM、向量 RAG、SSE、多轮记忆、观测和生产部署仍待完善。 |

已完成重点：

- 已提供统一接口 `POST /api/agent/chat`。
- 服务端口为 `8092`。
- 已支持商品查询、商品详情、百科查询、订单状态、商家入驻说明等工具能力。
- 已具备轻量知识检索与知识缺口识别能力。
- 已将每轮有效会话沉淀到 DataCenter：会话日志、工具调用日志、未解决问题或优质回答样本。
- 已接入 Spring AI 配置，真实 LLM 由 `MF_AGENT_LLM_ENABLED` 控制，默认关闭。
- 已预留 Qdrant 与嵌入服务配置。
- `mvn test` 已通过，测试结果为 `Tests run: 22, Failures: 0, Errors: 0, Skipped: 0`。

主要缺口：

- 优质回答进入正式知识库的治理闭环依赖 DataCenter 和 MF_EP，AgentService 本身不直接发布正式知识。
- 真实 LLM 运行尚未在安全环境完整验证。
- 生产级 RAG 仍缺知识导入、向量化、召回质量评估和更新机制。
- SSE、多轮记忆、监控、告警、链路追踪和生产部署脚本仍需补齐。

### 2.3 MF_DataCenter

项目路径：

```text
F:\20260518-xiangmu\MF_Project\MF_DataCenter
```

当前定位：

`MF_DataCenter` 已从基础运营后台推进为 AI 运营与知识治理工作台，负责会话沉淀、问题池、样本池、知识缺口、内容候选、草稿编排和发布审计。

完成度口径：

| 口径 | 当前完成度 | 判断 |
| --- | --- | --- |
| DataCenter 本体 | 约 82% | 运营观察、数据治理、AI 会话、问题/样本运营、知识工作台和 MF_EP 草稿对接已具备可运行能力。 |
| 三项目受控联调 | 约 80% | Agent 到 EP、Agent 到 DataCenter、DataCenter 到 EP 发布/下线事件链路均已完成真实闭环验证。 |
| 生产上线准备 | 约 55% | 回滚、自动化契约测试、权限、失败重试和生产配置演练仍未完成。 |

已完成重点：

- 已具备数据源契约、字段契约、快照、质量问题治理、指标字典和运营总览。
- AI 会话日志、工具调用日志、未解决问题池、样本候选池已可运行。
- 用户会话历史接口支持按用户和会话查询、软删除、鉴权与审计。
- 知识工作台可从未解决问题池聚合知识缺口。
- 审核通过且推荐入库的优质样本可创建知识缺口和内容候选。
- 内容候选可创建 MF_EP 内部草稿；低风险候选可调用发布/下线接口；高风险候选禁止直接发布。
- `source_sample_id` 唯一索引可防止同一优质回答重复沉淀。
- 后端测试 5 项通过，前端生产构建通过。

主要缺口：

- `DataCenter -> MF_EP` 草稿创建、发布、下线已完成真实联调验证。
- 发布后 AgentService 增量同步已通过事件轮询和 ack 机制完成本地验证。
- 发布后回滚未完成。
- 三方自动化契约测试仍需补齐。
- 生产环境内部令牌托管、权限最小化、告警和失败重试策略仍需完善。

## 3. 三项目联调链路状态

| 链路 | 当前状态 | 说明 |
| --- | --- | --- |
| MF_EP 客户端 -> MF_AgentService | 已打通 | 客户端开发服务 `5174` 通过代理调用 AgentService。 |
| MF_AgentService -> MF_EP | 已打通 | Agent 通过 HTTP 查询商品、百科、文章、FAQ 和订单，不直接连接 EP 数据库。 |
| MF_AgentService -> MF_DataCenter | 已打通 | 会话、工具调用、未解决问题和样本候选可沉淀。 |
| DataCenter 样本 -> 知识候选 | 已打通 | 审核通过样本可创建知识缺口与内容候选，重复提交可复用已有候选。 |
| DataCenter -> MF_EP 创建草稿 | 已验证 | DataCenter 候选 `5` 已创建 MF_EP 草稿 `4`。 |
| DataCenter -> MF_EP 发布/下线 | 已验证 | 正式 FAQ `27` 已完成发布和最终下线，当前 `is_published=0`。 |
| MF_EP 发布 -> AgentService 增量同步 | 已验证 | MF_EP 事件 `7` 为 publish，AgentService 自动消费并 ack。 |
| 下线后 AgentService 失效确认 | 已验证 | MF_EP 事件 `8` 为 offline，AgentService 自动消费并 ack，关键词不再命中该 FAQ。 |
| 内容版本回滚 | 未完成 | MF_EP 尚无完整版本历史和回滚接口。 |

当前关键判断：

> 三项目之间的 P0 受控发布、同步、检索、下线、失效链路已经完成本地真实验证；剩余重点是回滚、自动化、失败重试和生产化。

## 4. 当前验证情况

已知验证结果：

| 项目 | 验证项 | 结果 |
| --- | --- | --- |
| MF_EP | `mvn -pl fertilizer-api -am test -DskipTests` | 已通过。 |
| MF_EP | `mf-frontend` 与 `mf-frontend-client` 构建 | 已通过，仅有非阻断 Vite 警告。 |
| MF_EP | 客户端代理到 AgentService | `http://localhost:5174/agent-api/api/agent/chat` 返回 `200`。 |
| MF_AgentService | `mvn test` | 22 个测试通过。 |
| MF_AgentService | 聊天 HTTP 接口 | 问候、帮助、养护、商品、订单、商家、高风险和模糊提问均有结构化响应。 |
| MF_AgentService | 数据中台写入 | 会话、工具调用、未解决问题或样本候选可写入。 |
| MF_DataCenter | `datacenter-api mvn test` | 后端测试 5 项通过。 |
| MF_DataCenter | `datacenter-web npm run build` | 前端生产构建通过。 |
| 三项目联调 | AgentService -> EP、AgentService -> DataCenter | 真实咨询可识别百科意图、调用工具并沉淀样本。 |
| 三项目联调 | DataCenter 候选 -> MF_EP 草稿 -> publish/offline 事件 -> AgentService ack | 2026-07-14 已验证，候选 `5`、草稿 `4`、FAQ `27`、事件 `7/8` 均已形成审计记录。 |

仍需补充的验证：

- 将 P0 受控发布闭环沉淀为可重复执行的自动化验收。
- 版本回滚和审核审计记录。
- 同步失败后的重试、告警和治理待办。

## 5. 当前状态与剩余阻塞

### P0：正式受控发布闭环已本地验收通过

2026-07-14 已完成并记录以下链路：

```text
优质样本审核通过
-> DataCenter 创建内容候选
-> DataCenter 创建 MF_EP 草稿
-> MF_EP 发布正式 FAQ / 文章 / 百科
-> AgentService 增量同步或重新索引
-> 用户提问命中新内容
-> DataCenter 下线
-> AgentService 确认不再命中
```

已记录证据：

- DataCenter 候选 `5`。
- MF_EP 草稿 `4`。
- 正式 FAQ `27`。
- 发布事件 `7`，状态 `acknowledged`，由 `mf-agent-service` 于 `08:13:07` 确认。
- 下线事件 `8`，状态 `acknowledged`，由 `mf-agent-service` 于 `08:13:37` 确认。
- 草稿 `4` 当前为 `offline`，正式 FAQ `27` 的 `is_published=0`。

因此，三项目状态可从“可联调”提升为“P0 受控闭环本地验收通过”。

### P1：事件同步机制需要生产固化

当前已明确正常链路：

```text
MF_EP ai_content_sync_event
-> AgentService 轮询消费
-> 刷新知识索引
-> AgentService 回写 ack
-> DataCenter 记录同步状态
```

`POST /api/agent/knowledge/internal-sync` 不再作为正常发布/下线路径，只保留为故障重试工具。

仍需固化：

- `MF_EP_INTERNAL_TOKEN` 的环境配置和生产密钥托管。
- 事件轮询间隔、超时、重试次数和失败状态。
- pending / failed / acknowledged 的治理页面或通知。
- 同步失败后的手工重试入口。
- 同步事件表的保留、清理和审计策略。

### P1：版本回滚与人工复核不完整

当前草稿和发布能力已经存在，但生产级治理还缺：

- 版本快照。
- 按版本恢复。
- 回滚记录。
- 审核人、审核意见、审核时间。
- 高风险内容人工复核状态机。
- 发布、下线、回滚全链路审计。

## 6. 推荐下一步顺序

### 第一步：固化 P0 受控发布闭环自动化验收

责任建议：

| 事项 | 主负责人 | 协作方 |
| --- | --- | --- |
| 将候选 `5` 路径沉淀为自动化用例 | DataCenter | MF_EP、AgentService |
| 准备隔离低风险 FAQ 测试数据 | DataCenter | MF_EP |
| 自动创建草稿、发布、等待 ack | DataCenter | MF_EP、AgentService |
| 自动验证 Agent 命中和下线失效 | AgentService | DataCenter |
| 自动清理测试数据或标记为受控样本 | DataCenter | MF_EP |

验收标准：

```text
发布后 Agent 可检索
下线后 Agent 不再命中
DataCenter 可追踪样本、候选、草稿、正式内容和同步状态
测试可重复执行，不依赖人工点击和临时数据库操作
```

### 第二步：生产固化事件同步机制

建议沿用已验证的事件表轮询路径，不先拆独立 MCP Server，也不先做复杂多 Agent 平台。

建议能力：

- `MF_EP_INTERNAL_TOKEN` 环境配置规范。
- AgentService 事件轮询参数配置。
- 事件消费幂等。
- pending 超时治理任务。
- failed 重试与失败原因记录。
- `internal-sync` 仅作为故障重试工具保留。

### 第三步：补 MF_EP 版本回滚和人工复核

建议能力：

- 内容版本快照。
- 回滚接口。
- 回滚审计。
- 高风险内容审核状态。
- 审核人、审核意见、审核时间和授权凭据。

### 第四步：补三方自动化契约测试

建议覆盖：

- AgentService 写会话、工具调用、未解决问题、样本候选。
- DataCenter 调用 MF_EP 创建草稿、发布、下线。
- MF_EP 发布后 AgentService 检索命中。
- 下线后 AgentService 不再命中。
- 高风险内容禁止直接发布。
- 同步失败重试和状态回写。

### 第五步：再做真实 LLM、RAG 和生产增强

在闭环稳定后再推进：

- `MF_AGENT_LLM_ENABLED=true` 的真实模型验证。
- DeepSeek 或 OpenAI API Key 的安全托管。
- Qdrant 知识导入、向量化和召回评估。
- SSE、多轮记忆、监控、告警和生产部署脚本。

## 7. 责任边界

必须继续保持以下边界：

| 项目 | 负责 | 不负责 |
| --- | --- | --- |
| MF_EP | 正式 FAQ、百科、文章、电商业务、发布/下线/回滚执行 | 不承担数据中台治理，不让外部绕过它写正式内容。 |
| MF_AgentService | 意图识别、工具调用、回答生成、数据留存、检索同步 | 不直接操作数据库，不直接发布正式知识，不执行高风险业务动作。 |
| MF_DataCenter | 审核、治理、知识缺口、样本运营、草稿编排、发布审计 | 不直接修改 `fertilizer` 业务表，不替代 MF_EP 成为事实源。 |

关键原则：

```text
DataCenter 负责治理与编排
MF_EP 负责正式内容事实源
AgentService 负责检索、回答和同步消费
```

## 8. 联调验收建议

启动建议：

```powershell
cd F:\20260518-xiangmu\MF_Project
.\scripts\start-mf-closed-loop.ps1 -WithWebsite -WithPet -WithEpFrontends
```

端口检查：

```powershell
.\scripts\check-mf-ports.ps1
```

关键端口：

| 服务 | 端口 |
| --- | --- |
| MF_EP 后端 | `8080` |
| MF_EP 管理端 | `5173` |
| MF_EP 客户端 | `5174` |
| MF_EP 商家端 | `5175` |
| MF_DataCenter API | `8091` |
| MF_DataCenter Web | `5176` |
| MF_AgentService | `8092` |

联调验收时，不应只看服务是否启动。必须至少检查：

- AgentService 是否能返回结构化回答。
- AgentService 是否真实调用 MF_EP 工具。
- DataCenter 是否记录会话和工具调用。
- 优质回答是否能进入样本候选。
- 样本是否能生成知识候选和 MF_EP 草稿。
- 发布后 Agent 是否命中新内容。
- 下线后 Agent 是否不再命中。

## 9. 总负责人判断

当前三项目不是从零开始补功能，而是进入“把 P0 已验收链路变成可自动化、可回滚、可生产运行机制”的阶段。

下一阶段最重要的交付物不是新页面，也不是更复杂的 Agent 架构，而是把这条已经跑通的正式链路变成可重复、可审计、可失败恢复的工程能力：

```text
客服回答沉淀
-> 人工治理审核
-> 正式内容发布
-> Agent 知识同步
-> 用户问题命中
-> 下线与失效验证
-> 审计可追踪
```

这条链路自动化稳定后，再推进真实 LLM、生产级 RAG、DataCenter 权限、生产部署和多 Agent 拆分。
