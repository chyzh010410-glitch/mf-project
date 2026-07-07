# MF_AgentService 项目进度与接入说明

本文给项目负责人使用，用来了解 `MF_AgentService` 当前完成度、运行方式，以及它如何连接 `MF_Pet`、`MF_Website`、`MF_EP` 和 `MF_DataCenter`。

## 1. 项目定位

`MF_AgentService` 是苗丰平台的智能客服 Agent 服务层。

它位于用户入口和业务系统之间：

```text
MF_Pet / MF_Website / 商城前端
        ↓
MF_AgentService
        ↓
MF_EP + MF_DataCenter
```

当前 V1 的目标不是做复杂多 Agent 平台，而是先落地一个可运行、可接入、可沉淀数据的通用客服 Agent。

它负责：

- 接收用户自然语言问题。
- 判断用户意图。
- 调用商品、百科、订单、商家入驻等工具。
- 将咨询日志、工具调用日志、未解决问题、样本候选写入 `MF_DataCenter`。
- 对退款、改订单、确认收货、商家审核等高风险动作做安全拦截。

## 2. 当前完成进度

已完成：

- 已搭建独立 Spring Boot 服务。
- 已提供统一聊天接口：`POST /api/agent/chat`。
- 已实现规则型 Agent 编排层。
- 已实现 MCP 风格工具命名和工具调用记录。
- 已实现商品工具：`product.search`、`product.detail`。
- 已实现百科工具：`encyclopedia.search`。
- 已实现订单工具：`order.status`。
- 已实现商家入驻说明工具：`merchant.guide`。
- 已实现数据中台写入工具：
  - `datacenter.logConversation`
  - `datacenter.logToolCall`
  - `datacenter.reportUnresolved`
  - `datacenter.saveSampleCandidate`
- 已接入 Spring AI 依赖和 OpenAI API Key 配置。
- 已预留真实 LLM 调用开关。
- 已实现轻量 RAG 文档模型和本地知识检索。
- 已补充单元测试，覆盖意图识别、订单安全边界、工具调用和 DataCenter 日志。
- 已通过 `mvn test` 验证，当前测试结果为 6 个测试全部通过。

当前实现位置：

```text
F:\20260518-xiangmu\MF_Project\MF_AgentService
```

关键代码目录：

```text
MF_AgentService
├── src/main/java/com/mf/agentservice/api       对外 HTTP 接口
├── src/main/java/com/mf/agentservice/agent     Agent 编排与意图识别
├── src/main/java/com/mf/agentservice/tools     MCP 风格工具层
├── src/main/java/com/mf/agentservice/client    MF_EP / MF_DataCenter HTTP 客户端
├── src/main/java/com/mf/agentservice/rag       轻量知识检索
├── src/main/resources/application.yml          服务配置
└── src/test/java/com/mf/agentservice           测试
```

说明：

原先规划里的：

```text
agent-core
mcp-server
tools
prompts
```

目前没有拆成 Maven 多模块，而是先落到 Spring Boot 标准源码目录中：

- `agent-core` 对应 `src/main/java/com/mf/agentservice/agent`。
- `mcp-server / tools` 对应 `src/main/java/com/mf/agentservice/tools`。
- `prompts` 仍保留为提示词文档目录。

这样做是为了让 V1 先跑起来，避免一开始拆成复杂平台导致集成成本过高。后续工具数量变多后，可以再拆成多模块或独立 MCP Server。

## 3. 当前还没完成的部分

未完成或仅预留：

- 真实 LLM 目前默认关闭。
- Spring AI `ChatClient` 已接入，但只在 `MF_AGENT_LLM_ENABLED=true` 时用于直接回答类问题。
- 当前 Agent 决策主要是规则路由，不是完全由大模型自主选择工具。
- RAG 当前是轻量本地知识检索，不是生产级向量库。
- 还没有做流式 SSE 对话。
- 还没有做多轮上下文记忆。
- 还没有拆成独立 `MF_MCP_Server`。
- 还没有给前端做专门的聊天 UI，只提供后端接口。

负责人需要理解：当前项目已经具备“可运行、可被其他项目调用、可把数据写入 DataCenter”的 V1 能力，但还不是完整智能体平台。

## 4. 运行前依赖

`MF_AgentService` 默认依赖两个本地服务：

```text
MF_EP          http://localhost:8080
MF_DataCenter http://localhost:8091
```

用途：

- `MF_EP`：提供商品、百科、订单等业务查询接口。
- `MF_DataCenter`：接收 Agent 咨询日志、工具调用日志、未解决问题和样本候选。

如果只想启动 `MF_AgentService` 看接口是否能跑，可以先不启动依赖服务。

但如果要验证商品、百科、订单、日志沉淀闭环，需要同时启动：

```text
MF_EP
MF_DataCenter
MF_AgentService
```

## 5. 启动方式

进入项目目录：

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_AgentService
```

启动服务：

```powershell
mvn spring-boot:run
```

默认启动地址：

```text
http://localhost:8092
```

健康检查：

```text
GET http://localhost:8092/actuator/health
```

## 6. 配置说明

配置文件：

```text
F:\20260518-xiangmu\MF_Project\MF_AgentService\src\main\resources\application.yml
```

核心配置：

```yaml
server:
  port: 8092

mf:
  ep:
    base-url: ${MF_EP_BASE_URL:http://localhost:8080}
  datacenter:
    base-url: ${MF_DATACENTER_BASE_URL:http://localhost:8091}
  agent:
    llm-enabled: ${MF_AGENT_LLM_ENABLED:false}
    request-timeout: ${MF_AGENT_REQUEST_TIMEOUT:5s}
```

默认情况下：

- `MF_EP_BASE_URL` 不配置时使用 `http://localhost:8080`。
- `MF_DATACENTER_BASE_URL` 不配置时使用 `http://localhost:8091`。
- `MF_AGENT_LLM_ENABLED` 默认为 `false`，不会调用真实模型。

## 7. API Key 使用方式

项目已经预留 OpenAI API Key 配置。

配置项：

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:}
      chat:
        options:
          model: ${MF_AGENT_OPENAI_MODEL:gpt-4o-mini}
```

如果要打开真实模型调用，在启动前设置：

```powershell
$env:OPENAI_API_KEY="你的 OpenAI API Key"
$env:MF_AGENT_LLM_ENABLED="true"
$env:MF_AGENT_OPENAI_MODEL="gpt-4o-mini"

mvn spring-boot:run
```

当前策略：

- `MF_AGENT_LLM_ENABLED=false`：不调用模型，只走规则路由、工具调用、轻量 RAG。
- `MF_AGENT_LLM_ENABLED=true`：直接回答类问题会尝试调用 Spring AI `ChatClient`。
- 商品、百科、订单、商家入驻、DataCenter 日志仍优先走工具，不直接让模型编造业务数据。

后续升级方向：

- 让 LLM 成为核心编排器。
- 让 LLM 根据工具描述决定是否调用 `product.search`、`order.status`、`datacenter.*` 等工具。
- 引入更完整的 RAG 和多轮记忆。

## 8. 对外聊天接口

其他项目统一调用：

```http
POST http://localhost:8092/api/agent/chat
Content-Type: application/json
```

请求体：

```json
{
  "sessionId": "demo-session",
  "message": "推荐几款适合果树的肥料",
  "userId": "1001",
  "userType": "client",
  "authToken": ""
}
```

字段说明：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `sessionId` | 否 | 当前会话 ID，建议前端传入 |
| `message` | 是 | 用户问题 |
| `userId` | 否 | 当前用户 ID |
| `userType` | 否 | 用户类型，如 `client`、`merchant` |
| `authToken` | 否 | 用户登录 token，订单查询时必须传 |

响应示例：

```json
{
  "answer": "我已按你的问题查询了苗丰商品库...",
  "intent": "product",
  "resolved": true,
  "usedTools": [
    {
      "name": "product.search",
      "success": true,
      "durationMs": 12
    },
    {
      "name": "datacenter.logConversation",
      "success": true,
      "durationMs": 8
    }
  ],
  "conversationId": 42,
  "fallbackReason": null
}
```

响应字段说明：

| 字段 | 说明 |
| --- | --- |
| `answer` | Agent 返回给用户的回答 |
| `intent` | Agent 识别出的意图 |
| `resolved` | 当前问题是否被解决 |
| `usedTools` | 本轮调用过的工具 |
| `conversationId` | 写入 `MF_DataCenter` 后返回的咨询日志 ID |
| `fallbackReason` | 兜底原因，没有兜底时为 `null` |

## 9. PowerShell 调用示例

普通商品问题：

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8092/api/agent/chat" `
  -ContentType "application/json" `
  -Body '{"sessionId":"demo-session","message":"推荐几款适合果树的肥料","userId":"1001","userType":"client","authToken":""}'
```

百科/种植问题：

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8092/api/agent/chat" `
  -ContentType "application/json" `
  -Body '{"sessionId":"demo-session","message":"苹果树腐烂病怎么处理","userId":"1001","userType":"client","authToken":""}'
```

订单问题：

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8092/api/agent/chat" `
  -ContentType "application/json" `
  -Body '{"sessionId":"demo-session","message":"我的订单 123 发货了吗","userId":"1001","userType":"client","authToken":"Bearer xxx"}'
```

注意：

订单查询必须传 `authToken`。

如果不传 token，Agent 会返回登录提示，不会查询订单接口。

## 10. 其他项目怎么接入

### MF_Pet 接入方式

`MF_Pet` 可以把聊天面板里的用户输入发送到：

```text
POST http://localhost:8092/api/agent/chat
```

建议：

- `sessionId` 使用 MF_Pet 当前会话 ID。
- `message` 使用用户输入。
- 如果用户已登录商城，把登录 token 放到 `authToken`。
- 展示返回的 `answer`。
- 根据 `intent` 和 `usedTools` 显示“正在查询商品 / 正在查询百科 / 正在查询订单”等状态。

### MF_Website 接入方式

`MF_Website` 的“苗丰精灵”入口可以调用同一个接口。

建议：

- 未登录用户可以咨询商品、百科、商家入驻流程。
- 遇到订单问题时，引导用户登录。
- 登录后把商城 token 传给 `authToken`。

### MF_EP 接入方式

`MF_EP` 主要作为被调用方：

- 提供商品 public API。
- 提供百科 public API。
- 提供订单 client API。

如果后续要让商城内置 Agent 聊天窗口，商城前端也直接调用 `MF_AgentService`。

### MF_DataCenter 接入方式

`MF_DataCenter` 当前已经是数据沉淀方。

`MF_AgentService` 会写入：

- AI 咨询日志。
- Agent 工具调用日志。
- 未解决问题。
- 高质量样本候选。

负责人验证集成时，可以看 DataCenter 的 AI 分析页面是否出现新增咨询、工具调用和未解决问题。

## 11. 安全边界

当前项目明确不做：

- 不直接连业务数据库。
- 不自动退款。
- 不自动改订单。
- 不自动取消订单。
- 不自动确认收货。
- 不自动审核商家。
- 不替代人工做高风险业务决策。

订单查询边界：

- 有 `authToken`：透传给 `MF_EP` 查询当前用户自己的订单。
- 无 `authToken`：拒绝查询，只提示登录。

这是为了保证 Agent 只能做“查询、解释、沉淀”，不能越权操作业务。

## 12. 联调顺序建议

负责人可以按这个顺序安排联调：

1. 启动 `MF_DataCenter`，确认 `http://localhost:8091` 可访问。
2. 启动 `MF_EP`，确认 `http://localhost:8080` 可访问。
3. 启动 `MF_AgentService`，确认 `http://localhost:8092/actuator/health` 正常。
4. 用 PowerShell 调 `/api/agent/chat` 测商品问题。
5. 用 PowerShell 调 `/api/agent/chat` 测百科问题。
6. 不带 token 测订单问题，确认不会泄露订单。
7. 带 token 测订单问题，确认可以透传查询。
8. 打开 `MF_DataCenter` AI 分析页面，看咨询日志和工具调用是否沉淀。
9. 再让 `MF_Pet` 或 `MF_Website` 前端接入同一个聊天接口。

## 13. 推荐下一步

建议下一阶段按优先级推进：

1. 让 `MF_Pet` 聊天面板接入 `/api/agent/chat`。
2. 让 `MF_Website` 的“苗丰精灵”入口接入同一个接口。
3. 完成真实 API Key 环境配置，并开启 `MF_AGENT_LLM_ENABLED=true` 做模型联调。
4. 把当前轻量 RAG 替换为从 `MF_EP` 百科/FAQ/文章构建的知识索引。
5. 增加流式 SSE 输出，让前端有更自然的打字体验。
6. 当工具数量明显增加后，再把工具层拆成独立 `MF_MCP_Server`。

## 14. 当前验收结论

`MF_AgentService` 当前已经达到 V1 可接入状态：

- 可以独立启动。
- 可以被其他项目通过 HTTP 调用。
- 可以调用 `MF_EP` 查询业务信息。
- 可以向 `MF_DataCenter` 写入 Agent 数据。
- 具备基础安全边界。
- 具备后续接入真实 LLM 的配置入口。

当前还不应被描述为完整 Agent 平台，更准确的说法是：

```text
MF_AgentService 已完成智能客服 Agent V1 服务骨架和首批工具链路，
可以开始和 MF_Pet / MF_Website / MF_EP / MF_DataCenter 做端到端联调。
```
