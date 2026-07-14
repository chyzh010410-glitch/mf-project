# MF_AgentService 项目结构说明

更新时间：2026-07-13

## 1. 定位

`MF_AgentService` 是 MF 项目的独立 AI 客服 Agent 服务。

它替代原先 `MF_EP/projects/mf/mf-ai` 的职责。`MF_EP` 不再维护内置 Python AI 模块，只负责在前端展示聊天界面，并通过 HTTP 调用 `MF_AgentService`。

核心链路：

```text
MF_EP / MF_Website / MF_Pet
        ↓
MF_AgentService
        ↓
MF_EP 业务接口 + MF_DataCenter 数据沉淀接口
```

## 2. 当前项目路径

```text
F:\20260518-xiangmu\MF_Project\MF_AgentService
```

## 3. 当前实际结构

```text
MF_AgentService
├── pom.xml
├── README.md
├── agent-core/
├── mcp-server/
├── docs/
├── prompts/
└── src/
    ├── main/
    │   ├── java/com/mf/agentservice/
    │   │   ├── MfAgentServiceApplication.java
    │   │   ├── api/
    │   │   ├── agent/
    │   │   ├── client/
    │   │   ├── config/
    │   │   ├── rag/
    │   │   └── tools/
    │   └── resources/
    │       └── application.yml
    └── test/java/com/mf/agentservice/
```

说明：`agent-core`、`mcp-server` 当前主要作为边界说明目录保留；V1 代码先落在标准 Spring Boot `src/main/java` 结构中，避免过早拆成复杂多模块。

## 4. 包职责

| 路径 | 职责 |
| --- | --- |
| `api` | 对外 HTTP 接口，请求/响应 DTO，例如 `/api/agent/chat`、`/api/agent/chat/stream` |
| `agent` | Agent 编排、意图识别、会话记忆、LLM 网关 |
| `tools` | MCP 风格工具层，封装商品、百科、订单、商家、DataCenter 写入等工具 |
| `client` | 调用 `MF_EP` 和 `MF_DataCenter` 的 HTTP 客户端 |
| `rag` | 轻量知识检索、知识刷新、Embedding/RAG 相关封装 |
| `config` | 服务配置、外部地址、RestClient 等基础配置 |
| `prompts` | Agent 提示词文档 |
| `docs` | 架构、范围、安全边界说明 |
| `src/test` | 意图识别、客服编排、工具可靠性、订单安全边界等测试 |

## 5. 对 MF_EP 暴露的接口

本地默认服务地址：

```text
http://localhost:8092
```

健康检查：

```http
GET /actuator/health
```

普通聊天：

```http
POST /api/agent/chat
```

流式聊天：

```http
POST /api/agent/chat/stream
```

MF_EP 前端开发代理：

```text
/agent-api/api/agent/chat
/agent-api/api/agent/chat/stream
```

详细字段以接口文档为准：

```text
F:\20260518-xiangmu\MF_Project\Folder_description\MF_AgentService_MF_EP_AI客服对接接口文档.md
```

## 6. 与旧 mf-ai 的关系

旧目录：

```text
F:\20260518-xiangmu\MF_Project\MF_EP\projects\mf\mf-ai
```

当前状态：已删除。

后续要求：

1. 不再启动旧 Python AI 服务。
2. 不再维护旧 `/client/ai/chat` 作为 AI 客服主链路。
3. 不再把 DeepSeek Key、RAG、工具调用逻辑放在 MF_EP 前端或 MF_EP 后端。
4. AI 客服、意图识别、知识检索、工具调用和 DataCenter 日志沉淀统一放到 `MF_AgentService`。

## 7. 联调验收

建议负责人按顺序验收：

1. 启动 `MF_EP` 后端：`http://localhost:8080`。
2. 启动 `MF_DataCenter` 后端：`http://localhost:8091`。
3. 启动 `MF_AgentService`：`http://localhost:8092`。
4. 访问 `GET http://localhost:8092/actuator/health`。
5. 启动 `mf-frontend-client`，打开 `/ai` 页面。
6. 在 `/ai` 输入商品、百科、订单、商家入驻问题。
7. 确认请求走 `/agent-api/api/agent/chat`。
8. 确认 `MF_AgentService` 返回 `answer`、`intent`、`usedTools` 等字段。
9. 确认 DataCenter 可看到咨询日志和工具调用沉淀。

## 8. 负责人注意事项

- `MF_AgentService` 是独立服务，不属于 MF_EP 的 Maven 模块。
- 当前 V1 是单 Agent 服务，不是 Agent 集群。
- MCP 工具层当前在服务内部，不是独立 MCP Server。
- Agent 不直接连接业务数据库，只通过 MF_EP/DataCenter HTTP API 工作。
- Agent 不执行退款、改订单、确认收货、商家审核等高风险动作。
