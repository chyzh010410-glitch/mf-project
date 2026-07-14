# MF_AgentService 与 MF_EP AI 客服对接接口文档

面向：`MF_EP` 前端/后端负责人  
服务提供方：`MF_AgentService`  
更新时间：2026-07-13

## 1. 对接目标与边界

`MF_EP` 的客户端、商家端或管理端只负责展示 AI 客服交互；所有意图识别、知识检索、DeepSeek 调用、商品/百科/订单工具调用及 DataCenter 数据沉淀均由 `MF_AgentService` 负责。

```text
MF_EP 前端
  -> MF_AgentService
  -> MF_EP 公开/用户业务接口
  -> MF_DataCenter
```

前端不得绕过 Agent 直接把自然语言转换为业务接口请求，也不得自行处理订单隐私判断。

## 2. 服务地址

本地开发默认地址：

```text
MF_AgentService: http://localhost:8092
```

健康检查：

```http
GET /actuator/health
```

## 3. 普通聊天接口

```http
POST /api/agent/chat
Content-Type: application/json
```

请求体：

```json
{
  "sessionId": "mf-ep-client-1001",
  "message": "推荐几款适合果树的肥料",
  "userId": "1001",
  "userType": "client",
  "authToken": "Bearer 用户登录后的Token"
}
```

字段说明：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `sessionId` | 建议 | 当前聊天会话 ID。同一低风险会话可获得有限上下文。 |
| `message` | 是 | 用户输入，不能为空。 |
| `userId` | 建议 | 当前商城用户 ID。 |
| `userType` | 建议 | `client`、`merchant` 等用户类型。 |
| `authToken` | 订单问题必填 | 商城登录 token。无 token 时 Agent 不会查询订单。 |

响应示例：

```json
{
  "answer": "我是苗丰智能客服，已为你查到 2 款匹配商品……",
  "intent": "product",
  "resolved": true,
  "usedTools": [
    { "name": "product.search", "success": true, "durationMs": 12, "failureReason": null },
    { "name": "datacenter.logConversation", "success": true, "durationMs": 8, "failureReason": null }
  ],
  "conversationId": 42,
  "fallbackReason": null,
  "sources": [],
  "confidence": 85,
  "reviewRequired": false
}
```

## 4. 流式聊天接口

```http
POST /api/agent/chat/stream
Content-Type: application/json
Accept: text/event-stream
```

请求体与普通聊天接口完全一致。服务依次返回：

```text
event: status
data: thinking

event: status
data: working

event: result
data: {完整 AgentChatResponse JSON}

event: status
data: success
```

最终状态还可能是 `doubt` 或 `error`。

注意：该接口为 **POST SSE**，浏览器原生 `EventSource` 只能发送 GET，不能直接使用。请使用 `fetch` 读取 `ReadableStream`。

示例：

```javascript
const response = await fetch('/agent-api/api/agent/chat/stream', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(request)
})

const reader = response.body.getReader()
const decoder = new TextDecoder('utf-8')
let buffer = ''

while (true) {
  const { done, value } = await reader.read()
  if (done) break
  buffer += decoder.decode(value, { stream: true })
  const events = buffer.split('\n\n')
  buffer = events.pop()
  for (const event of events) {
    const name = event.match(/^event:(.+)$/m)?.[1]?.trim()
    const data = event.match(/^data:(.+)$/m)?.[1]?.trim()
    if (name === 'status') updateChatStatus(data)
    if (name === 'result') renderAgentAnswer(JSON.parse(data))
  }
}
```

## 5. 前端展示规则

| 返回字段 | 建议展示行为 |
| --- | --- |
| `answer` | 主回答文本。 |
| `intent` | 可选显示“商品查询/树木百科/订单查询/商家入驻”。 |
| `usedTools` | 用于展示“正在查询商品/百科/订单”的过程与调试信息。 |
| `sources` | 知识类回答时展示来源标题和链接；商品、订单不需要伪造知识来源。 |
| `confidence < 70` 或 `reviewRequired=true` | 显示“建议补充信息或人工确认”。 |
| `fallbackReason` | 显示友好降级提示，不直接展示内部错误堆栈。 |

常见 `fallbackReason`：

| 值 | 用户侧提示 |
| --- | --- |
| `intent_unknown` | 请补充作物、商品、订单或商家背景。 |
| `knowledge_not_enough` | 请补充地区、树龄、症状与图片。 |
| `upstream_timeout` | 查询超时，请稍后重试。 |
| `upstream_unavailable` | 服务暂时不可用，请稍后重试。 |
| `datacenter_log_failed` | 回答可展示；无需让用户感知数据沉淀失败。 |
| `unsafe_action_blocked` | 退款、改订单、确认收货等需走平台人工流程。 |

## 6. 订单安全要求

- 订单问题必须将当前登录用户的 token 原样放入 `authToken`。
- 无 token 时，Agent 会返回登录提示，前端不得尝试用订单号绕过该限制。
- Agent 只查询当前用户自己的订单；不执行退款、改订单、取消订单、确认收货或商家审核。

## 7. 前端代理配置

建议使用开发服务器代理，避免浏览器跨域问题。以 Vite 为例：

```javascript
server: {
  proxy: {
    '/agent-api': {
      target: 'http://localhost:8092',
      changeOrigin: true,
      rewrite: path => path.replace(/^\/agent-api/, '')
    }
  }
}
```

前端统一调用：

```text
/agent-api/api/agent/chat
/agent-api/api/agent/chat/stream
```

生产环境请由网关或反向代理配置同等转发规则；不要把 DeepSeek API Key 下发到浏览器。

## 8. 联调验收清单

1. `GET http://localhost:8092/actuator/health` 返回正常。
2. 商品问题返回 `intent=product` 且出现 `product.search`。
3. 百科问题返回 `intent=encyclopedia` 且出现 `encyclopedia.search`。
4. 商家入驻问题返回 `intent=merchant` 且出现 `merchant.guide`。
5. 无 token 订单问题不返回订单详情，`order.status.success=false`。
6. 有效 token 订单问题可查询当前用户自己的订单。
7. 流式接口依次收到 `thinking`、`working`、`result`、最终状态事件。
8. DataCenter 中能查到咨询日志和对应工具调用。

## 9. 不对外开放的接口

以下接口仅供内部运维/评测使用，MF_EP 前端不得调用：

```text
POST /api/agent/evaluate
POST /api/agent/knowledge/reindex
POST /api/agent/knowledge/sync
```

它们需要 `X-MF-Evaluation-Key`，该 key 只能保存在服务端环境变量中。
