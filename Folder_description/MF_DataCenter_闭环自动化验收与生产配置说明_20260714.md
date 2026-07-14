# MF_DataCenter 闭环自动化验收与生产配置说明

更新时间：2026-07-14

## 2026-07-14 自动化验收结果

已在本地真实联调环境执行 `scripts/test-knowledge-closed-loop.ps1 -ConfirmRun`：问题 `6`、候选 `6` 创建 MF_EP 草稿 `5`，发布内容 `28`；MF_EP 发布事件 `9` 与下线事件 `10` 均已被 AgentService 确认。该受控 FAQ 已下线，不作为正式知识内容保留。

P1 自动化复验：问题 `8`、候选 `8`、MF_EP 草稿 `7`、内容 `30`；发布事件 `13` 和下线事件 `14` 的单事件查询均返回 `deliveryStatus=acknowledged`、`consumer=mf-agent-service`。脚本还通过 AgentService 真实咨询验证“发布后命中、下线后不再命中”，内容 `30` 已下线。

## 自动化验收

脚本：

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_DataCenter
.\scripts\test-knowledge-closed-loop.ps1 -ConfirmRun -MfEpInternalToken $env:MF_EP_INTERNAL_TOKEN
```

脚本通过 DataCenter API 创建带时间戳的低风险受控 FAQ 候选，依次执行草稿创建、发布、等待 AgentService 对 `publish` 事件 ack、按事件 ID 查询 MF_EP 的最终状态、调用 AgentService 验证内容命中、下线、等待 `offline` 事件 ack、再次查询最终状态并验证不再命中。

输出包含知识缺口、候选、MF_EP 草稿/正式内容、两个事件 ID、确认方、确认时间和命中/失效验证结果。内部令牌必须由调用方显式传入，脚本不保存令牌。测试内容会被正式下线，但草稿、事件和 DataCenter 审计保留作追溯证据；如需物理清理，应由 MF_EP 负责人按测试标记处理。

## 生产配置

正常链路为：

```text
MF_EP ai_content_sync_event
-> AgentService 轮询、验证、ack
-> DataCenter 观察 pending/acknowledged
```

以下密钥不得写入仓库：

```text
MF_EP_INTERNAL_TOKEN=<MF_EP、AgentService、DataCenter一致的内部服务凭据>
```

DataCenter 通过部署环境变量配置 MF_EP 内部调用凭据；`internal-sync` 仅用于故障处置，不是发布后的正常路径。

## 失败治理

- 新发布/下线事件先记为 `pending`。
- DataCenter 先观察到 MF_EP 事件，再在事件从待确认队列消失时标为 `acknowledged`。
- 仍为 `pending` 的记录应在工作台审计中处理；手工“同步重试”用于诊断或故障恢复。
- 不将“已发布/已下线”因同步故障改写为失败，内容事实以 MF_EP 为准。
