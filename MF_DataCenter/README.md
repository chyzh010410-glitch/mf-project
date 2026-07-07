# MF_DataCenter

苗丰轻量级数据中台，用于把商城、内容、商家、AI 咨询和官网行为数据沉淀为可分析、可复用的数据资产。

当前 V1 目标是先形成一个能独立启动、能展示看板、能沉淀 AI 数据的版本。它不替代 `MF_EP` 管理后台，也不直接修改订单、商品、用户、商家等业务数据。

## 当前能力

### datacenter-api

- Spring Boot 后端骨架。
- 统一响应结构：`{ code, message, data }`。
- 运营总览接口：`GET /api/dashboard/overview`。
- 商品、内容、商家、AI 分析接口。
- AI 咨询日志写入和查询。
- Agent 工具调用日志写入和查询。
- 未解决问题池新增、查询、状态更新。
- 样本候选池新增、查询、审核。
- 指标字典接口。
- AI 数据支持 MySQL 持久化；默认配置保留 JSON fallback，`local` profile 使用 `mf_datacenter`。
- OpenAPI/Swagger UI：`/swagger-ui.html`。

### datacenter-web

- Vue 3 + Vite + Element Plus + ECharts。
- 采用 MF_Website 浅绿色主视觉：

```css
background: linear-gradient(135deg, #a8ddba, #c8ebcb 56%, #e1f3d8);
```

- 页面：
  - `/dashboard` 运营总览
  - `/products` 商品分析
  - `/content` 内容分析
  - `/merchants` 商家分析
  - `/ai-analysis` AI 咨询分析
  - `/unresolved-questions` 未解决问题池
  - `/sample-candidates` 样本候选池

## 启动方式

### 后端

```bash
cd F:\20260518-xiangmu\MF_Project\MF_DataCenter\datacenter-api
mvn -DskipTests package
java -jar target\datacenter-api-0.0.1-SNAPSHOT.jar
```

默认地址：

```text
http://localhost:8091
```

### 前端

```bash
cd F:\20260518-xiangmu\MF_Project\MF_DataCenter\datacenter-web
npm install
npm run dev
```

默认地址：

```text
http://localhost:5176/dashboard
```

Vite 已配置 `/api` 代理到 `http://localhost:8091`。

## 验证命令

后端测试：

```bash
cd F:\20260518-xiangmu\MF_Project\MF_DataCenter\datacenter-api
mvn test
```

后端打包：

```bash
mvn -DskipTests package
```

前端构建：

```bash
cd F:\20260518-xiangmu\MF_Project\MF_DataCenter\datacenter-web
npm run build
```

手工回归清单见：

```text
docs/manual-regression-checklist.md
```

## 数据状态

### 当前真实写入

以下数据通过接口写入。默认配置使用本地 JSON fallback；本机 `local` profile 已切换到 MySQL 库 `mf_datacenter`：

- AI 咨询日志
- Agent 工具调用日志
- 未解决问题池
- 样本候选池

默认 JSON 配置位置：

```yaml
datacenter:
  storage:
    ai-data-file: data/ai-store.json
```

本地 MySQL 配置位置：

```text
datacenter-api/src/main/resources/application-local.yml
```

### 当前 mock 聚合

以下业务分析数据仍为 V1 mock 聚合数据：

- 用户、商品、订单、GMV、商家等运营指标。
- 商品分析、内容分析、商家分析中的业务侧指标。
- 趋势图和分类销售占比。

后续接入 `MF_EP` 时，应保持只读连接，不通过数据中台修改业务数据。

## 文档索引

- 产品范围：`docs/product-scope.md`
- API 文档：`docs/api.md`
- 数据来源说明：`docs/data-source.md`
- 本地 MySQL 配置：`docs/local-mysql.md`
- 指标口径：`docs/metrics-dictionary.md`
- 手工回归：`docs/manual-regression-checklist.md`
- 数据库建表 SQL：`datacenter-api/src/main/resources/db/schema.sql`

## 非目标

V1 不做：

- Kafka / Flink。
- 完整数仓分层。
- 自助 BI。
- 数据血缘。
- 多租户数据治理。
- 模型训练平台。
- Agent 集群。
- 修改 `MF_EP` 业务流程。

这些能力可以作为后续企业级演进方向，但不进入当前 V1。
