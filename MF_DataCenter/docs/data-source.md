# MF_DataCenter 数据来源说明

## V1 接入策略

V1 采用轻量方式先完成可运行闭环：

- `datacenter-web` 通过 HTTP API 调用 `datacenter-api`。
- `datacenter-api` 的运营、商品、内容、商家分析接口暂时返回 mock 聚合数据。
- AI 咨询日志、工具调用日志、未解决问题池、样本候选池通过接口写入。默认配置持久化到 `datacenter.storage.ai-data-file` 指定的本地 JSON 文件；启用 `datacenter.mysql.enabled=true` 时持久化到 MySQL。
- 不修改 `MF_EP` 的业务库结构和业务流程。

## 后续真实数据接入

`datacenter-api/src/main/resources/application.yml` 已预留：

```yaml
datacenter:
  storage:
    ai-data-file: data/ai-store.json
  mysql:
    enabled: false
  mf-ep:
    datasource:
      enabled: false
      url:
      username:
      password:
```

后续接入真实业务库时，建议保持只读连接：

- 用户、商品、订单、GMV、商家等指标从 `MF_EP` MySQL 读取。
- AI 咨询日志、问题池、样本池保存在数据中台自身表。
- 数据中台不直接修改订单、商品、用户、商家等业务数据。

## 当前限制

- 默认配置使用本地 JSON 文件保存 AI 咨询、工具调用、问题池和样本池数据；本机 `local` profile 已切换为 MySQL 库 `mf_datacenter`。
- 分析接口的业务指标目前为 mock，不能作为真实经营报表。
- `datacenter.internal-token` 已预留，但 V1 尚未启用复杂鉴权。
