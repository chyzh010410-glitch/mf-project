# MF Project 端口规划与联调启动说明

更新时间：2026-07-06

## 固定端口

| 系统 | 服务 | 端口 | 说明 |
| --- | --- | --- | --- |
| MF_EP | 后端 API | `8080` | 主业务接口 |
| MF_EP | 管理端 | `5173` | 管理后台前端 |
| MF_EP | 客户端 | `5174` | 客户商城前端 |
| MF_EP | 商家端 | `5175` | 商家工作台前端 |
| MF_DataCenter | 后端 API | `8091` | 数据中台接口 |
| MF_DataCenter | 前端 Web | `5176` | 数据中台页面 |
| MF_Website | 官网 | `5178` | 品牌官网，预留 `5177` 给后续网关或临时调试 |
| MF_Pet | Web Demo | `5179` | 苗丰精灵 demo |
| MF_AgentService | Agent API | `8092` | 智能客服接口 |

前端 Vite 项目使用 `strictPort`，端口冲突时直接失败，避免自动漂移到不可预期端口。

## 统一脚本

在项目根目录执行：

```powershell
cd F:\20260518-xiangmu\MF_Project
.\scripts\check-mf-ports.ps1
.\scripts\stop-mf-local.ps1
.\scripts\start-mf-closed-loop.ps1
```

启动官网和苗丰精灵 demo：

```powershell
.\scripts\start-mf-closed-loop.ps1 -WithWebsite -WithPet
```

启动 MF_EP 三个前端：

```powershell
.\scripts\start-mf-closed-loop.ps1 -WithEpFrontends
```

停止本地 MF 项目进程：

```powershell
.\scripts\stop-mf-local.ps1
```

默认只停止 MF 项目端口，不停止 MySQL 和 Redis。确实需要同时停止 Redis 时：

```powershell
.\scripts\stop-mf-local.ps1 -IncludeDependencies
```

## 闭环验证顺序

1. 启动 MySQL 和 Redis。
2. 启动 MF_EP 后端 `8080`。
3. 启动 MF_DataCenter `8091/5176`。
4. 启动 MF_AgentService `8092`。
5. 调用 `POST http://127.0.0.1:8092/api/agent/chat`。
6. 打开 DataCenter AI 咨询分析、问题池、样本池确认数据沉淀。

## 边界

- MF_DataCenter 只读 `fertilizer`，不反向修改业务库。
- MF_AgentService 通过 HTTP 调用 MF_EP 和 MF_DataCenter，不直接连业务数据库。
- MF_Pet 和 MF_Website 不直接访问数据库。
- 无用户 token 时，Agent 不返回订单隐私数据。
