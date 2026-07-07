# MF_DataCenter 企业级架构文档

## 1. 项目定位

`MF_DataCenter` 是苗丰体系中的轻量级数据中台项目。

它独立于主业务系统，不直接承载商城交易、内容发布、商家审核或订单履约，而是负责把苗丰各业务系统产生的数据沉淀为可分析、可复用、可服务 AI 的数据资产。

一句话定义：

> MF_DataCenter 是苗丰平台的数据资产层，统一汇总商城、内容、商家、AI 咨询和官网行为数据，为运营决策、商家分析、内容优化、客服 Agent 和后续模型微调提供数据支撑。

## 2. 在 MF 体系中的位置

```text
MF_Website
生态品牌官网、苗丰精灵入口
        ↓

MF_Pet
苗丰精灵互动角色
        ↓

MF_AgentService
通用客服 Agent + 内部 MCP 工具层
        ↓

MF_EP
商城、百科、文章、订单、用户、商家、管理后台
        ↓

MF_DataCenter
指标分析、问题池、样本池、AI 咨询分析、运营看板
```

`MF_DataCenter` 与其他项目的关系：

- 从 `MF_EP` 获取业务数据：用户、商品、订单、内容、商家、支付、退款、评论、收藏等。
- 从 `MF_AgentService` 获取 AI 咨询数据：用户问题、Agent 回答、工具调用、失败问题、样本候选。
- 后续可从 `MF_Website` 和 `MF_Pet` 获取官网行为数据：点击、唤出精灵、跳转商城、百科入口等。
- 向管理者提供数据看板。
- 向 Agent 和模型优化链路提供问题池、样本池和评估数据。

## 3. 总体架构

V1 推荐采用“独立项目、轻量架构、可演进”的方式。

```text
                ┌────────────────────────┐
                │      datacenter-web     │
                │ Vue3 + Element Plus     │
                │ ECharts 数据看板         │
                └───────────▲────────────┘
                            │ HTTP API
                ┌───────────┴────────────┐
                │      datacenter-api     │
                │ Spring Boot             │
                │ 指标聚合 / 样本池 / 问题池 │
                └───────▲────────▲───────┘
                        │        │
        ┌───────────────┘        └────────────────┐
        │                                         │
┌───────┴────────┐                       ┌────────┴─────────┐
│ MF_EP MySQL     │                       │ MF_AgentService   │
│ 业务数据源       │                       │ 咨询日志 / 工具调用 │
└────────────────┘                       └──────────────────┘
```

V1 可以先直接读取 `MF_EP` 的 MySQL 业务库，同时提供接口接收 `MF_AgentService` 写入的 AI 咨询日志。后续再逐步演进为独立指标库、定时同步、宽表和数据服务层。

## 4. 项目模块

推荐目录：

```text
MF_DataCenter
├── datacenter-api
├── datacenter-web
├── docs
└── README.md
```

### 4.1 datacenter-api

数据中台后端。

职责：

- 连接或读取 `MF_EP` 业务数据。
- 提供运营总览、商品分析、内容分析、商家分析、AI 咨询分析接口。
- 接收 `MF_AgentService` 写入的咨询日志。
- 管理未解决问题池。
- 管理高质量样本候选池。
- 统一指标口径。

建议技术：

- Java 17
- Spring Boot
- MyBatis-Plus
- MySQL
- Knife4j/OpenAPI
- Lombok

### 4.2 datacenter-web

数据中台前端。

职责：

- 展示运营看板。
- 展示商品、内容、商家、AI 咨询分析页面。
- 管理未解决问题池。
- 管理样本候选池。
- 支持基础筛选：时间范围、分类、状态、关键词。

建议技术：

- Vue 3
- Vite
- Element Plus
- ECharts
- Axios
- Vue Router

### 4.3 docs

文档目录。

职责：

- 指标口径说明。
- 数据来源说明。
- API 说明。
- 样本池规则。
- 后续演进记录。

## 5. 数据域划分

V1 按业务域组织数据，不做复杂数仓分层。

### 5.1 运营总览域

目标：回答平台整体表现。

核心指标：

- 用户总数
- 新增用户数
- 商品总数
- 上架商品数
- 商家总数
- 待审核商家数
- 订单数
- GMV
- 支付订单数
- 退款申请数

### 5.2 商品分析域

目标：回答商品表现。

核心指标：

- 商品浏览数
- 商品收藏数
- 加购数
- 下单数
- 销售额
- 转化率
- 库存预警
- 热门商品
- 低转化商品

### 5.3 内容分析域

目标：回答百科、文章是否产生价值。

核心指标：

- 百科浏览数
- 文章浏览数
- 点赞数
- 收藏数
- 评论数
- 热门百科
- 热门文章
- 内容带来的商品点击或咨询入口点击

### 5.4 商家分析域

目标：回答商家供给和履约表现。

核心指标：

- 商家总数
- 待审核商家数
- 已通过商家数
- 被拒绝商家数
- 禁用商家数
- 商家商品数
- 商家订单项数量
- 商家发货数量
- 发货超时风险

### 5.5 AI 咨询分析域

目标：回答 AI 服务是否有效，以及后续如何优化。

核心指标：

- 咨询次数
- 独立咨询用户数
- 高频问题
- 未解决问题数
- 工具调用次数
- 知识命中来源
- 低满意度问题
- 样本候选数量

### 5.6 问题池与样本池

问题池：

- 存放 Agent 无法解决、用户不满意、需要人工补充知识的问题。
- 用于 FAQ、百科和知识库补全。

样本池：

- 存放可用于后续模型微调或评测的数据候选。
- V1 只存候选，不直接进入训练。
- 样本必须人工审核后才能进入训练集或评测集。

## 6. 数据流设计

### 6.1 业务数据流

```text
MF_EP 业务库
      ↓
datacenter-api 查询/聚合
      ↓
datacenter-web 看板展示
```

V1 允许直接读取业务库，优先快速形成看板能力。

后续演进：

```text
MF_EP 业务库
      ↓ 定时同步 / ETL
MF_DataCenter 指标库
      ↓
datacenter-api
      ↓
datacenter-web
```

### 6.2 AI 咨询数据流

```text
用户 / 苗丰精灵
      ↓
MF_AgentService
      ↓
datacenter-api 写入咨询日志
      ↓
AI 咨询分析 / 未解决问题池 / 样本候选池
```

### 6.3 后续模型优化数据流

```text
AI 咨询日志
      ↓
问题池 / 样本候选池
      ↓ 人工审核
高质量样本集 / 评测集
      ↓
RAG 优化 / 模型微调
```

## 7. V1 数据模型建议

V1 可新增数据中台自身表，优先服务 AI 咨询和样本沉淀。

建议表：

```text
dc_ai_conversation_log
dc_ai_tool_call_log
dc_unresolved_question
dc_sample_candidate
dc_metric_snapshot
```

### dc_ai_conversation_log

用于记录客服 Agent 咨询日志。

建议字段：

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

### dc_ai_tool_call_log

用于记录 Agent 工具调用。

建议字段：

- id
- conversation_id
- tool_name
- request_summary
- response_summary
- success
- error_message
- duration_ms
- create_time

### dc_unresolved_question

用于沉淀未解决问题。

建议字段：

- id
- conversation_id
- question
- reason
- status
- owner
- remark
- create_time
- update_time

### dc_sample_candidate

用于沉淀样本候选。

建议字段：

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

### dc_metric_snapshot

用于后续存储指标快照。

建议字段：

- id
- metric_code
- metric_name
- metric_value
- dimension_key
- dimension_value
- snapshot_date
- create_time

V1 可以先不实现所有表，但开发时应避免把 AI 咨询数据散落到无结构日志中。

## 8. API 分层建议

datacenter-api 推荐按业务域分 Controller：

```text
/api/dashboard/overview
/api/analysis/products
/api/analysis/content
/api/analysis/merchants
/api/analysis/ai
/api/ai/conversations
/api/ai/unresolved-questions
/api/ai/sample-candidates
/api/metrics/snapshots
```

接口返回统一结构，避免前端直接适配多种格式。

## 9. 权限与安全

V1 权限可以保持简单，但边界必须明确：

- 数据中台面向平台内部人员，不面向普通消费者。
- 后续应接入管理员登录或独立数据中台账号。
- 订单、用户、咨询日志等敏感信息展示时应做脱敏。
- Agent 写入数据中台时应使用服务端 token 或内部签名。
- 样本池不得直接暴露用户隐私信息。
- 模型训练前必须进行隐私清洗和人工审核。

## 10. 企业级演进路线

### 阶段 1：轻量数据中台 V1

- 独立 datacenter-api。
- 独立 datacenter-web。
- 直接读取 MF_EP 业务库。
- 接收 Agent 咨询日志。
- 形成基础看板、问题池、样本池。

### 阶段 2：指标库与定时同步

- 增加数据中台独立指标库。
- 增加每日/每小时指标快照。
- 统一指标口径。
- 降低对业务库的直接查询压力。

### 阶段 3：行为埋点与漏斗分析

- 官网行为埋点。
- 商城浏览、收藏、加购、下单漏斗。
- 苗丰精灵唤出到咨询/下单转化分析。

### 阶段 4：AI 样本治理

- 样本审核流。
- 训练集、验证集、评测集管理。
- RAG 知识缺口分析。
- 模型版本效果对比。

### 阶段 5：高级数据平台

- 数据同步任务。
- 宽表。
- 数据质量检查。
- 简单数据血缘。
- 多 Agent 指标服务。

Kafka、Flink、完整数仓分层、自助 BI、复杂数据治理不属于 V1。

## 11. 成功标准

V1 成功标准：

- 数据中台能独立启动。
- 前端能展示运营总览、商品分析、内容分析、商家分析、AI 咨询分析中的至少一批核心指标。
- Agent 能写入咨询日志。
- 未解决问题能进入问题池。
- 可沉淀的问答能进入样本候选池。
- 指标口径有文档说明。
- 不破坏 MF_EP 现有业务系统。

