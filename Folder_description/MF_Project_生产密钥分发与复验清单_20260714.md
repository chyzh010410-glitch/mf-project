# MF Project 生产密钥分发与复验清单

更新时间：2026-07-14  
适用对象：总负责人、部署负责人、MF_EP 负责人、MF_AgentService 负责人、MF_DataCenter 负责人

## 1. 当前状态

三方已完成各自生产配置准备，但尚未完成真实生产环境统一密钥注入与跨服务复验。

| 项目 | 当前状态 | 仍等待 |
| --- | --- | --- |
| MF_EP | 已准备 `application-prod.yml` 和生产配置交接清单 | 等总负责人分发内部 token、数据库/Redis 生产凭据、DataCenter 内部凭据 |
| MF_AgentService | 已准备 `application-prod.yml` 和生产配置说明 | 等总负责人分发 `MF_EP_INTERNAL_TOKEN`、`MF_AGENT_KNOWLEDGE_SYNC_KEY`，并注入部署环境 |
| MF_DataCenter | 已准备生产配置说明和闭环自动化验收脚本 | 等总负责人分发 `MF_EP_INTERNAL_TOKEN`、DataCenter 自身内部令牌和身份密钥 |

注意：

```text
真实模型 API Key 只属于 MF_AgentService。
MF_EP 和 MF_DataCenter 不保存、不接收、不配置模型 API Key。
```

## 2. 密钥与变量分发表

### 2.1 总负责人统一生成

| 变量 | 用途 | 接收方 | 主责项目 |
| --- | --- | --- | --- |
| `MF_EP_INTERNAL_TOKEN` | 访问 MF_EP 内部 AI 内容、同步事件、ack、failure、retry 接口 | MF_EP、MF_AgentService、MF_DataCenter | MF_EP |
| `MF_DATACENTER_INTERNAL_TOKEN` | 调用 DataCenter 内部接口 | MF_DataCenter、MF_EP、必要时 MF_AgentService | MF_DataCenter |
| `MF_DATACENTER_IDENTITY_SECRET` | DataCenter 内部身份签名/校验 | MF_DataCenter、MF_EP | MF_DataCenter |
| `MF_AGENT_KNOWLEDGE_SYNC_KEY` | AgentService 人工同步、状态查询、失败重试接口 | MF_AgentService，必要时部署/运维负责人 | MF_AgentService |

### 2.2 部署环境提供

| 变量 | 用途 | 注入项目 |
| --- | --- | --- |
| `MF_DB_URL` | MF_EP 生产 MySQL 地址 | MF_EP |
| `MF_DB_USERNAME` | MF_EP 生产 MySQL 用户 | MF_EP |
| `MF_DB_PASSWORD` | MF_EP 生产 MySQL 密码 | MF_EP |
| `MF_REDIS_HOST` | MF_EP 生产 Redis 地址 | MF_EP |
| `MF_REDIS_PORT` | MF_EP 生产 Redis 端口 | MF_EP |
| `MF_REDIS_PASSWORD` | MF_EP 生产 Redis 密码 | MF_EP |
| `MF_EP_BASE_URL` | MF_EP 服务内网地址 | MF_AgentService |
| `MF_DATACENTER_BASE_URL` | DataCenter 服务内网地址 | MF_EP、MF_AgentService |

### 2.3 只交给 AgentService 负责人

| 变量 | 用途 | 备注 |
| --- | --- | --- |
| `MF_AGENT_LLM_ENABLED=true` | 启用真实模型调用 | 只在确认 API Key 可用后开启 |
| `DEEPSEEK_API_KEY` | DeepSeek 模型 API Key | 不写入 Git、YAML、脚本、文档 |
| `OPENAI_API_KEY` | OpenAI 兼容模型 API Key | 如果不用 OpenAI，可不配置 |
| `MF_AGENT_OPENAI_BASE_URL` | OpenAI 兼容接口地址 | 视模型供应商而定 |
| `MF_AGENT_OPENAI_MODEL` | 模型名称 | 视模型供应商而定 |

## 3. 各项目注入清单

### 3.1 MF_EP

MF_EP 负责人注入：

```text
SPRING_PROFILES_ACTIVE=prod
MF_DB_URL=<生产 MySQL 地址>
MF_DB_USERNAME=<生产 MySQL 用户>
MF_DB_PASSWORD=<生产 MySQL 密码>
MF_REDIS_HOST=<生产 Redis 地址>
MF_REDIS_PORT=<生产 Redis 端口>
MF_REDIS_PASSWORD=<生产 Redis 密码>
MF_EP_INTERNAL_TOKEN=<总负责人分发>
MF_DATACENTER_BASE_URL=<DataCenter 内网地址>
MF_DATACENTER_INTERNAL_TOKEN=<总负责人分发>
MF_DATACENTER_IDENTITY_SECRET=<总负责人分发>
```

迁移要求：

```text
1. ai-content-draft-v1.sql
2. ai-content-governance-v2.sql
3. ai-content-governance-v3.sql
4. ai-content-sync-governance-v4.sql
```

迁移只允许对目标库单次执行，不得重复执行同一脚本。

### 3.2 MF_AgentService

AgentService 负责人注入：

```text
SPRING_PROFILES_ACTIVE=prod
MF_EP_BASE_URL=<MF_EP 内网地址>
MF_DATACENTER_BASE_URL=<DataCenter 内网地址>
MF_EP_INTERNAL_TOKEN=<总负责人分发，必须与 MF_EP 一致>
MF_AGENT_KNOWLEDGE_SYNC_KEY=<总负责人分发>
MF_AGENT_KNOWLEDGE_SYNC_POLL_INTERVAL=30s
MF_AGENT_KNOWLEDGE_SYNC_MAX_RETRIES=5
MF_AGENT_KNOWLEDGE_SYNC_PENDING_TIMEOUT=5m
```

真实模型配置：

```text
MF_AGENT_LLM_ENABLED=true
DEEPSEEK_API_KEY=<AgentService 负责人持有并轮换>
```

如使用 OpenAI 兼容模型，再补：

```text
OPENAI_API_KEY=<AgentService 负责人持有并轮换>
MF_AGENT_OPENAI_BASE_URL=<模型服务地址>
MF_AGENT_OPENAI_MODEL=<模型名称>
```

### 3.3 MF_DataCenter

DataCenter 负责人注入：

```text
SPRING_PROFILES_ACTIVE=prod
MF_EP_INTERNAL_TOKEN=<总负责人分发，必须与 MF_EP 一致>
MF_DATACENTER_INTERNAL_TOKEN=<总负责人分发>
MF_DATACENTER_IDENTITY_SECRET=<总负责人分发>
MF_EP_BASE_URL=<MF_EP 内网地址>
```

DataCenter 不配置模型 API Key。

## 4. 分发顺序

建议顺序：

```text
1. 总负责人生成内部 token 和 DataCenter 身份密钥。
2. MF_EP 负责人注入生产数据库、Redis、MF_EP_INTERNAL_TOKEN 和 DataCenter 内部凭据。
3. MF_EP 负责人按 V1-V4 顺序执行生产库迁移。
4. MF_DataCenter 负责人注入 MF_EP_INTERNAL_TOKEN 和自身内部凭据。
5. MF_AgentService 负责人注入 MF_EP_INTERNAL_TOKEN、MF_AGENT_KNOWLEDGE_SYNC_KEY 和模型 API Key。
6. 三服务统一以 prod Profile 启动。
7. 总负责人组织生产复验。
```

不要先启动 AgentService 再临时补 token；事件消费服务应在完整环境变量存在后启动。

## 5. 生产复验步骤

### 5.1 基础健康检查

检查：

```text
MF_EP /actuator/health
MF_AgentService /actuator/health
MF_DataCenter /api/system/status
DataCenter check-health.ps1 或等价生产健康检查
```

### 5.2 内部 token 校验

必须验证：

```text
使用真实 MF_EP_INTERNAL_TOKEN 可访问 MF_EP 内部事件接口
使用 change-me 不能访问 MF_EP 内部事件接口
使用错误 token 不能访问 AgentService 同步状态/重试接口
```

### 5.3 正常闭环复验

由 DataCenter 执行：

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_DataCenter
.\scripts\test-knowledge-closed-loop.ps1 -ConfirmRun -MfEpInternalToken $env:MF_EP_INTERNAL_TOKEN
```

验收标准：

```text
低风险候选创建成功
MF_EP 草稿创建成功
publish 事件 acknowledged
AgentService 发布后命中
offline 事件 acknowledged
AgentService 下线后不再命中
测试 FAQ 最终 is_published=0
```

### 5.4 故障注入复验

按已验证路径执行：

```text
MF_EP 事件 pending
-> 注入 failure failed
-> MF_EP retry 恢复 pending
-> AgentService retry/轮询再消费
-> acknowledged
-> 发布后命中
-> 下线后不再命中
```

验收标准：

```text
failure_attempts > 0
retry_count > 0
last_failure_reason 保留
最终 delivery_status=acknowledged
consumer=mf-agent-service
```

## 6. 放行条件

全部满足后，才可标记真实生产上线验收通过：

```text
三服务均以 prod Profile 启动
默认 change-me 不再可用
MF_EP V1-V4 迁移已落生产库
正常发布/下线/ack 闭环通过
失败回写 -> retry -> ack 通过
DataCenter 审计记录完整
AgentService 真实模型 Key 仅存在部署环境，不存在源码、YAML、脚本和文档
测试内容最终下线
```

## 7. 不允许事项

```text
不得把真实密钥写入 Git
不得把真实密钥写入 application-prod.yml
不得把真实密钥写入 PowerShell 脚本
不得把真实密钥写入交接文档
不得让 MF_EP 或 DataCenter 保存模型 API Key
不得继续使用 change-me 做生产内部令牌
不得绕过 MF_EP 事件表直接把 internal-sync 当正常发布链路
```

