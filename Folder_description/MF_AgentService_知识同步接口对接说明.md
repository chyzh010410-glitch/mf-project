# MF_AgentService 知识同步对接说明

## 1. 用途

正式同步链路由 `MF_AgentService` 主动拉取 `MF_EP` 的待确认事件：

```text
MF_EP 发布/下线/回滚
-> 待确认同步事件
-> MF_AgentService 拉取、刷新与验证
-> MF_EP 确认事件
```

数据中台是审计观察方，应保存 MF_EP 事件与同步结果；它不需要直接调用 AgentService。下文接口保留为人工补偿、排障和受控手工刷新入口。

## 2. 事件消费者

AgentService 使用以下配置调用 MF_EP：

```text
MF_EP_INTERNAL_TOKEN=<MF_EP内部服务凭据>
MF_AGENT_KNOWLEDGE_SYNC_POLL_INTERVAL=30s
```

每次轮询：

```http
GET  /internal/ai-content/sync-events?limit=100
POST /internal/ai-content/sync-events/{eventId}/ack
X-MF-Internal-Token: <MF_EP_INTERNAL_TOKEN>
```

对于 `publish` 和 `rollback`，AgentService 只有在对应内容进入关键词索引后才确认；对于 `offline`，只有在对应内容不再存在于索引后才确认。失败事件保持 `pending` 并在后续轮询重试。

## 3. 人工补偿接口

```http
POST http://localhost:8092/api/agent/knowledge/internal-sync
X-MF-Internal-Token: <MF_AGENT_KNOWLEDGE_SYNC_KEY>
Content-Type: application/json
```

请求体：

```json
{
  "requestId": "candidate-42-published-v1",
  "contentIds": [101]
}
```

字段说明：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `requestId` | 是 | 数据中台生成的稳定幂等键。建议由候选 ID、操作类型和版本组成。 |
| `contentIds` | 否 | 本次发布或下线的 MF_EP 正式内容 ID；用于关联审计。空数组也可触发全量刷新。 |

## 4. 响应与处理

成功示例：

```json
{
  "requestId": "candidate-42-published-v1",
  "contentIds": [101],
  "refreshMode": "full_refresh",
  "success": true,
  "reused": false,
  "indexedDocuments": 36,
  "error": null,
  "completedAt": "2026-07-13T07:23:14Z"
}
```

人工补偿请求的调用方应保存以下字段：`requestId`、内容候选 ID、MF_EP 草稿/正式内容 ID、操作类型、`success`、`indexedDocuments`、`error`、`completedAt`。

处理约定：

- `success=true`：记录同步成功，再进行用户提问命中或下线失效验证。
- `success=false`：记录失败原因，保留治理待办并重试。
- 对成功的相同 `requestId` 重复调用会返回 `reused=true`，不会重复刷新。
- 失败请求不会被缓存，使用相同 `requestId` 可重试。

## 5. 当前刷新策略

当前 MF_EP 提供的是公开知识列表查询，因此 AgentService 会重新拉取百科、FAQ 和文章并重建轻量检索索引。`contentIds` 用于本次变更关联和审计，不表示只拉取指定内容。

这个策略满足 V1 的发布后即时可检索、下线后即时失效要求；后续如 MF_EP 提供按内容 ID 的增量查询接口，再升级为真实增量同步。

## 6. 环境配置

在 AgentService 环境中配置：

```text
MF_AGENT_KNOWLEDGE_SYNC_KEY=<随机且仅内部使用的密钥>
MF_EP_INTERNAL_TOKEN=<MF_EP内部服务凭据>
MF_AGENT_KNOWLEDGE_SYNC_POLL_INTERVAL=30s
MF_AGENT_KNOWLEDGE_SYNC_MAX_RETRIES=5
MF_AGENT_KNOWLEDGE_SYNC_PENDING_TIMEOUT=5m
```

人工调用补偿接口时使用 `MF_AGENT_KNOWLEDGE_SYNC_KEY`；AgentService 拉取 MF_EP 事件时使用 `MF_EP_INTERNAL_TOKEN`。两者均不得暴露给客户端或写入代码仓库。

生产环境使用 Spring `prod` Profile，配置文件为 `MF_AgentService/src/main/resources/application-prod.yml`；文件不包含任何密钥值。

## 7. 失败治理与人工重试

AgentService 会记录每个已观察事件的同步状态、失败次数、最后错误、首次观察时间和最后尝试时间：

```text
pending      自动轮询重试中
acknowledged 已完成刷新、验证和 MF_EP 确认
failed       已达到 MF_AGENT_KNOWLEDGE_SYNC_MAX_RETRIES
timed_out    超过 MF_AGENT_KNOWLEDGE_SYNC_PENDING_TIMEOUT
```

每次刷新、验证或确认失败后，AgentService 会调用 MF_EP：

```http
POST /internal/ai-content/sync-events/{eventId}/failure
```

请求携带 `consumer=mf-agent-service`、当前 `state`、累计 `failureAttempts` 和最近 `reason`。MF_EP 负责持久化这些字段，并在 `failed` 或 `timed_out` 时将事件移出普通待确认队列。

状态查询与失败重试接口均要求 `X-MF-Internal-Token: <MF_AGENT_KNOWLEDGE_SYNC_KEY>`：

```http
GET  /api/agent/knowledge/sync-events/status
POST /api/agent/knowledge/sync-events/retry-failed
```

`retry-failed` 只处理本地状态为 `failed` 或 `timed_out` 的待确认事件，不会重复刷新其他正常 `pending` 事件。

人工恢复顺序固定为：

```text
MF_EP POST /internal/ai-content/sync-events/{eventId}/retry
-> AgentService POST /api/agent/knowledge/sync-events/retry-failed
-> 再次刷新、验证并 ack
```

## 8. 本轮验收范围

AgentService 已完成事件拉取、内部鉴权、索引刷新、发布/下线验证、事件确认、失败重试和单元测试。人工补偿接口保留成功请求幂等和失败可重试。

2026-07-14 本地测试结果：`Tests run: 28, Failures: 0, Errors: 0, Skipped: 0`。

三方本地真实联调已验证发布事件自动确认和下线后失效。后续需将这条链路沉淀为可重复自动化验收，并覆盖回滚、高风险审核拦截和失败重试状态回写。
