# MF_DataCenter API

统一响应结构：

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

## 看板与分析

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/dashboard/overview` | 运营总览、趋势、分类占比、治理可信状态 |
| GET | `/api/analysis/products` | 商品分析 |
| GET | `/api/analysis/content` | 内容分析 |
| GET | `/api/analysis/merchants` | 商家分析 |
| GET | `/api/analysis/ai` | AI 咨询分析 |
| GET | `/api/system/status` | 服务和数据源状态 |

## AI 数据沉淀

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/ai/conversations` | 写入 AI 咨询日志 |
| GET | `/api/ai/conversations` | 查询 AI 咨询日志 |
| POST | `/api/ai/tool-calls` | 写入 Agent 工具调用日志 |
| GET | `/api/ai/tool-calls` | 查询 Agent 工具调用日志 |
| POST | `/api/ai/unresolved-questions` | 新增未解决问题 |
| GET | `/api/ai/unresolved-questions` | 查询未解决问题，支持 `status`、`keyword` |
| PATCH | `/api/ai/unresolved-questions/{id}/status` | 修改问题状态和备注 |
| POST | `/api/ai/sample-candidates` | 新增样本候选 |
| GET | `/api/ai/sample-candidates` | 查询样本候选，支持 `reviewStatus`、`keyword` |
| PATCH | `/api/ai/sample-candidates/{id}/review` | 审核样本候选 |

## 指标治理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/metrics/dictionary` | 查询指标字典 |
| GET | `/api/metrics/compute-registry` | 查询指标计算注册表 |
| PATCH | `/api/metrics/compute-registry/{id}/enabled` | 启用或停用受控指标计算 |
| POST | `/api/metrics/dictionary` | 新增指标定义 |
| PUT | `/api/metrics/dictionary/{id}` | 更新指标定义 |
| PATCH | `/api/metrics/dictionary/{id}/enabled` | 启用或停用指标定义 |
| GET | `/api/metrics/latest` | 查询最新全局指标快照 |
| GET | `/api/metrics/query` | 按指标、粒度、维度、日期范围查询指标快照 |
| POST | `/api/metrics/snapshots/hourly/refresh` | 手动刷新小时快照 |
| POST | `/api/metrics/snapshots/daily/refresh` | 手动刷新日快照 |

## 数据质量

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/data-quality/summary` | 查询最新质量汇总 |
| GET | `/api/data-quality/checks` | 查询最新质量检查结果 |
| POST | `/api/data-quality/run` | 立即运行质量检查 |
| GET | `/api/data-quality/rules` | 查询质量规则 |
| POST | `/api/data-quality/rules` | 新增质量规则 |
| PUT | `/api/data-quality/rules/{id}` | 更新质量规则 |
| PATCH | `/api/data-quality/rules/{id}/enabled` | 启用或停用质量规则 |
| GET | `/api/data-quality/issues` | 查询质量问题，支持 `status` |
| GET | `/api/data-quality/issues/trend` | 查询近 14 天质量问题趋势 |
| GET | `/api/data-quality/issues/{id}/history` | 查询质量问题处理历史 |
| PATCH | `/api/data-quality/issues/{id}/status` | 流转质量问题状态 |

## 数据源接入

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/source/contracts` | 查询 MF_EP 源表契约 |
| GET | `/api/source/check` | 检查源库连接、表、字段是否满足契约 |
| POST | `/api/source/check` | 立即执行源库契约检查 |

质量问题状态：

- `open`
- `processing`
- `resolved`
- `ignored`

## 指标查询参数

`GET /api/metrics/query` 支持：

- `code`: 单个或逗号分隔的指标编码，例如 `gmv_total,order_total`。
- `codes`: 可重复传入的指标编码，例如 `codes=gmv_total&codes=order_total`。
- `period`: `daily` 或 `hourly`。
- `dimensionKey`: 维度 key，例如 `global`、`category`。
- `dimensionValue`: 维度值，例如 `all`、具体分类名。
- `startDate`: 开始日期，格式 `yyyy-MM-dd`。
- `endDate`: 结束日期，格式 `yyyy-MM-dd`。
- `limit`: 返回条数，范围 `1` 到 `1000`。
