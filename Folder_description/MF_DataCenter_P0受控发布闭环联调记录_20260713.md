# MF_DataCenter P0 受控发布闭环联调记录

更新时间：2026-07-13  
依据：`MF_Project_EP_Agent_Data进度汇总报告_20260713.md`

## 1. DataCenter 已完成的本轮事项

- 为内容候选补齐发布审计写入和查询：`draft_created`、`published`、`offline`、`failed`。
- 知识运营工作台新增发布审计查看入口。
- 修复知识工作台全部路径参数显式绑定，避免运行时无法解析 `id`。
- 已验证 DataCenter 会在 MF_EP 草稿创建失败时：
  - 保留候选；
  - 标记 `status=sync_failed`；
  - 保存失败信息；
  - 写入 `dc_ai_content_publish_log` 的 `failed` 审计记录；
  - 不继续执行发布。

## 2. P0 真实联调执行结果

按总负责人建议，创建了一个带 `integration-audit` 标记的低风险 FAQ 候选，并调用：

```text
DataCenter
-> POST /api/ai/knowledge/candidates/{id}/mf-ep-draft
-> MF_EP POST /internal/ai-content/drafts
```

结果：MF_EP 返回业务失败，DataCenter 正确停止在草稿创建阶段，没有产生正式公开内容。

根因经只读数据库核对为：

```text
fertilizer.ai_content_draft 表不存在
```

因此，运行中的 MF_EP 尚未执行其 AI 草稿表初始化脚本。该表属于 MF_EP 正式内容能力，DataCenter 不应直接在 `fertilizer` 库创建或修改它。

本次临时知识缺口、候选和失败审计记录均已清理；未留下测试运营数据。

## 3. 当前阻塞与交接动作

| 事项 | 负责人 | 所需动作 | 验收 |
| --- | --- | --- | --- |
| 初始化 AI 草稿表 | MF_EP | 执行并确认 `ai-content-draft-v1.sql` 已落到运行中的 `fertilizer` 库 | `SHOW TABLES LIKE 'ai_content_draft'` 返回该表 |
| 重测创建草稿 | DataCenter | 重试低风险候选的 MF_EP 草稿创建 | DataCenter 保存 `mf_ep_draft_id`，审计为 `draft_created` |
| 发布与下线 | DataCenter + MF_EP | 发布低风险 FAQ 后立即下线 | DataCenter 保存 `mf_ep_content_id`，审计包含 `published`、`offline` |
| 发布后同步 | MF_AgentService | 提供/启用带鉴权的 `/api/agent/knowledge/sync` 调用凭据与约定 | 发布后检索命中，下线后不再命中 |

## 4. DataCenter 下一步

MF_EP 完成表初始化后，DataCenter 将按以下顺序继续：

```text
低风险候选
-> 创建 MF_EP 草稿
-> 发布
-> 记录草稿 ID、正式内容 ID 和审计
-> 等待/调用 AgentService 同步约定
-> 下线
-> 记录下线审计
```

在 AgentService 同步鉴权与回写约定明确前，DataCenter 不会伪造“发布后已检索命中”的验收结论。

## 5. 2026-07-14 阻塞解除后的真实验证

MF_EP 已完成 `ai_content_draft`、`ai_content_draft_version` 和 `ai_content_sync_event` 表初始化后，DataCenter 使用一次性本地内部同步凭据完成了低风险 FAQ 的真实受控链路：

```text
知识候选 3
-> MF_EP 草稿 1
-> 发布得到正式内容 24
-> AgentService internal-sync 成功（关键词索引 23 条）
-> 下线
-> AgentService internal-sync 成功（关键词索引 22 条）
```

DataCenter 审计已记录：

```text
draft_created
published
agent_sync_success（publish）
offline
agent_sync_success（offline）
```

发布和下线后的候选状态均由 DataCenter 保存；MF_EP 中的测试内容已经通过正式下线接口下线，未继续公开展示。草稿、正式内容和 DataCenter 审计记录保留用于跨项目追溯，不由 DataCenter 直接删除 MF_EP 业务数据。

### 残留问题：MF_EP 同步事件拉取接口

本轮直接调用：

```http
GET /internal/ai-content/sync-events?limit=20
```

MF_EP 当前返回业务失败（`code=500`），并且在 `ai_content_sync_event` 中未查询到草稿 `1` 的事件行。因此：

- DataCenter -> AgentService 的主动 `internal-sync` 闭环已验证成功；
- 但“MF_EP 事件拉取 -> AgentService ack”的契约路径尚不能验收；
- 该问题应由 MF_EP 负责人检查事件创建、查询接口及运行中服务版本。

## 6. 2026-07-14 事件消费者闭环验收

MF_EP 与 AgentService 完成事件消费者联调后，DataCenter 已将发布后行为调整为“登记并观察 MF_EP 同步事件”，不再自动调用旧 `internal-sync` 路径造成重复刷新。手工同步按钮仅保留为故障处置。

使用新的 DataCenter 低风险候选完成验证：

```text
候选 5
-> MF_EP 草稿 4
-> 正式内容 27
-> publish 事件 7
-> AgentService 自动 ack
-> DataCenter 记录 acknowledged
-> 下线
-> offline 事件 8
-> AgentService 自动 ack
-> DataCenter 记录 acknowledged
```

DataCenter 同步审计新增保存：MF_EP 事件 ID、`pending/acknowledged` 状态和确认时间。只有先观察到 pending、之后从 MF_EP 待确认队列消失的事件，才会标记为 `acknowledged`，避免将未确认事件误判为成功。

本次测试内容已通过正式下线接口下线；候选、草稿、正式内容、事件及 DataCenter 审计记录保留为三方联调证据。

## 7. 2026-07-14 自动化复验

- 新增受控验收脚本：`MF_DataCenter/scripts/test-knowledge-closed-loop.ps1`；脚本必须显式传入 `-ConfirmRun`，避免误创建测试知识。
- 真实复验：DataCenter 问题 `6` → 候选 `6` → MF_EP 草稿 `5` → 内容 `28`；发布事件 `9`、下线事件 `10` 均获 AgentService `acknowledged`。
- 该测试内容已下线；DataCenter 审计弹窗现展示 MF_EP 事件 ID 与“待确认 / 已确认 / 失败”状态，供人工复核与故障重试使用。

## 8. 2026-07-14 P1 三方自动化复验

自动化脚本不再仅依据 DataCenter 观察到的事件消失判定成功，而是按 DataCenter 审计记录的事件 ID 调用 MF_EP `GET /internal/ai-content/sync-events/{id}`，核验 `deliveryStatus=acknowledged` 和确认方；随后通过 AgentService `POST /api/agent/chat` 验证发布命中和下线失效。

```text
问题 8 -> 候选 8 -> MF_EP 草稿 7 -> 内容 30
-> publish 事件 13（acknowledged，mf-agent-service）
-> AgentService 检索命中
-> offline 事件 14（acknowledged，mf-agent-service）
-> AgentService 检索不再命中
```

内容 `30` 已通过正式下线接口下线。脚本要求显式传入 `-MfEpInternalToken`，不把生产内部令牌写入仓库。
