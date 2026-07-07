# MF_AgentService 架构说明

## 核心链路

```text
MF_Website / MF_Pet
苗丰精灵入口
        ↓

MF_AgentService
通用客服 Agent
        ↓

MCP 工具层
商品工具 / 百科工具 / 订单工具 / 商家工具 / 数据中台工具
        ↓

MF_EP + MF_DataCenter
业务能力与数据沉淀
```

## 通用客服 Agent 职责

- 理解用户问题。
- 判断问题类型。
- 决定是否需要调用工具。
- 组织回答。
- 对无法解决的问题做兜底。
- 写入咨询日志。
- 将可沉淀问题交给数据中台。

## MCP 工具层职责

MCP 工具层负责把业务能力标准化暴露给 Agent，避免 Agent 直接访问数据库或散乱调用接口。

V1 工具建议：

- `product.search`：查询商品。
- `product.detail`：查询商品详情。
- `encyclopedia.search`：查询百科。
- `order.status`：查询订单状态。
- `merchant.guide`：查询商家入驻说明。
- `datacenter.logConversation`：写入咨询日志。
- `datacenter.reportUnresolved`：写入未解决问题。
- `datacenter.saveSampleCandidate`：写入样本候选。

## 安全边界

- Agent 不直接操作数据库。
- Agent 不直接执行高风险业务动作。
- 查询订单等敏感信息必须验证用户身份。
- V1 不做自动退款、自动改订单、自动审核商家。
- 所有工具调用需要记录日志。

