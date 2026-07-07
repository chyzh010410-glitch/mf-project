# MF_AgentService

苗丰智能服务层 V1。

## 定位

`MF_AgentService` 是苗丰智能服务体系的第一阶段项目，负责实现一个通用客服 Agent，并在项目内部实现 MCP 工具层。

当前阶段 MCP 不单独作为顶级项目，而是作为 `MF_AgentService` 内部的工具协议层。等后续扩展到多个 Agent 时，再考虑将 MCP 抽离为独立 `MF_MCP_Server`。

## 架构关系

```text
用户 / 苗丰精灵
      ↓
MF_AgentService
      ├─ customer-service-agent
      └─ mcp-server / tools
              ↓
MF_EP 业务系统
商品、百科、订单、用户、商家
              ↓
MF_DataCenter
咨询日志、未解决问题、样本池、分析结果
```

## V1 范围

V1 只做一个通用客服 Agent：

- 支持用户自然语言咨询。
- 能回答基础种植、商品、百科、订单、商家入驻相关问题。
- 通过 MCP 工具查询商品、百科、订单等业务能力。
- 将咨询日志、未解决问题和高质量样本写入数据中台。
- 支持失败兜底和人工处理提示。

## 暂不做

- Agent 集群。
- 商品推荐 Agent。
- 商家助手 Agent。
- 运营分析 Agent。
- 内容生成 Agent。
- 复杂多 Agent 协同。
- 模型微调训练平台。

## 建议结构

```text
MF_AgentService
├── agent-core
├── mcp-server
├── tools
├── prompts
├── docs
└── README.md
```

## 后续演进

第一阶段：

- 通用客服 Agent + 内部 MCP 工具层。

第二阶段：

- 扩展更多工具：商品推荐、商家经营、运营分析。

第三阶段：

- 多 Agent 集群。
- MCP 抽离为独立工具网关。

## 当前 V1 实现

`MF_AgentService` 已落地为独立 Spring Boot 服务：

- Java 17 + Spring Boot 3.4.x。
- Spring AI 1.0.x 作为后续 LLM/Tool Calling 接入口。
- 默认端口：`8092`。
- 对外接口：`POST /api/agent/chat`。
- 默认依赖：
  - `MF_EP_BASE_URL=http://localhost:8080`
  - `MF_DATACENTER_BASE_URL=http://localhost:8091`

### 启动

```powershell
mvn spring-boot:run
```

如果需要接入真实模型：

```powershell
$env:OPENAI_API_KEY="your-api-key"
$env:MF_AGENT_LLM_ENABLED="true"
mvn spring-boot:run
```

### 聊天接口

```http
POST /api/agent/chat
Content-Type: application/json

{
  "sessionId": "demo-session",
  "message": "推荐几款适合果树的肥料",
  "userId": "1001",
  "userType": "client",
  "authToken": ""
}
```

响应包含：

- `answer`：Agent 回答。
- `intent`：识别出的意图。
- `resolved`：是否已解决。
- `usedTools`：本轮调用过的 MCP 风格工具。
- `conversationId`：写入 MF_DataCenter 后返回的咨询日志 ID。
- `fallbackReason`：兜底原因。

### V1 工具清单

- `product.search`
- `product.detail`
- `encyclopedia.search`
- `order.status`
- `merchant.guide`
- `datacenter.logConversation`
- `datacenter.logToolCall`
- `datacenter.reportUnresolved`
- `datacenter.saveSampleCandidate`

### 安全边界

V1 只做查询、解释和数据沉淀：

- 不直接访问 MF_EP 或 MF_DataCenter 数据库。
- 不自动退款。
- 不自动改订单。
- 不自动确认收货。
- 不自动审核商家。
- 订单查询必须携带用户 `Authorization` token。
