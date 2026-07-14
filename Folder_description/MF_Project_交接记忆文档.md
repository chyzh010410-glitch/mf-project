# MF Project 交接记忆文档

更新时间：2026-07-13  
适用对象：后续总负责人、各项目负责人、接手 Agent、联调负责人

## 1. 接手原则

这是 `MF_Project` 的项目级交接记忆文档。接手时不要直接全仓扫描、不要直接重构、不要直接清理未提交文件。

先按以下顺序理解项目：

```text
1. Folder_description\MF_Project_交接记忆文档.md
2. Folder_description\MF_Project_统筹负责人记忆文档.md
3. Folder_description\MF_全项目相互调用与联调总览.md
4. Folder_description\MF_Project_端口规划与联调启动说明.md
5. Folder_description\MF_Project_总负责人联调验收与优化顺序建议.md
```

当前项目已经进入多项目联调和体验完善阶段，不是单个后端或单个前端项目。

## 2. 当前项目定位

`MF_Project` 是围绕“苗丰”品牌形成的 AI 应用型电商系统。

核心结构：

```text
MF_EP            主业务系统，负责电商、商品、百科、订单、用户、商家
MF_DataCenter    数据中台，负责指标、质量、治理、AI 咨询沉淀
MF_AgentService  智能客服 Agent，负责意图识别、工具调用、回答生成
MF_Pet           苗丰精灵交互层，负责前端表现、状态、聊天入口
MF_Website       品牌官网，负责官网入口和苗丰精灵入口
MF_Logo          官方品牌资产
```

一句话定位：

> MF 是以苗木肥料垂直电商为业务底座，融合品牌官网、苗丰精灵、智能客服 Agent、数据中台和后续 RAG 能力的 AI 应用型电商平台。

不要把它描述成普通商城，也不要描述成孤立 AI Demo。

## 3. 当前 Git 状态记忆

远程仓库：

```text
origin https://github.com/chyzh010410-glitch/mf-project.git
```

当前主分支：

```text
master
```

本地和远程关系：

```text
master...origin/master
```

重要注意：

- 当前本地存在大量未提交改动。
- 不要执行 `git reset --hard`。
- 不要执行 `git checkout -- .`。
- 不要删除未确认的新增文件。
- 后续接手时必须先运行 `git status --short --branch`，确认当前工作区。

推荐协作方式：

```text
master                 稳定主线
feature/pet-*          MF_Pet 相关任务
feature/agent-*        MF_AgentService 相关任务
feature/data-*         MF_DataCenter 相关任务
feature/website-*      MF_Website 相关任务
feature/ep-*           MF_EP 相关任务
```

不要多人直接在 `master` 上开发。每个任务从 `master` 拉功能分支，完成后推送远程并通过 PR 合并回 `master`。

## 4. 固定端口与启动方式

固定端口：

| 系统 | 服务 | 端口 |
| --- | --- | --- |
| MF_EP | 后端 API | `8080` |
| MF_EP | 管理端 | `5173` |
| MF_EP | 客户端 | `5174` |
| MF_EP | 商家端 | `5175` |
| MF_DataCenter | 后端 API | `8091` |
| MF_DataCenter | 前端 Web | `5176` |
| MF_Website | 官网 | `5178` |
| MF_Pet | Web Demo | `5179` |
| MF_AgentService | Agent API | `8092` |

统一启动：

```powershell
cd F:\20260518-xiangmu\MF_Project
.\scripts\start-mf-closed-loop.ps1 -WithWebsite -WithPet -WithEpFrontends
```

检查端口：

```powershell
.\scripts\check-mf-ports.ps1
```

停止本地项目进程：

```powershell
.\scripts\stop-mf-local.ps1
```

默认不要停止 MySQL 和 Redis。确实需要停止 Redis 时再使用：

```powershell
.\scripts\stop-mf-local.ps1 -IncludeDependencies
```

## 5. 已打通的核心闭环

当前目标闭环：

```text
MF_Website
  -> MF_Pet
  -> MF_AgentService /api/agent/chat
  -> MF_EP 商品/百科/订单等业务能力
  -> MF_DataCenter 咨询日志/问题池/样本池
```

已验证的基础行为：

- `MF_EP` 后端可作为业务能力来源。
- `MF_AgentService` 可接收 `/api/agent/chat` 请求。
- 商品类问题可走 `product.search`。
- 咨询记录可写入 `MF_DataCenter`。
- 无 token 的订单查询不应泄露订单隐私。

后续验收时不要只看服务是否启动，还要验证数据是否真正写入 DataCenter。

## 6. 项目边界

### 6.1 MF_EP

职责：

- 主业务系统。
- 管商品、百科、订单、用户、商家、三端前端。
- 持有 `fertilizer` 业务库。

边界：

- 不承担数据中台治理。
- 不直接实现苗丰精灵 AI 编排。
- 不建议在当前阶段重构订单、支付、商家审核、权限等高风险逻辑。

注意：

- Redis 依赖 `127.0.0.1:6379`，密码默认 `123456`。
- 数据库默认 `fertilizer`。
- 配置文件中敏感项优先保留环境变量形式，例如 `${MYSQL_PASSWORD:123456}`。

### 6.2 MF_DataCenter

职责：

- 数据中台。
- 负责指标、快照、数据质量、源表契约、治理状态。
- 接收 Agent 咨询日志、工具调用、问题池、样本池。
- 自有库为 `mf_datacenter`。

边界：

- 只读 `fertilizer`。
- 不反向修改 MF_EP 业务表。
- 当前不适合直接暴露公网，因为基础登录权限仍需完善。

注意：

- `GovernanceStatus = risk` 不一定代表系统不可用，可能是快照过期等治理信号。
- 判断 DataCenter 是否正常，应同时看接口、页面、源表契约、AI 数据沉淀。

### 6.3 MF_AgentService

职责：

- 智能客服 Agent。
- 暴露 `POST /api/agent/chat`。
- 调用 MF_EP 获取业务数据。
- 调用 MF_DataCenter 写入日志、问题池、样本池。
- 当前是 V1 客服 Agent，不是完整多 Agent 平台。

边界：

- 不直接操作数据库。
- 不执行退款、改订单、取消订单、确认收货、商家审核等高风险动作。
- 订单查询必须检查用户身份或 token。

注意：

- 真实 LLM 默认关闭。
- `MF_AGENT_LLM_ENABLED=false` 时仍应能以规则路由、工具调用、轻量 RAG 运行。
- 不要一开始拆独立 `MF_MCP_Server`，当前工具层保留在 AgentService 内部即可。

### 6.4 MF_Pet

职责：

- 苗丰精灵 runtime。
- 负责桌宠、气泡、状态机、聊天面板、交互反馈。

边界：

- 不直接查数据库。
- 不直接访问 MF_EP 或 DataCenter 的底层数据。
- 通过 MF_AgentService 获得 AI 和业务能力。

下一步重点：

- 真正接入 `/api/agent/chat`。
- 根据 Agent 返回结果展示 thinking、success、error、doubt 等状态。
- 逐步替换 clawd GIF 占位素材，形成正式 MF 视觉。

### 6.5 MF_Website

职责：

- 品牌官网。
- 承载苗丰精灵入口。
- 承接商城、百科、商家入驻、智能咨询入口。

边界：

- 不承载完整交易闭环。
- 不复制 MF_Pet runtime 源码。
- 不要做成和主业务无关的纯环保展示页。

下一步重点：

- 官网导航接入苗丰精灵。
- 从官网唤出 MF_Pet。
- 让官网成为完整体验入口。

### 6.6 MF_Logo

职责：

- 作为官方 logo 源。

边界：

- 各项目应复制到自己的 `assets/brand` 目录。
- 不要跨项目直接引用 logo 源路径。

## 7. 下一步优先级

推荐顺序：

```text
1. MF_Pet 接入 MF_AgentService
2. MF_Website 接入 MF_Pet
3. MF_AgentService 回答质量和意图识别优化
4. MF_DataCenter AI 观察台、问题池、样本池增强
5. MF_EP 三端体验和业务接口稳定性整理
```

理由：

- 底座已经能跑，当前最缺用户可感知的完整体验。
- 先把体验链路打通，展示价值最大。
- MF_EP 和 DataCenter 应以稳定为主，不应先做高风险大改。

## 8. 验收清单

### 8.1 启动验收

```powershell
.\scripts\check-mf-ports.ps1
```

至少应看到：

```text
8080 Listening
8091 Listening
8092 Listening
5176 Listening
5178 Listening
5179 Listening
```

如果启动了 MF_EP 三端前端，还应看到：

```text
5173 Listening
5174 Listening
5175 Listening
```

### 8.2 接口验收

```text
http://127.0.0.1:8080/doc.html
http://127.0.0.1:8091/api/system/status
http://127.0.0.1:8092/actuator/health
```

### 8.3 Agent 闭环验收

调用：

```http
POST http://127.0.0.1:8092/api/agent/chat
```

示例请求：

```json
{
  "sessionId": "handoff-demo",
  "message": "推荐几款适合果树的肥料",
  "userId": "1001",
  "userType": "client",
  "authToken": ""
}
```

关注：

- 是否返回 `answer`。
- `intent` 是否合理。
- `usedTools` 是否包含业务工具。
- 是否写入 DataCenter。
- DataCenter AI 分析、问题池、样本池是否能看到记录。

## 9. 常见风险

1. 直接在 `master` 开发，导致多人改动混杂。
2. 未确认就清理本地未提交文件。
3. DataCenter 反向修改 `fertilizer`。
4. MF_Pet 绕过 AgentService 直接查业务接口或数据库。
5. AgentService 默认开启真实 LLM，带来成本和密钥风险。
6. 把 clawd GIF 当成最终正式宠物资产。
7. 把 DataCenter 暴露公网但未加登录权限。
8. 重构 MF_EP 高风险业务逻辑，破坏既有闭环。

## 10. 给后续负责人的执行建议

接手当天先做三件事：

```text
1. 看文档，确认边界。
2. 看 git status，确认未提交改动。
3. 启动闭环，验证 Agent 到 DataCenter 的真实写入。
```

然后按任务创建分支：

```powershell
git checkout master
git pull origin master
git checkout -b feature/pet-agent-chat
```

不要把多个大任务混在一个分支里。推荐按功能拆分：

```text
feature/pet-agent-chat
feature/website-pet-entry
feature/agent-answer-polish
feature/datacenter-ai-view
feature/ep-client-ai-entry
```

完成后推送远程并通过 PR 合并回 `master`。

## 11. 当前总判断

`MF_Project` 当前已经具备系统雏形：

```text
业务系统可运行
数据中台可沉淀
Agent 服务可调用
苗丰精灵可演示
官网入口可访问
端口和脚本已统一
```

下一阶段应聚焦：

```text
把可运行系统打磨成可体验产品
```

具体就是：

```text
MF_Website -> MF_Pet -> MF_AgentService -> MF_EP / MF_DataCenter
```

这条链路稳定后，再做真实 LLM、RAG 增强、DataCenter 权限、正式部署和多 Agent 拆分。
