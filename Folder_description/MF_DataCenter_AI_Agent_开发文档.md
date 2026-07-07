# MF_DataCenter 开发文档

## 1. 给接手 Agent 的任务说明

你负责开发 `MF_DataCenter`，即苗丰轻量级数据中台。

当前阶段不是做完整企业级大数据平台，而是先完成一个能运行、能展示、能沉淀 AI 咨询数据的 V1。

请先阅读：

```text
F:\20260518-xiangmu\MF_Project\MF_DataCenter\README.md
F:\20260518-xiangmu\MF_Project\MF_DataCenter\docs\product-scope.md
F:\20260518-xiangmu\MF_Project\Folder_description\MF_DataCenter_企业级架构文档.md
```

如需理解主业务系统，再按需阅读：

```text
F:\20260518-xiangmu\MF_Project\MF_EP\projects\mf\项目总结.md
F:\20260518-xiangmu\MF_Project\MF_EP\projects\mf\memory\agent-handoff.md
```

不要一开始全仓库乱扫，也不要进入 `node_modules`、`dist`、`target`、`venv`。

## 2. 开发目标

在 `F:\20260518-xiangmu\MF_Project\MF_DataCenter` 下建设：

```text
MF_DataCenter
├── datacenter-api
├── datacenter-web
├── docs
└── README.md
```

V1 必须实现：

1. 数据中台后端骨架 `datacenter-api`。
2. 数据中台前端骨架 `datacenter-web`。
3. 运营总览页面和接口。
4. AI 咨询日志写入接口。
5. 未解决问题池接口和页面。
6. 样本候选池接口和页面。
7. 基础指标口径文档。

如果时间有限，优先完成后端接口和基础页面，不要追求复杂动效。

## 3. 技术选型

### 3.1 datacenter-api

建议：

- Java 17
- Spring Boot
- MyBatis-Plus
- MySQL
- Lombok
- Knife4j 或 OpenAPI

如果直接复制 `MF_EP` 的部分 Maven 配置，请保持最小化，不要把无关业务模块搬进数据中台。

### 3.2 datacenter-web

建议：

- Vue 3
- Vite
- Element Plus
- ECharts
- Axios
- Vue Router

页面风格应偏数据后台：清晰、密集、可扫描，不要做成营销页或官网风格。

## 4. 后端模块建议

推荐包结构：

```text
datacenter-api
└── src/main/java/com/mf/datacenter
    ├── DatacenterApplication.java
    ├── config
    ├── common
    ├── dashboard
    ├── analysis
    │   ├── product
    │   ├── content
    │   ├── merchant
    │   └── ai
    ├── conversation
    ├── unresolved
    ├── sample
    └── metric
```

推荐接口：

```text
GET  /api/dashboard/overview
GET  /api/analysis/products
GET  /api/analysis/content
GET  /api/analysis/merchants
GET  /api/analysis/ai
POST /api/ai/conversations
GET  /api/ai/conversations
POST /api/ai/unresolved-questions
GET  /api/ai/unresolved-questions
PATCH /api/ai/unresolved-questions/{id}/status
POST /api/ai/sample-candidates
GET  /api/ai/sample-candidates
PATCH /api/ai/sample-candidates/{id}/review
```

V1 可以先用模拟聚合数据或最小真实查询，但接口形状要稳定。

## 5. 数据库建议

V1 至少新增数据中台自身表：

```text
dc_ai_conversation_log
dc_ai_tool_call_log
dc_unresolved_question
dc_sample_candidate
```

可选：

```text
dc_metric_snapshot
```

建议新建 SQL 文件：

```text
MF_DataCenter/datacenter-api/src/main/resources/db/schema.sql
```

不要修改 `MF_EP` 的业务库结构来适配数据中台，除非用户明确要求。

### 5.1 咨询日志表

记录客服 Agent 每次咨询。

字段建议：

- id
- source
- session_id
- user_id
- user_type
- question
- answer
- intent
- resolved
- satisfaction
- create_time

### 5.2 未解决问题表

字段建议：

- id
- conversation_id
- question
- reason
- status
- owner
- remark
- create_time
- update_time

状态建议：

```text
pending
processing
resolved
ignored
```

### 5.3 样本候选表

字段建议：

- id
- conversation_id
- question
- answer
- source
- quality_status
- review_status
- reviewer
- review_remark
- create_time
- update_time

审核状态建议：

```text
pending
approved
rejected
```

## 6. 前端页面建议

推荐路由：

```text
/dashboard
/products
/content
/merchants
/ai-analysis
/unresolved-questions
/sample-candidates
```

### 6.1 运营总览

展示：

- 用户总数
- 商品总数
- 上架商品数
- 订单数
- GMV
- 商家总数
- 待审核商家数
- AI 咨询次数

图表：

- 近 7 日订单趋势
- 近 7 日 GMV 趋势
- 商品分类销售占比

### 6.2 商品分析

展示：

- 热门商品
- 低库存商品
- 低转化商品
- 分类销售排行

### 6.3 内容分析

展示：

- 热门百科
- 热门文章
- 评论/收藏/点赞趋势
- 内容知识缺口入口

### 6.4 商家分析

展示：

- 商家审核状态分布
- 商家商品数排行
- 商家订单项排行
- 发货风险列表

### 6.5 AI 咨询分析

展示：

- 咨询次数
- 高频问题
- 未解决问题数
- 工具调用次数
- 样本候选数量

### 6.6 未解决问题池

功能：

- 列表查看
- 状态筛选
- 关键词搜索
- 修改状态
- 填写处理备注

### 6.7 样本候选池

功能：

- 列表查看
- 审核通过
- 审核拒绝
- 填写审核备注
- 后续导出预留

## 7. 指标口径文档

新增：

```text
MF_DataCenter/docs/metrics-dictionary.md
```

至少写清楚：

- 指标名称
- 指标编码
- 数据来源
- 计算方式
- 统计周期
- 备注

第一批指标建议：

```text
user_total
product_total
product_on_sale_total
order_total
gmv_total
merchant_total
merchant_pending_total
ai_conversation_total
unresolved_question_total
sample_candidate_total
```

## 8. 与 MF_EP 的连接策略

V1 推荐先采用“读取型连接”：

- `datacenter-api` 只读 `MF_EP` 业务数据。
- 不通过数据中台修改订单、商品、用户、商家等业务数据。
- 业务操作仍然在 `MF_EP` 完成。

如果数据库连接信息暂时不可用：

- 先使用 mock 数据完成接口和页面。
- 把真实数据源配置留在 `application.yml`。
- 在最终报告中说明哪些接口是 mock，哪些接口已接真实数据。

## 9. 与 MF_AgentService 的连接策略

`MF_AgentService` 后续会调用数据中台接口写入：

- 咨询日志
- 工具调用日志
- 未解决问题
- 样本候选

因此写入接口必须稳定、简单、可被服务端调用。

V1 可以先不做复杂鉴权，但要预留内部服务 token 配置：

```text
datacenter.internal-token
```

## 10. 非目标

本阶段不要做：

- Kafka
- Flink
- 完整数仓分层
- 自助 BI
- 数据血缘
- 多租户数据治理
- 模型训练平台
- Agent 集群
- 复杂权限系统
- 修改 MF_EP 业务流程

这些可以在架构文档里作为后续演进，但不要在 V1 实现。

## 11. 验证要求

后端：

```bash
cd F:\20260518-xiangmu\MF_Project\MF_DataCenter\datacenter-api
mvn test
```

如果没有测试，至少执行：

```bash
mvn -DskipTests package
```

前端：

```bash
cd F:\20260518-xiangmu\MF_Project\MF_DataCenter\datacenter-web
npm run build
```

手动验证：

- 能打开数据中台前端。
- 运营总览有数据展示。
- AI 咨询日志可以写入。
- 未解决问题可以新增和修改状态。
- 样本候选可以新增和审核。
- 页面刷新后数据不丢失。
- 构建通过。

## 12. 最终报告格式

完成后请输出：

1. 改动文件列表。
2. 后端接口列表。
3. 前端页面列表。
4. 数据库表结构说明。
5. 指标口径说明。
6. 哪些数据为真实读取，哪些为 mock。
7. 验证命令和结果。
8. 已知限制和下一步建议。

