# MF_AgentService 架构说明

## 模块化单体

```text
customer-agent-app
        |
        +--> agent-core ----------> agent-contract
        |
        +--> agent-integrations -> agent-core + agent-contract
```

- `agent-contract` 定义聊天 DTO、工具执行模型与外部能力端口。
- `agent-core` 负责客服编排、40 类契约路由、会话、回答与降级策略。
- `agent-integrations` 负责 MF_EP、MF_DataCenter、RAG、LLM 和内部工具适配。
- `customer-agent-app` 负责 HTTP/SSE、Spring Boot 配置与模块装配。

## 运行边界

- Agent 不直接访问 MF_EP 或 MF_DataCenter 数据库。
- 商品、订单、百科和商家事实只能来自受控工具调用。
- 退款、改订单、确认收货和商家审核只解释流程，不自动执行。
- 未解决问题、工具调用与可复用样本继续写入 DataCenter。

## 后续演进

商家助手和运营助手成熟后新增各自 `*-app` 启动模块，共用 contract、core 与 integrations；只有在独立发布、负载或权限隔离确有需要时才拆为独立服务或 MCP Gateway。
