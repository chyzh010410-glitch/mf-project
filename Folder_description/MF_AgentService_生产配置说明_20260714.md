# MF_AgentService 生产配置说明

更新时间：2026-07-14  
适用负责人：MF_AgentService 负责人、总负责人/部署负责人

## 1. 配置边界

`MF_AgentService` 的生产 Profile 位于：

```text
MF_AgentService/src/main/resources/application-prod.yml
```

该文件只引用环境变量，不包含任何真实密钥、内部令牌或数据库凭据。密钥由总负责人统一生成和分发，服务负责人只在本服务的部署环境中配置。

## 2. AgentService 需要的环境变量

```text
SPRING_PROFILES_ACTIVE=prod
MF_EP_BASE_URL=<MF_EP 地址>
MF_DATACENTER_BASE_URL=<MF_DataCenter 地址>
MF_EP_INTERNAL_TOKEN=<总负责人分发的 MF_EP 内部令牌>
MF_AGENT_KNOWLEDGE_SYNC_KEY=<AgentService 人工同步/重试内部令牌>
MF_AGENT_KNOWLEDGE_SYNC_POLL_INTERVAL=30s
MF_AGENT_KNOWLEDGE_SYNC_MAX_RETRIES=5
MF_AGENT_KNOWLEDGE_SYNC_PENDING_TIMEOUT=5m
```

真实模型仅由 AgentService 使用：

```text
MF_AGENT_LLM_ENABLED=true
DEEPSEEK_API_KEY=<仅配置在 AgentService 部署环境>
```

在轮换后的真实模型 Key 尚未注入前，必须设置 `MF_AGENT_LLM_ENABLED=false`，以保持事件同步和受控工具路由可运行；收到新 Key 后，再将模型开关与 Key 一起启用。

若改用 OpenAI 兼容服务，可改为配置 `OPENAI_API_KEY` 和对应的 `MF_AGENT_OPENAI_BASE_URL`、`MF_AGENT_OPENAI_MODEL`。不得同时把密钥写入 Git、YAML、PowerShell 脚本或交接文档。

## 3. 运行约定

启用 `prod` Profile 后，AgentService 会：

- 使用 `MF_EP_INTERNAL_TOKEN` 轮询、失败回写、人工恢复后的重新消费和 `ack`。
- 使用 `MF_AGENT_KNOWLEDGE_SYNC_KEY` 保护状态查询、故障补偿和失败事件重试接口。
- 仅在 `MF_AGENT_LLM_ENABLED=true` 且模型 API Key 已配置时调用真实模型；否则继续走受控工具路由和知识检索降级。

本次不启动服务或执行接口调用。待总负责人分发共享内部令牌，并完成三服务部署编排后，再统一进行生产环境联调。
