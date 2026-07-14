# MF Project 生产演练验收记录

验收时间：2026-07-14 10:48  
验收人：Codex  
范围：`MF_EP`、`MF_AgentService`、`MF_DataCenter` 的 AI 知识发布、同步、失败重试与下线失效链路

## 0. 第二次独立复验（12:38-12:43）

本节为三方完成本机 `prod` 配置后的独立复验，不采信负责人文字回执作为验收证据。

### 0.1 运行态与凭据

| 检查项 | 结果 |
| --- | --- |
| MF_EP `8080` 健康检查 | `UP` |
| MF_AgentService `8092` 健康检查 | `UP` |
| DataCenter `8091` 状态检查 | `code=0` |
| DataCenter 启动命令 | 明确带有 `--spring.profiles.active=prod` |
| DataCenter 访问 MF_EP 使用已分发密钥 | 业务返回 `code=200` |
| MF_EP 使用默认 `change-me` 访问同步事件 | 业务返回 `code=403` |

说明：MF_EP 的错误响应 HTTP 状态仍为 `200`，实际验权结论以响应 JSON 的 `code` 字段为准。因此默认密钥已被拒绝，非默认内部密钥已生效。

### 0.2 正常发布与下线闭环

执行 `MF_DataCenter/scripts/test-knowledge-closed-loop.ps1`，令牌仅从当前 Windows 部署用户环境读取，未输出或写入文件。

| 项目 | 值 |
| --- | --- |
| Topic | `datacenter-closed-loop-audit-20260714123915` |
| CandidateId | `13` |
| MF_EP DraftId / ContentId | `12` / `35` |
| 发布事件 / 下线事件 | `24` / `25` |
| 发布确认 / 下线确认 | 均为 `mf-agent-service` |
| 发布后 Agent 命中 | `true` |
| 下线后 Agent 不再命中 | `true` |
| 结果 | `acknowledged_and_retrieval_verified` |

### 0.3 失败回写与人工重试闭环

独立创建受控内容后，将发布事件 `26` 注入为 `failed`，再调用 MF_EP `retry` 接口。该事件最终由 AgentService 确认；随后下线事件 `27` 也被确认。

| 项目 | 值 |
| --- | --- |
| Topic | `failure-retry-audit-20260714124222` |
| CandidateId / ContentId | `14` / `36` |
| 发布事件 | `26` |
| FailureAttempts / RetryCount | `1` / `1` |
| 发布最终状态 | `acknowledged` |
| 下线事件最终状态 | `27`，`acknowledged` |
| 下线确认方 | `mf-agent-service` |
| 结果 | `failure_retry_acknowledged` |

### 0.4 未满足真实生产放行的事项

- 本次是单机 `127.0.0.1` 本机 `prod` 演练，不是目标生产服务器验收。
- DataCenter `/api/system/status` 显示 `mfEpDatasource.enabled=false`，因此 MF_EP 业务库只读数据源未启用，运营总览仍会使用演示数据。若真实生产需要该数据源，必须配置 `MF_EP_DATASOURCE_ENABLED=true`、`MF_EP_DB_URL`、`MF_EP_DB_USERNAME`、`MF_EP_DB_PASSWORD` 后复验。
- AgentService 的真实模型密钥未在本次验收中读取或输出；模型调用的计费、限流、超时和真实回答质量仍应在目标生产环境单独验收。

本次复验结论：本机 `prod` 的 P0 知识发布、ack、失败重试、下线失效和默认密钥拒绝均通过；真实生产服务器上线仍需按上述两项环境条件复验。

## 1. 验收结论

本次验收结论分层如下：

| 层级 | 结论 |
| --- | --- |
| 本地真实跨服务闭环 | 通过 |
| P0 自动发布/下线/ack 闭环 | 通过 |
| 失败回写与人工 retry 演练 | 通过 |
| V3/V4 数据库字段 | 通过 |
| 后端回归与 DataCenter Web 构建 | 通过 |
| 真实生产环境放行 | 暂不放行 |

暂不放行真实生产的原因：

- 当前运行进程不是完整 `prod` Profile。
- 当前本地运行态仍接受默认内部凭据 `change-me`。
- 生产密钥注入、密钥轮换、最小权限、失败告警和真实部署脚本演练仍需在目标部署环境执行。

因此，本次可标记为：

```text
生产流程演练验收通过，待真实生产环境密钥与 prod Profile 复验后放行上线。
```

## 2. 当前运行环境

端口检查结果：

| 服务 | 端口 | 状态 |
| --- | --- | --- |
| MF_EP 后端 API | 8080 | Listening |
| MF_EP 管理端 | 5173 | Listening |
| MF_EP 客户端 | 5174 | Listening |
| MF_EP 商家端 | 5175 | Listening |
| MF_DataCenter API | 8091 | Listening |
| MF_DataCenter Web | 5176 | Listening |
| MF_AgentService | 8092 | Listening |
| MF_Website | 5178 | Free |
| MF_Pet Demo | 5179 | Free |

运行进程观察：

- `MF_DataCenter` 进程使用 `--spring.profiles.active=local`。
- `MF_AgentService` 进程未显示 `prod` Profile。
- `MF_EP` 后端以本地 classpath 方式运行，未显示 `prod` Profile。

健康检查：

| 检查项 | 结果 |
| --- | --- |
| MF_EP `/actuator/health` | `UP` |
| MF_AgentService `/actuator/health` | `UP` |
| MF_DataCenter `/api/system/status` | `code=0` |
| DataCenter `scripts/check-health.ps1` | `GovernanceStatus=trusted` |

DataCenter 健康脚本输出摘要：

```text
ApiStatus=0
MetricDefinitions=8
QualityChecks=22
SourceFailedTables=0
SourceMissingFields=0
GovernanceStatus=trusted
WebStatus=200
```

## 3. P0 正常闭环验收

执行命令：

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_DataCenter
.\scripts\test-knowledge-closed-loop.ps1 -ConfirmRun -MfEpInternalToken change-me
```

验收结果：

| 项目 | 值 |
| --- | --- |
| Topic | `datacenter-closed-loop-audit-20260714104237` |
| GapId | `9` |
| CandidateId | `9` |
| MF_EP DraftId | `8` |
| MF_EP ContentId | `31` |
| PublishEventId | `16` |
| OfflineEventId | `17` |
| PublishConfirmedBy | `mf-agent-service` |
| PublishConfirmedAt | `2026-07-14T10:42:41` |
| OfflineConfirmedBy | `mf-agent-service` |
| OfflineConfirmedAt | `2026-07-14T10:43:11` |
| 发布后 Agent 命中 | `true` |
| 下线后 Agent 不再命中 | `true` |
| 结果 | `acknowledged_and_retrieval_verified` |

结论：

```text
DataCenter 候选
-> MF_EP 草稿
-> 发布事件
-> AgentService 自动消费并 ack
-> Agent 发布后命中
-> 下线事件
-> AgentService 自动消费并 ack
-> Agent 下线后不再命中
```

该链路通过。

## 4. 失败回写与 retry 演练

执行方式：

1. 创建低风险受控 FAQ 候选。
2. 发布到 MF_EP，生成 `publish` 事件。
3. 在事件被确认前，通过 MF_EP 失败回写接口注入 `failed` 状态。
4. 调用 MF_EP `retry` 接口恢复为 `pending`。
5. 等待 AgentService 再次消费并 ack。
6. 调用 AgentService 验证发布后命中。
7. 下线该内容并验证下线后不再命中。

验收结果：

| 项目 | 值 |
| --- | --- |
| Topic | `prod-failure-retry-audit-20260714104445` |
| GapId | `10` |
| CandidateId | `10` |
| MF_EP DraftId | `9` |
| MF_EP ContentId | `32` |
| PublishEventId | `18` |
| 注入失败状态 | `failed` |
| FailureAttempts | `5` |
| RetryCountAfterRetry | `1` |
| FinalPublishStatus | `acknowledged` |
| PublishConsumer | `mf-agent-service` |
| PublishAckAt | `2026-07-14T10:45:11` |
| AgentHitAfterRetry | `true` |
| OfflineEventId | `19` |
| OfflineStatus | `acknowledged` |
| OfflineAckAt | `2026-07-14T10:45:41` |
| AgentMissAfterOffline | `true` |
| 结果 | `failed_retry_ack_verified` |

数据库审计复核：

| EventId | delivery_status | consumer | failure_attempts | retry_count | acknowledged_at | last_failure_reason |
| --- | --- | --- | --- | --- | --- | --- |
| 16 | acknowledged | mf-agent-service | 0 | 0 | 2026-07-14 10:42:41 | NULL |
| 17 | acknowledged | mf-agent-service | 0 | 0 | 2026-07-14 10:43:11 | NULL |
| 18 | acknowledged | mf-agent-service | 5 | 1 | 2026-07-14 10:45:11 | production acceptance controlled failure injection |
| 19 | acknowledged | mf-agent-service | 0 | 0 | 2026-07-14 10:45:41 | NULL |

结论：

```text
failed -> retry -> pending -> AgentService 再消费 -> acknowledged
```

该恢复链路通过。

## 5. V3/V4 数据库字段验收

已在 `fertilizer` 库只读核验字段：

`ai_content_draft_version`：

```text
reviewed_at
```

`ai_content_sync_event`：

```text
failure_attempts
last_failure_reason
last_failed_at
retry_count
last_retry_at
```

结论：

```text
V3 reviewed_at 字段存在。
V4 失败回写与 retry 审计字段存在。
```

## 6. 测试内容清理状态

本次生产演练生成的正式 FAQ：

| FAQ ID | is_published |
| --- | --- |
| 31 | 0 |
| 32 | 0 |

结论：

```text
本次测试内容均已下线，不作为正式知识内容保留。
```

草稿、同步事件和 DataCenter 审计记录保留，用于追溯验收证据。

## 7. 代码级回归

### 7.1 MF_AgentService

执行：

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_AgentService
mvn test
```

结果：

```text
Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 7.2 MF_DataCenter API

执行：

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_DataCenter\datacenter-api
mvn test
```

结果：

```text
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 7.3 MF_EP 后端

执行：

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_EP\projects\mf\mf-fertilizer
mvn -pl fertilizer-api -am test -DskipTests
```

结果：

```text
BUILD SUCCESS
```

注意：该命令带 `-DskipTests`，验证的是多模块编译与资源处理，不是执行单元测试。

### 7.4 MF_DataCenter Web

执行：

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_DataCenter\datacenter-web
npm run build
```

结果：

```text
vite build success
```

非阻断警告：

```text
@vueuse/core PURE annotation warning
```

该警告与此前构建表现一致，不阻断本次验收。

## 8. 不放行项

以下事项必须在真实生产环境补齐后，才能标记为生产上线验收通过：

1. 使用非默认内部凭据。

当前本地运行态仍接受：

```text
MF_EP_INTERNAL_TOKEN=change-me
```

生产环境不得使用默认值。

2. 使用 `prod` Profile 重启三服务。

需验证：

```text
MF_EP: application-prod.yml
MF_AgentService: application-prod.yml
MF_DataCenter: 生产部署配置
```

3. 通过部署环境或密钥管理注入：

```text
MF_EP_INTERNAL_TOKEN
MF_DATACENTER_INTERNAL_TOKEN
MF_DATACENTER_IDENTITY_SECRET
MF_AGENT_KNOWLEDGE_SYNC_KEY
MF_EP_BASE_URL
MF_DATACENTER_BASE_URL
```

4. 真实生产环境再次执行：

```text
正常发布/下线/ack 闭环
失败回写 -> failed -> retry -> ack
下线后 Agent 不再命中
DataCenter 审计状态一致
```

5. 建议补充生产告警：

```text
pending 超时
failed/timed_out 事件
retry 失败
AgentService 无法访问 MF_EP
DataCenter 无法读取同步审计
```

## 9. 最终判断

本次验收已经证明：

```text
P0 自动发布/下线/ack 闭环可运行
失败回写与 retry 恢复链路可运行
V3/V4 字段存在且能承载审计
AgentService 和 DataCenter 后端测试通过
DataCenter Web 可生产构建
DataCenter 健康检查为 trusted
```

但当前环境仍是本地/演练态，不是严格生产态。

最终结论：

```text
生产演练验收通过。
真实生产上线验收暂不放行，待非默认密钥注入与 prod Profile 部署后复验。
```
