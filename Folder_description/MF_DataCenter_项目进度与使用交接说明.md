# MF_DataCenter 项目进度与使用交接说明

更新时间：2026-07-06  
项目位置：`F:\20260518-xiangmu\MF_Project\MF_DataCenter`

## 1. 项目定位

`MF_DataCenter` 是苗丰项目的企业级数据中台模块，当前定位不是替代 MF_EP 主业务系统，而是作为独立的数据治理、指标快照、质量检查和运营可信看板。

它目前连接两个核心数据来源：

- `mf_datacenter`：DataCenter 自有库，存放指标字典、指标快照、质量规则、质量问题、AI 数据沉淀、源表契约等。
- `fertilizer`：MF_EP 业务库，只读接入，用于读取用户、商品、订单、商家等业务指标。

当前后端默认端口：`8091`  
当前前端默认端口：`5176`

## 2. 当前完成进度

### 已完成

1. 基础项目结构
   - 后端：`datacenter-api`，Spring Boot + MyBatis-Plus + Flyway + MySQL。
   - 前端：`datacenter-web`，Vue + Vite + Element Plus。
   - 本地脚本：`scripts`。
   - 交付产物目录：`dist`。

2. 数据库与迁移
   - 使用系统 MySQL 服务 `MySQL80`。
   - DataCenter 数据库：`mf_datacenter`。
   - MF_EP 源库：`fertilizer`。
   - Flyway 当前已迁移到 `v8`。

3. 指标治理
   - 指标字典：支持查询、新增、编辑、启停。
   - 指标快照：支持小时快照、日快照。
   - 统一指标查询：支持多指标、粒度、维度、日期范围、limit。
   - 指标计算注册表：支持控制哪些受控指标参与快照计算。

4. 数据质量
   - 质量规则可配置。
   - 支持立即执行质量检查。
   - 自动生成质量问题。
   - 支持质量问题状态流转：`open`、`processing`、`resolved`、`ignored`。
   - 已增加问题处理历史和近 14 天趋势。

5. 数据源接入标准化
   - 已登记 MF_EP 源表契约。
   - 已检查源库连接、表是否存在、字段是否存在。
   - 当前契约覆盖：
     - `user`
     - `product`
     - `product_category`
     - `merchant`
     - `order`
     - `order_item`

6. 运营看板可信状态
   - 首页已接入治理状态。
   - 会展示：
     - 源表契约是否通过。
     - 是否存在待处理质量问题。
     - 最新快照时间和新鲜度。
     - 当前数据是否可信：`trusted` 或 `risk`。

7. 生产打包与运行
   - 已提供本地启动、停止、健康检查脚本。
   - 已提供生产打包脚本。
   - 已提供生产配置文件 `application-prod.yml`。

## 3. 当前主要页面

启动前端后访问：

- 运营总览：`http://127.0.0.1:5176/dashboard`
- 数据源接入：`http://127.0.0.1:5176/source-governance`
- 指标治理：`http://127.0.0.1:5176/metric-governance`
- 数据质量：`http://127.0.0.1:5176/data-quality`
- 商品分析：`http://127.0.0.1:5176/products`
- 内容分析：`http://127.0.0.1:5176/content`
- 商家分析：`http://127.0.0.1:5176/merchants`
- AI 咨询分析：`http://127.0.0.1:5176/ai-analysis`
- 问题池：`http://127.0.0.1:5176/unresolved-questions`
- 样本池：`http://127.0.0.1:5176/sample-candidates`

## 4. 如何启动项目

### 4.1 前置条件

需要确保：

- 系统 MySQL 服务 `MySQL80` 已启动。
- MySQL 用户名：`root`
- MySQL 密码：`123456`
- 存在数据库：
  - `mf_datacenter`
  - `fertilizer`

如果数据库不存在，可以执行：

```sql
CREATE DATABASE IF NOT EXISTS mf_datacenter DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS fertilizer DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

### 4.2 一键本地启动

在 PowerShell 进入：

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_DataCenter
```

执行：

```powershell
.\scripts\start-local.ps1
```

启动后：

- 后端：`http://127.0.0.1:8091`
- 前端：`http://127.0.0.1:5176`

### 4.3 停止项目

```powershell
.\scripts\stop-local.ps1
```

该脚本只停止 DataCenter 的 `8091` 和 `5176` 端口进程，不停止系统 MySQL。

### 4.4 健康检查

```powershell
.\scripts\check-health.ps1
```

健康检查会返回：

- API 是否正常。
- 指标字典数量。
- 数据质量检查数量。
- 源表契约异常表数量。
- 源表契约缺失字段数量。
- 看板治理状态。
- 前端页面是否可访问。

正常情况下，治理状态应为：

```text
GovernanceStatus : trusted
```

## 5. 如何打包交付

进入项目目录：

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_DataCenter
```

执行：

```powershell
.\scripts\package-prod.ps1
```

产物会生成在：

```text
F:\20260518-xiangmu\MF_Project\MF_DataCenter\dist
```

包含：

- `dist\datacenter-api.jar`
- `dist\web`

生产启动 API：

```powershell
.\scripts\start-prod-api.ps1
```

前端 `dist\web` 可以交给 Nginx、IIS 或其他静态文件服务器托管，并将 `/api` 代理到：

```text
http://127.0.0.1:8091
```

## 6. 需要负责人重点关注的连接关系

### 6.1 DataCenter 与 MF_EP

DataCenter 后端通过配置连接 MF_EP 业务库：

```yaml
datacenter:
  mf-ep:
    datasource:
      enabled: true
      url: jdbc:mysql://127.0.0.1:3306/fertilizer?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
      username: root
      password: 123456
```

该连接是只读用途，主要用于：

- 运营总览实时读取。
- 指标快照刷新。
- 源表契约检查。
- 分类销售额等业务指标计算。

### 6.2 DataCenter 自有库

DataCenter 写入自己的库：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/mf_datacenter?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 123456
```

主要写入：

- 指标字典。
- 指标快照。
- 指标计算注册表。
- 质量规则。
- 质量检查结果。
- 质量问题和处理历史。
- AI 日志、问题池、样本池。
- 源表契约。

### 6.3 前端与后端

前端默认通过 `/api` 访问后端。

本地开发时由 Vite 代理。  
生产部署时，需要 Web 服务器将：

```text
/api
```

代理到：

```text
http://127.0.0.1:8091
```

## 7. 重要接口

### 系统状态

```text
GET /api/system/status
```

### 运营总览

```text
GET /api/dashboard/overview
```

返回运营指标、趋势、分类销售额，以及治理可信状态。

### 数据源契约

```text
GET  /api/source/contracts
GET  /api/source/check
POST /api/source/check
```

### 指标治理

```text
GET   /api/metrics/dictionary
POST  /api/metrics/dictionary
PUT   /api/metrics/dictionary/{id}
PATCH /api/metrics/dictionary/{id}/enabled
```

### 指标计算注册表

```text
GET   /api/metrics/compute-registry
PATCH /api/metrics/compute-registry/{id}/enabled
```

### 指标快照

```text
POST /api/metrics/snapshots/hourly/refresh
POST /api/metrics/snapshots/daily/refresh
GET  /api/metrics/latest
GET  /api/metrics/query
```

`/api/metrics/query` 支持：

- `code=gmv_total,order_total`
- `codes=gmv_total&codes=order_total`
- `period=daily`
- `dimensionKey=global`
- `dimensionValue=all`
- `startDate=2026-07-01`
- `endDate=2026-07-06`
- `limit=100`

### 数据质量

```text
GET   /api/data-quality/summary
GET   /api/data-quality/checks
POST  /api/data-quality/run
GET   /api/data-quality/rules
POST  /api/data-quality/rules
PUT   /api/data-quality/rules/{id}
PATCH /api/data-quality/rules/{id}/enabled
GET   /api/data-quality/issues
GET   /api/data-quality/issues/trend
GET   /api/data-quality/issues/{id}/history
PATCH /api/data-quality/issues/{id}/status
```

## 8. 数据流说明

当前核心数据流：

```text
MF_EP 业务库 fertilizer
        │
        │ 只读读取
        ▼
MF_DataCenter 后端 datacenter-api
        │
        ├─ 源表契约检查
        ├─ 指标计算注册表
        ├─ 指标快照生成
        ├─ 数据质量检查
        ├─ 质量问题闭环
        └─ 运营看板可信状态
        │
        ▼
DataCenter 自有库 mf_datacenter
        │
        ▼
MF_DataCenter 前端 datacenter-web
```

## 9. 当前验证状态

最近一次完整验证包含：

- Flyway 已迁移到 `v8`。
- 源表契约：6 张表。
- 指标计算注册表：8 条。
- 后端测试：`mvn test` 通过。
- 前端构建：`npm run build` 通过。
- 生产打包：`scripts\package-prod.ps1` 通过。
- 健康检查：`scripts\check-health.ps1` 通过。
- 看板治理状态：`trusted`。

## 10. 目前还没有完成或建议后续做的事

下面这些不是当前阶段阻塞项，但负责人后续规划时应考虑：

1. 权限与登录
   - 当前 DataCenter 还没有独立登录鉴权。
   - 如果要开放给多人使用，需要接入统一登录或后台账号权限。

2. 生产 Web 托管
   - 当前已生成 `dist\web`。
   - 还需要由负责人决定使用 Nginx、IIS 或其他方式托管。

3. 生产备份策略
   - `mf_datacenter` 需要纳入备份。
   - `fertilizer` 由 MF_EP 主系统备份策略负责。

4. 更多业务域真实化
   - 商品分析、内容分析、商家分析部分仍有后续扩展空间。
   - 当前核心可信链路优先覆盖运营总览、指标治理、数据质量。

5. 告警通知
   - 质量问题目前在页面内闭环。
   - 后续可接入企业微信、邮件或站内消息。

## 11. 给负责人连接项目时的建议顺序

建议按下面顺序联调：

1. 确认系统 MySQL `MySQL80` 正常启动。
2. 确认 `mf_datacenter`、`fertilizer` 两个库存在。
3. 启动 DataCenter 后端。
4. 打开 `http://127.0.0.1:8091/api/system/status`。
5. 打开 `http://127.0.0.1:8091/api/source/check`，确认源表契约通过。
6. 打开 `http://127.0.0.1:5176/source-governance`，确认源接入页面正常。
7. 打开 `http://127.0.0.1:5176/metric-governance`，刷新小时快照或日快照。
8. 打开 `http://127.0.0.1:5176/data-quality`，执行质量检查。
9. 打开 `http://127.0.0.1:5176/dashboard`，确认治理状态为 `trusted`。

如果这 9 步都通过，说明 DataCenter 已经和 MF_EP 业务库、系统 MySQL、前端页面完整连通。
