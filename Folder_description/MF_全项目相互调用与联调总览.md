# MF 全项目相互调用与联调总览

更新时间：2026-07-06  
适用对象：总负责人、联调负责人、部署负责人、后续接手 Agent

## 1. 总体关系

当前 `F:\20260518-xiangmu\MF_Project` 已形成一个多项目协作体系：

```text
MF_Website
生态品牌官网，承载苗丰精灵入口
        │
        ▼
MF_Pet
苗丰精灵前端 runtime / 桌宠 / 交互层
        │
        ▼
MF_AgentService
智能客服 Agent 服务，提供 /api/agent/chat
        │
        ├──────────────► MF_EP
        │                 电商主业务系统，提供商品、百科、订单、商家等业务能力
        │
        └──────────────► MF_DataCenter
                          数据中台，沉淀咨询日志、工具调用、问题池、样本池

MF_DataCenter ─────────► MF_EP 数据库 fertilizer
只读读取业务数据，生成指标、治理、质量检查和运营看板
```

一句话理解：

> MF_EP 负责业务数据和交易闭环；MF_DataCenter 负责数据治理与分析；MF_AgentService 负责 AI 客服和工具调用；MF_Pet 负责苗丰精灵前端交互；MF_Website 是品牌入口和精灵入口。

## 2. 项目职责边界

| 项目 | 主要职责 | 不应该做 |
| --- | --- | --- |
| `MF_EP` | 电商主业务、商品、百科、订单、用户、商家、管理端、客户端、商家端 | 不承担数据中台治理；不直接实现苗丰精灵 AI 编排 |
| `MF_DataCenter` | 指标、快照、数据质量、源表契约、AI 咨询日志、问题池、样本池 | 不修改 MF_EP 业务表；不替代 MF_EP 管理后台 |
| `MF_AgentService` | 通用客服 Agent、规则路由、工具调用、RAG 预留、写入 DataCenter | 不直接操作数据库；不做退款、改订单、商家审核等高风险动作 |
| `MF_Pet` | 苗丰精灵 runtime、状态机、气泡、面板、桌面壳、Web 接入 API | 不实现业务查询逻辑；不直接访问 MF_EP / DataCenter 数据库 |
| `MF_Website` | 品牌官网、生态叙事、苗丰精灵入口、商城/百科/商家入口 | 不承载完整交易闭环；不复制 MF_Pet runtime 源码 |

## 3. 服务端口与地址

| 系统 | 服务 | 地址 / 端口 |
| --- | --- | --- |
| `MF_EP` | 后端 API | `http://127.0.0.1:8080` |
| `MF_EP` | 管理端 | 通常 `http://localhost:5173` |
| `MF_EP` | 客户端 | Vite 自动端口，建议后续固定 |
| `MF_EP` | 商家端 | Vite 自动端口，建议后续固定 |
| `MF_DataCenter` | 后端 API | `http://127.0.0.1:8091` |
| `MF_DataCenter` | 前端 Web | `http://127.0.0.1:5176` |
| `MF_AgentService` | Agent API | `http://127.0.0.1:8092` |
| `MF_Pet` | Web Demo | `http://127.0.0.1:5179/demo/clawd-style-demo.html` |

建议下一步总负责人统一固定所有 Vite 前端端口，避免多个项目默认抢 `5173`。

## 4. 数据库关系

| 数据库 | 所属系统 | 读写关系 |
| --- | --- | --- |
| `fertilizer` | `MF_EP` | MF_EP 读写；DataCenter 只读 |
| `mf_datacenter` | `MF_DataCenter` | DataCenter 读写；AgentService 通过 API 写入 |

关键原则：

- `MF_EP` 是业务数据源。
- `MF_DataCenter` 只读 `fertilizer`，不要反向改业务表。
- `MF_DataCenter` 自己的数据写入 `mf_datacenter`。
- `MF_AgentService` 通过 DataCenter HTTP API 写咨询日志、工具调用、未解决问题和样本候选。
- `MF_Pet` 和 `MF_Website` 不直接访问数据库。

## 5. 核心调用链路

### 5.1 苗丰精灵咨询链路

```text
用户
  │
  ▼
MF_Website / MF_Pet
  │  用户输入问题
  ▼
POST http://localhost:8092/api/agent/chat
  │
  ▼
MF_AgentService
  ├─ 意图识别
  ├─ 工具选择
  ├─ RAG / 轻量知识检索
  ├─ 回答生成
  └─ 日志沉淀
      │
      ├─ 查询 MF_EP：商品、百科、订单、商家入驻说明
      └─ 写入 MF_DataCenter：咨询日志、工具调用、未解决问题、样本候选
```

### 5.2 DataCenter 指标链路

```text
MF_EP 后端 / 前端业务操作
        │
        ▼
fertilizer 业务库
        │  DataCenter 只读
        ▼
MF_DataCenter datacenter-api
        ├─ 源表契约检查
        ├─ 指标快照
        ├─ 数据质量检查
        ├─ 治理可信状态
        └─ 运营看板
        │
        ▼
MF_DataCenter datacenter-web
```

### 5.3 Agent 数据沉淀链路

```text
MF_AgentService
        │
        ├─ datacenter.logConversation
        ├─ datacenter.logToolCall
        ├─ datacenter.reportUnresolved
        └─ datacenter.saveSampleCandidate
        │
        ▼
MF_DataCenter / mf_datacenter
        ├─ AI 咨询分析
        ├─ 问题池
        └─ 样本池
```

### 5.4 MF_Pet 状态反馈链路

```text
MF_Pet 用户输入
        │
        ▼
MF_AgentService /api/agent/chat
        │
        ▼
返回 answer / intent / usedTools / resolved / fallbackReason
        │
        ▼
MF_Pet 根据结果切换状态
        ├─ working / thinking：正在处理
        ├─ success：回答成功
        ├─ error：请求失败
        ├─ doubt：需要澄清
        └─ idle / resting：默认或休息
```

## 6. 关键 API

### 6.1 MF_AgentService 聊天接口

```http
POST http://localhost:8092/api/agent/chat
Content-Type: application/json
```

请求示例：

```json
{
  "sessionId": "demo-session",
  "message": "推荐几款适合果树的肥料",
  "userId": "1001",
  "userType": "client",
  "authToken": ""
}
```

响应关注字段：

```json
{
  "answer": "Agent 返回给用户的回答",
  "intent": "product",
  "resolved": true,
  "usedTools": [],
  "conversationId": 42,
  "fallbackReason": null
}
```

接入方：

- `MF_Pet` 聊天面板。
- `MF_Website` 苗丰精灵入口。
- 后续 `MF_EP` 客户端内置 AI 助手。

### 6.2 MF_DataCenter 健康与治理接口

```text
GET http://127.0.0.1:8091/api/system/status
GET http://127.0.0.1:8091/api/source/check
GET http://127.0.0.1:8091/api/dashboard/overview
```

健康检查脚本：

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_DataCenter
.\scripts\check-health.ps1
```

正常情况下治理状态应为：

```text
GovernanceStatus : trusted
```

## 7. 推荐联调启动顺序

### 7.1 基础依赖

1. 启动 MySQL `MySQL80`。
2. 确认数据库存在：

```sql
CREATE DATABASE IF NOT EXISTS fertilizer DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS mf_datacenter DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

3. 启动 Redis：

```powershell
& "E:\Program Files\Redis\redis-server.exe" --port 6379 --requirepass 123456
```

### 7.2 启动 MF_EP

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_EP\projects\mf\mf-fertilizer
mvn -pl fertilizer-api -am spring-boot:run
```

确认：

```text
http://127.0.0.1:8080
```

### 7.3 启动 MF_DataCenter

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_DataCenter
.\scripts\start-local.ps1
.\scripts\check-health.ps1
```

确认：

```text
http://127.0.0.1:8091/api/system/status
http://127.0.0.1:5176
```

### 7.4 启动 MF_AgentService

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_AgentService
mvn spring-boot:run
```

确认：

```text
http://localhost:8092/actuator/health
```

### 7.5 启动 MF_Pet Demo

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_Pet
python -m http.server 5179
```

访问：

```text
http://127.0.0.1:5179/demo/clawd-style-demo.html
```

### 7.6 启动 MF_Website / MF_EP 前端

按需要启动：

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_Website\mf_Website
npm run dev

cd F:\20260518-xiangmu\MF_Project\MF_EP\projects\mf\mf-frontend
npm run dev

cd F:\20260518-xiangmu\MF_Project\MF_EP\projects\mf\mf-frontend-client
npm run dev

cd F:\20260518-xiangmu\MF_Project\MF_EP\projects\mf\mf-frontend-merchant
npm run dev
```

## 8. 端到端验收路径

### 8.1 电商 + 数据中台验收

1. 登录 MF_EP 管理端：`admin / admin123`。
2. 确认商品、订单、商家、用户等业务页面可访问。
3. 打开 MF_DataCenter。
4. 执行源表契约检查。
5. 刷新小时或日指标快照。
6. 执行数据质量检查。
7. 打开 DataCenter 首页，确认治理状态为 `trusted`。

### 8.2 Agent + DataCenter 验收

1. 启动 `MF_DataCenter`。
2. 启动 `MF_EP`。
3. 启动 `MF_AgentService`。
4. 调用商品问题：

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8092/api/agent/chat" `
  -ContentType "application/json" `
  -Body '{"sessionId":"demo-session","message":"推荐几款适合果树的肥料","userId":"1001","userType":"client","authToken":""}'
```

5. 打开 DataCenter AI 咨询分析、问题池、样本池，确认数据写入。

### 8.3 Pet + Agent 验收

1. 启动 `MF_AgentService`。
2. 启动 `MF_Pet` demo 或接入页面。
3. 在宠物聊天面板输入商品/百科问题。
4. 前端调用 `/api/agent/chat`。
5. 显示返回 answer。
6. 根据返回状态展示 working、success、error、doubt 等状态。

### 8.4 Website + Pet + Agent 验收

1. 启动 `MF_Website`。
2. 点击官网导航中的苗丰精灵入口。
3. 唤出 `MF_Pet` runtime。
4. 输入问题并调用 `MF_AgentService`。
5. 返回结果展示在苗丰精灵面板中。
6. DataCenter 看到咨询日志沉淀。

## 9. 安全边界

必须保持：

- `MF_AgentService` 不直接连业务数据库。
- `MF_Pet` 不直接访问业务接口或数据库，只调用 AgentService。
- `MF_DataCenter` 不修改 `fertilizer` 业务库。
- 订单查询必须传用户登录 token。
- 无 token 时，Agent 只能提示登录，不能泄露订单数据。
- Agent 不执行退款、改订单、取消订单、确认收货、商家审核等动作。
- DataCenter 暂无独立登录鉴权，不适合直接暴露到公网。

## 10. 当前已知限制

- `MF_AgentService` 真实 LLM 默认关闭，`MF_AGENT_LLM_ENABLED=false`。
- `MF_AgentService` 当前主要是规则路由，不是完整大模型自主工具选择。
- RAG 当前是轻量本地知识检索，不是生产级向量库。
- `MF_Pet` 使用 clawd GIF 作为开发占位，不是最终 MF 正式素材。
- `MF_Pet` 与 `MF_AgentService` 尚需做真实前端接入联调。
- `MF_Website` 接入 `MF_Pet` 仍是下一步重点。
- `MF_DataCenter` 暂无独立登录权限。
- 各前端端口建议统一固定。

## 11. 下一步统筹优先级

1. 固定端口表和启动脚本。
2. 完成 `MF_Pet -> MF_AgentService -> MF_DataCenter` 聊天闭环。
3. 完成 `MF_Website -> MF_Pet` 官网入口接入。
4. 完成 `MF_AgentService` 开启真实 LLM 的联调。
5. 将 RAG 数据源从本地轻量知识扩展到 MF_EP 百科/FAQ/文章。
6. 为 DataCenter 增加基础登录权限。
7. 替换 MF_Pet 正式素材。
8. 规划是否将工具层从 `MF_AgentService` 抽离为独立 `MF_MCP_Server`。

