# MF_DataCenter 手工回归清单

用于每次修改后快速确认 V1 仍然可用。先启动后端和前端，再按顺序检查。

## 1. 启动检查

- 后端启动成功：`http://localhost:8091/api/dashboard/overview` 返回 `code = 0`。
- Swagger 可打开：`http://localhost:8091/swagger-ui.html`。
- 前端可打开：`http://localhost:5176/dashboard`。
- 前端请求接口不出现浏览器控制台红色错误。

## 2. 视觉检查

- 页面整体使用 MF_Website 浅绿色渐变背景。
- 侧边栏、顶部栏、指标卡、表格不出现文字重叠。
- 桌面宽度下，运营总览指标卡和图表布局清晰。
- 移动窄屏下，侧边导航和指标卡能换行，不横向溢出。

## 3. 页面检查

- `/dashboard` 展示运营总览、订单趋势、GMV 趋势、分类销售占比。
- `/products` 展示热门商品、风险商品、分类销售排行。
- `/content` 展示热门百科、热门文章、互动趋势、知识缺口标签。
- `/merchants` 展示商家状态分布、排行和发货风险。
- `/ai-analysis` 展示 AI 指标、高频问题、咨询日志和工具调用日志。
- `/unresolved-questions` 能筛选、查询、新增、修改状态。
- `/sample-candidates` 能筛选、查询、新增、审核、导出 CSV。

## 4. AI 数据链路检查

- 在 `/ai-analysis` 新增一条咨询日志后，咨询次数增加。
- 在 `/ai-analysis` 新增一条工具调用日志后，工具调用次数增加。
- 在 `/unresolved-questions` 新增一个问题后，列表可见。
- 修改未解决问题状态为 `resolved` 后，状态刷新正确。
- 在 `/sample-candidates` 新增一个样本后，列表可见。
- 审核样本为 `approved` 或 `rejected` 后，审核状态刷新正确。
- 导出样本 CSV 后，文件包含当前列表中的问题和回答。

## 5. 持久化检查

- 新增一条咨询日志、一个未解决问题、一个样本候选。
- 停止后端。
- 重新启动后端。
- 刷新前端页面。
- 刚才新增的数据仍然存在。

默认 profile 持久化文件：

```text
datacenter-api/data/ai-store.json
```

`local` profile 持久化库：

```text
mf_datacenter
```

## 6. 构建检查

后端：

```bash
cd F:\20260518-xiangmu\MF_Project\MF_DataCenter\datacenter-api
mvn test
mvn -DskipTests package
```

前端：

```bash
cd F:\20260518-xiangmu\MF_Project\MF_DataCenter\datacenter-web
npm run build
```

允许的非阻断项：

- Vite 输出 VueUse PURE 注释警告。
- 前端依赖审计提示需要单独评估，不在普通回归中自动强制升级。

## 7. 当前已知限制

- 业务侧运营、商品、内容、商家指标仍是 mock 聚合数据。
- 默认 profile 使用本地 JSON 文件；`local` profile 已使用 MySQL。生产并发写入仍建议补充连接池和迁移工具。
- `datacenter.internal-token` 已预留，暂未启用强鉴权。
