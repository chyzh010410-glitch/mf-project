# MF_DataCenter 指标口径

V1 指标优先服务运营总览、AI 咨询沉淀和基础分析页面。当前业务分析指标先使用 mock 聚合数据，AI 咨询、问题池、样本池通过 `datacenter-api` 写入接口沉淀。

| 指标名称 | 指标编码 | 数据来源 | 计算方式 | 统计周期 | 备注 |
| --- | --- | --- | --- | --- | --- |
| 用户总数 | user_total | MF_EP 用户表 | count(*) | 实时/每日 | V1 mock，后续接只读业务库 |
| 商品总数 | product_total | MF_EP 商品表 | count(*) | 实时/每日 | 包含全部商品 |
| 上架商品数 | product_on_sale_total | MF_EP 商品表 | count(status = on_sale) | 实时/每日 | 状态值以后以 MF_EP 为准 |
| 订单数 | order_total | MF_EP 订单表 | count(*) | 实时/每日 | V1 mock |
| GMV | gmv_total | MF_EP 订单/支付表 | sum(pay_amount) | 实时/每日 | 仅统计已支付订单 |
| 商家总数 | merchant_total | MF_EP 商家表 | count(*) | 实时/每日 | V1 mock |
| 待审核商家数 | merchant_pending_total | MF_EP 商家表 | count(status = pending) | 实时/每日 | V1 mock |
| AI 咨询次数 | ai_conversation_total | dc_ai_conversation_log | count(*) | 实时 | V1 由写入接口产生 |
| 工具调用次数 | ai_tool_call_total | dc_ai_tool_call_log | count(*) | 实时 | 统计 Agent 工具链路调用次数 |
| 未解决问题数 | unresolved_question_total | dc_unresolved_question | count(status in pending, processing) | 实时 | 用于知识补全 |
| 样本候选数 | sample_candidate_total | dc_sample_candidate | count(*) | 实时 | 审核后可进入训练/评测链路 |

## V1 数据状态

- 真实写入：AI 咨询日志、工具调用日志、未解决问题、样本候选。当前实现为本地 JSON 文件持久化，默认路径为 `datacenter-api/data/ai-store.json`，接口和表结构已固定。
- Mock 聚合：运营总览、商品分析、内容分析、商家分析、AI 分析统计卡片。
- 预留接入：`datacenter-api/src/main/resources/application.yml` 中保留 `datacenter.mf-ep.datasource`。
