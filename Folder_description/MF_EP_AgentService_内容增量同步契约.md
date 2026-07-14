# MF_EP 与 MF_AgentService 内容增量同步契约

更新时间：2026-07-14  
契约提供方：MF_EP  
契约消费者：MF_AgentService  
协作观察方：MF_DataCenter

## 1. 目的

当 MF_EP 的 AI 内容草稿发生正式发布、下线或版本回滚时，MF_EP 会写入待确认同步事件。

MF_AgentService 处理事件、刷新自身知识索引并确认成功后，MF_EP 才将事件标记为已确认。

```text
MF_EP 发布/下线/回滚
  -> ai_content_sync_event(pending)
  -> MF_AgentService 拉取事件
  -> 从 MF_EP 读取最新公开 FAQ/文章/百科
  -> 刷新关键词索引和已启用的向量索引
  -> 验证刷新成功
  -> MF_EP 确认事件(acknowledged)
```

## 2. 鉴权

所有接口使用内部服务凭据：

```http
X-MF-Internal-Token: <MF_EP内部服务凭据>
```

生产环境不得继续使用默认值 `change-me`，应通过部署环境变量或密钥管理服务注入。

## 3. 拉取待同步事件

```http
GET /internal/ai-content/sync-events?limit=100
```

响应数据为待确认事件列表：

```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 18,
      "draftId": 36,
      "mfEpContentId": 128,
      "contentType": "faq",
      "action": "publish",
      "version": 4,
      "deliveryStatus": "pending",
      "createTime": "2026-07-13 15:30:00"
    }
  ]
}
```

字段含义：

| 字段 | 含义 |
| --- | --- |
| `id` | 同步事件唯一 ID，也是确认对象。 |
| `draftId` | MF_EP AI 内容草稿 ID。 |
| `mfEpContentId` | 正式 FAQ/文章/百科内容 ID。 |
| `contentType` | `faq`、`article` 或 `encyclopedia`。 |
| `action` | `publish`、`offline` 或 `rollback`。 |
| `version` | 草稿当前版本；与 `draftId + version + action` 共同构成业务幂等键。 |

## 4. AgentService 处理规则

1. 定时拉取待确认事件，建议本地开发间隔不超过 30 秒，生产按容量调整。
2. 按事件顺序读取 MF_EP 的公开 FAQ、文章和百科接口，刷新关键词索引。
3. 若向量 RAG 已启用，同时更新向量索引；未启用时不得因此阻塞关键词索引刷新。
4. `publish` 与 `rollback`：刷新成功后，应能按内容标题或关键标签检索到对应内容。
5. `offline`：刷新成功后，应确认下线内容不再出现在公开知识索引中。
6. 仅在上述刷新与验证成功后调用确认接口。
7. 失败时不确认事件，并调用 MF_EP 失败回写接口。未达到重试上限时状态保持 `pending`；达到上限或超时时写为 `failed` 或 `timed_out`。

## 5. 确认已处理事件

```http
POST /internal/ai-content/sync-events/{eventId}/ack
Content-Type: application/json
```

```json
{
  "consumer": "mf-agent-service"
}
```

确认后 MF_EP 会记录：

```text
deliveryStatus = acknowledged
consumer = mf-agent-service
acknowledgedAt = 当前时间
```

## 6. 查询单个事件审计

```http
GET /internal/ai-content/sync-events/{eventId}
```

该接口返回指定事件的完整状态，可用于 DataCenter 在事件从待确认队列消失后确认其最终状态。返回字段包括 `deliveryStatus`、`consumer` 和 `acknowledgedAt`。

`ack` 为幂等操作：已确认事件的重复确认不会改变原确认信息。

## 7. 幂等与失败处理

- AgentService 必须以 `eventId` 或 `draftId + version + action` 去重。
- 重复拉取同一 `pending` 事件时，允许重复刷新，但不得产生重复知识文档。
- 确认接口可安全重复调用；最终状态保持 `acknowledged`。
- 不允许在刷新失败、内容未命中或下线内容仍可命中时确认事件。
- DataCenter 应展示 MF_EP 草稿 ID、正式内容 ID、事件 ID、同步状态和确认时间。

### 7.1 AgentService 失败回写

```http
POST /internal/ai-content/sync-events/{eventId}/failure
Content-Type: application/json
```

```json
{
  "consumer": "mf-agent-service",
  "state": "pending",
  "failureAttempts": 2,
  "reason": "knowledge index refresh failed"
}
```

| 字段 | 说明 |
| --- | --- |
| `consumer` | 消费者标识，当前使用 `mf-agent-service`。 |
| `state` | `pending`、`failed` 或 `timed_out`。 |
| `failureAttempts` | AgentService 对该事件的累计失败次数，必须为正整数。 |
| `reason` | 最近失败原因，不能为空。 |

状态规则：`pending` 会继续由 AgentService 正常轮询；`failed` 和 `timed_out` 不再出现在待确认事件列表，等待人工恢复。

### 7.2 人工恢复失败事件

```http
POST /internal/ai-content/sync-events/{eventId}/retry
Content-Type: application/json
```

```json
{
  "operator": "ops-user"
}
```

该接口只接受 `failed` 或 `timed_out` 事件，恢复后状态变为 `pending`，并保留失败原因、失败次数以及人工重试次数/时间作为审计证据。

恢复后的协作顺序：先调用 MF_EP `retry`，再调用 AgentService `POST /api/agent/knowledge/sync-events/retry-failed`，由 AgentService 对恢复为 `pending` 的事件再次刷新、验证并 ack。

## 8. 三方验收用例

| 用例 | 预期 |
| --- | --- |
| 低风险 FAQ 发布 | MF_EP 产生 `publish` 事件，Agent 刷新后命中新内容，事件被确认。 |
| 内容下线 | MF_EP 产生 `offline` 事件，Agent 刷新后不再命中，事件被确认。 |
| 回滚到旧版本 | MF_EP 产生 `rollback` 事件，Agent 命中旧版本内容，事件被确认。 |
| 高风险未审核发布 | MF_EP 拒绝发布，不能产生 `publish` 事件。 |
| 同步失败 | 事件保持 `pending`，Agent 有重试与告警记录，DataCenter 可见失败状态。 |

## 9. 当前状态与边界

MF_EP 已提供事件落库、待确认拉取、单事件审计查询、幂等确认、失败回写和人工恢复接口。MF_AgentService 已实现事件消费者、索引刷新、检索验证、失败状态与定向重试；MF_DataCenter 已将事件 ID、待确认/已确认状态纳入发布审计，并提供受控闭环自动验收脚本。

2026-07-14 的自动化验收已验证：DataCenter 候选 `6` 创建 MF_EP 草稿 `5` 和 FAQ `28`；发布事件 `9` 与下线事件 `10` 均由 `mf-agent-service` 自动确认，FAQ 最终为下线状态。
