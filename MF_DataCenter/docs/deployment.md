# MF_DataCenter 本地落地运行

## 前置条件

- Windows 上已启动系统 MySQL 服务 `MySQL80`。
- MySQL 账号为 `root / 123456`。
- 系统 MySQL 中存在 `mf_datacenter` 和 `fertilizer` 两个库；没有时可先创建。
- 后端 local profile 会连接系统 MySQL 的 `127.0.0.1:3306`。

## 启动

在 `MF_DataCenter` 目录执行：

```powershell
.\scripts\start-local.ps1
```

启动后访问：

- 后端健康：`http://127.0.0.1:8091/api/system/status`
- 前端页面：`http://127.0.0.1:5176`
- 指标治理：`http://127.0.0.1:5176/metric-governance`
- 数据质量：`http://127.0.0.1:5176/data-quality`

## 健康检查

```powershell
.\scripts\check-health.ps1
```

检查项包括：

- 后端系统状态接口。
- 指标字典数量。
- 数据质量汇总。
- 源表契约异常表和缺失字段数量。
- 运营总览治理可信状态。
- 前端指标治理页面是否可访问。

## 停止

```powershell
.\scripts\stop-local.ps1
```

该脚本只停止 `8091` 和 `5176` 上的 DataCenter 进程，不停止系统 MySQL。

## 生产打包

```powershell
.\scripts\package-prod.ps1
```

产物位置：

- `dist\datacenter-api.jar`
- `dist\web`

## 生产 API 启动

```powershell
.\scripts\start-prod-api.ps1
```

生产 profile 使用 `application-prod.yml`，支持通过环境变量覆盖：

- `DATACENTER_DB_URL`
- `DATACENTER_DB_USERNAME`
- `DATACENTER_DB_PASSWORD`
- `MF_DATACENTER_INTERNAL_TOKEN`
- `MF_DATACENTER_IDENTITY_SECRET`
- `MF_EP_BASE_URL`
- `MF_EP_INTERNAL_TOKEN`
- `DATACENTER_SCHEDULER_ENABLED`

完整变量清单见 `.env.production.example`。该文件只有变量名和占位符，不能填入或提交真实值。

责任边界：

- `MF_DATACENTER_INTERNAL_TOKEN`、`MF_DATACENTER_IDENTITY_SECRET` 由 DataCenter 负责人在本服务部署环境配置。
- `MF_EP_INTERNAL_TOKEN` 由 MF_EP 负责人签发，DataCenter 仅在部署环境注入后用于调用 MF_EP 内部内容接口。
- `MF_AGENT_KNOWLEDGE_SYNC_KEY` 属于 MF_AgentService 的人工故障处置凭据，不属于 DataCenter 常规生产注入项；正常发布/下线链路不调用该接口。
- 不在 DataCenter 配置任何 `DEEPSEEK_API_KEY` 或 `OPENAI_API_KEY`；模型密钥仅属于 MF_AgentService。

前端 `dist\web` 可交给 Nginx、IIS 或任意静态文件服务器托管，并把 `/api` 代理到 `http://127.0.0.1:8091`。

## 备份建议

至少备份：

- `mf_datacenter`：中台元数据、快照、质量规则、问题闭环。
- `fertilizer`：MF_EP 业务源库，由业务系统备份策略负责。
- `datacenter-api.log`：生产 API 运行日志。

## 数据库迁移

后端启动时由 Flyway 自动迁移 `mf_datacenter`。当前迁移包含：

- `V1` 到 `V3`：基础表和指标快照粒度。
- `V4`：指标字典和质量检查结果。
- `V5`：质量规则和质量问题闭环。

## 注意

- 不再使用项目内临时 MySQL。
- `MF_EP` 源库 `fertilizer` 只读使用，DataCenter 只写 `mf_datacenter`。生产默认关闭直接源库读取；如需开启，必须使用独立只读账号并配置 `MF_EP_DATASOURCE_ENABLED=true`。
- 如果 `fertilizer` 缺少业务表，快照刷新可能失败，但指标字典、质量规则、问题闭环等 DataCenter 自有能力仍可运行。
