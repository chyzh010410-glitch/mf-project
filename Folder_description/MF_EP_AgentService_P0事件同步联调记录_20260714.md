# MF_EP 与 AgentService P0 事件同步联调记录

更新时间：2026-07-14  
范围：MF_EP 内容发布事件到 AgentService 知识索引的自动同步与确认。

## 1. 本次完成项

- 已在本地 `fertilizer` 库执行 MF_EP AI 内容表初始化：
  - `ai_content_draft`
  - `ai_content_draft_version`
  - `ai_content_sync_event`
- 已修正 `ai-content-governance-v2.sql` 的 MySQL 8.0 兼容写法；该脚本应由迁移流程单次执行，不能在同一库重复手工执行。
- 已重启 MF_EP，使运行中的 `8080` 服务加载事件查询与确认接口。
- 已为 AgentService 运行环境补齐 `MF_EP_INTERNAL_TOKEN`，使其可轮询 MF_EP 内部事件并回写确认。

## 2. 真实联调结果

使用一次性低风险 FAQ 草稿进行验证，未使用模拟确认接口：

```text
MF_EP 发布草稿
-> 写入 publish 事件
-> AgentService 30 秒轮询
-> 刷新知识索引并按关键词验证命中
-> AgentService 回写 ack
-> MF_EP 事件状态 acknowledged
```

结果：发布事件自动确认；AI 客服检索到来源 `faq-25`，回答包含测试专用关键词。

随后验证下线链路：

```text
MF_EP 下线草稿
-> 写入 offline 事件
-> AgentService 刷新索引并验证内容不存在
-> AgentService 回写 ack
-> MF_EP 事件状态 acknowledged
```

结果：下线事件自动确认；同一关键词不再返回 `faq-25`，客服返回知识不足结果且 `sources=[]`。

本次共验证 4 条事件：

| 操作 | 版本 | 事件确认方 | 结果 |
| --- | --- | --- | --- |
| 发布 | 2 | 手工诊断确认 | 用于先验证 MF_EP ack 接口 |
| 下线 | 3 | `mf-agent-service` | 自动确认通过 |
| 再发布 | 4 | `mf-agent-service` | 自动确认并检索命中 |
| 最终下线 | 5 | `mf-agent-service` | 自动确认并检索失效 |

## 3. 发现并处理的问题

| 问题 | 根因 | 处理 |
| --- | --- | --- |
| DataCenter 创建草稿失败 | MF_EP 运行库未初始化 AI 草稿表 | 已执行 MF_EP 初始化脚本 |
| 事件查询返回旧路由错误 | 8080 仍运行旧版 MF_EP 进程 | 已重启为当前代码 |
| 事件长期 pending | AgentService 未配置 `MF_EP_INTERNAL_TOKEN`，无权读取/确认内部事件 | 已在本地 AgentService 进程注入该环境变量并重启 |

## 4. 测试数据处理

本次创建的临时草稿 `2`、FAQ 内容 `25`、4 条同步事件及草稿版本记录已全部从本地测试库清除。没有删除既有业务内容。

## 5. 对 DataCenter 的交接

DataCenter 现在可以重试其受控发布闭环：

```text
知识候选
-> POST /internal/ai-content/drafts
-> 发布
-> 等待 AgentService 自动消费和 ack
-> 下线
-> 等待 AgentService 自动消费和 ack
```

联调运行环境必须保持以下内部凭据一致：

```text
MF_EP_INTERNAL_TOKEN=<与 MF_EP 内部接口配置一致的值>
```

本记录只证明 MF_EP <-> AgentService 的事件契约已通过。DataCenter 的“候选 -> 草稿 -> 发布/下线 -> 发布审计”完整三方审计，仍应由 DataCenter 依据其候选记录再执行一次并保留审计证据。

## 6. 对 DataCenter 旧阻塞结论的复核

2026-07-14 使用当前运行中的 MF_EP `8080` 服务复核：

```http
GET /internal/ai-content/sync-events?limit=20
```

已返回 `code=200`，当前没有待确认事件。此前 DataCenter 联调记录中的 `code=500` 来自 MF_EP 重启前的旧运行进程，不能作为当前阻塞结论。

DataCenter 候选 `3` 对应的草稿 `1` 没有历史事件行，是因为当时采用了 DataCenter 主动调用 AgentService `POST /api/agent/knowledge/internal-sync` 的路径；该路径已验证发布命中、下线失效，但不会为已经完成的旧操作补写 MF_EP 事件。若需要同一候选链路同时留下 DataCenter 审计和 MF_EP 事件 ack 证据，请以新的低风险候选重跑一次。

## 7. DataCenter 三方闭环复核通过

DataCenter 已使用新的低风险候选完成事件观察路径。MF_EP 于 2026-07-14 只读核验到：

| DataCenter 候选 | MF_EP 草稿 | 正式内容 | 事件 | MF_EP 状态 | Agent 确认 |
| --- | --- | --- | --- | --- | --- |
| 5 | 4 | 27 | 7（publish，版本 2） | `acknowledged` | `mf-agent-service` 于 08:13:07 确认 |
| 5 | 4 | 27 | 8（offline，版本 3） | `acknowledged` | `mf-agent-service` 于 08:13:37 确认 |

草稿 `4` 当前为 `offline`，正式 FAQ `27` 的 `is_published=0`。同时，`GET /internal/ai-content/sync-events?limit=20` 返回 `code=200` 且无待确认事件。

因此，以下链路已完成真实验证：

```text
DataCenter 候选
-> MF_EP 草稿
-> 发布事件
-> AgentService 自动消费并 ack
-> DataCenter 记录 acknowledged
-> 下线事件
-> AgentService 自动消费并 ack
-> DataCenter 记录 acknowledged
```

DataCenter 不再在正常发布/下线流程中调用旧的 `internal-sync`；该接口仅保留为故障重试工具。

## 8. MF_EP P1 治理补充（2026-07-14）

本轮已在 MF_EP 完成以下生产化补充：

- 新增 `GET /internal/ai-content/sync-events/{id}`，可按事件 ID 查询 `pending` 或 `acknowledged`、确认方和确认时间；已用事件 `7` 实测返回 `acknowledged`。
- `ack` 改为幂等：重复确认已确认事件不会改写确认信息或报错。
- 高风险草稿被修改后会清除原审核人和审核时间，重新进入 `pending_review`，必须再次人工审核通过后才能发布。
- 版本快照增加 `reviewed_at`，使审核人、审核意见和审核时间可以随版本追溯。
- 新增 `application-prod.yml`：生产环境数据库、Redis、MF_EP 内部令牌和 DataCenter 内部凭据均从环境变量读取，Knife4j 关闭。

现有环境需单次执行：

```text
mf-fertilizer/fertilizer-api/src/main/resources/db/ai-content-governance-v3.sql
```

生产环境至少需要提供：

```text
MF_DB_URL
MF_DB_USERNAME
MF_DB_PASSWORD
MF_REDIS_HOST
MF_REDIS_PASSWORD
MF_EP_INTERNAL_TOKEN
MF_DATACENTER_BASE_URL
MF_DATACENTER_INTERNAL_TOKEN
MF_DATACENTER_IDENTITY_SECRET
```

MF_EP 已提供失败回写和人工恢复接口；AgentService 需按最新契约调用失败回写接口，才能把其本地失败次数、失败原因和 `failed`/`timed_out` 状态持久化到 MF_EP 事件审计。该项不影响已通过的 publish/offline 自动 ack 主链路。

### 8.1 失败回写 V4 迁移与调用

现有环境还需单次执行：

```text
mf-fertilizer/fertilizer-api/src/main/resources/db/ai-content-sync-governance-v4.sql
```

该迁移为 `ai_content_sync_event` 增加失败次数、最近失败原因/时间、人工重试次数/时间。AgentService 接入顺序为：

```text
自动失败但仍可重试 -> POST /sync-events/{id}/failure (state=pending)
达到重试上限 -> POST /sync-events/{id}/failure (state=failed)
超过等待上限 -> POST /sync-events/{id}/failure (state=timed_out)
人工恢复 -> POST /sync-events/{id}/retry
AgentService 再调用 /api/agent/knowledge/sync-events/retry-failed
```

MF_EP 已实测该契约：临时事件 `15` 回写 `failed` 后可查询到失败次数 `5` 和失败原因；人工重试后恢复为 `pending` 且重试次数为 `1`。该临时事件已物理清理。

## 9. DataCenter 自动化闭环复核通过

DataCenter 已将三方主链路固化为 `scripts/test-knowledge-closed-loop.ps1 -ConfirmRun`。MF_EP 于 2026-07-14 只读复核其最新一次执行结果：

| DataCenter 候选 | MF_EP 草稿 | FAQ | 事件 | 结果 |
| --- | --- | --- | --- | --- |
| 6 | 5 | 28 | 9（publish，版本 2） | `acknowledged`，`mf-agent-service` 于 09:23:38 确认 |
| 6 | 5 | 28 | 10（offline，版本 3） | `acknowledged`，`mf-agent-service` 于 09:24:08 确认 |

草稿 `5` 为 `offline`，FAQ `28` 的 `is_published=0`。该结果表明三方 publish -> ack -> offline -> ack 验收已可重复执行，测试内容以正式下线方式保留审计证据。
