# MF_EP：AI 客服会话历史接入任务书

面向：MF_EP 前端、后端负责人  
协作方：MF_DataCenter、MF_AgentService  
更新时间：2026-07-13

## 1. 目标

用户从订单页、商品页等位置返回 `/ai` 后，能继续看到自己的 AI 客服聊天记录；刷新页面后也不丢失。历史记录以当前登录用户为边界，不能串号，也不能暴露订单 token、地址、手机号等敏感信息。

本期采用“双层恢复”：

```text
浏览器 localStorage：快速恢复最近消息、支持短暂离线
MF_DataCenter：长期持久化、支持换浏览器/设备恢复
```

`MF_AgentService` 仍负责回答、工具调用和写入咨询日志；MF_EP 不复制意图识别或业务安全规则。

## 2. 本负责人交付内容

### 2.1 稳定的会话标识

AI 页面不得在每次进入时随机生成 `sessionId`。登录用户默认使用：

```text
mf-ep-client-{userId}-default
```

后续支持“新建会话”时再生成：

```text
mf-ep-client-{userId}-{uuid}
```

向 AgentService 调用 `/api/agent/chat` 和 `/api/agent/chat/stream` 时持续传入该 `sessionId`、当前用户 ID、用户类型和登录 token。

### 2.2 页面恢复逻辑

在 `mf-frontend-client/src/views/ai/index.vue` 实现：

1. 页面进入后，先从本地缓存恢复消息，避免白屏。
2. 随后通过 MF_EP 后端获取 DataCenter 中当前用户的历史消息，以服务端结果为准合并、去重并刷新页面。
3. 用户发送消息、收到流式 token、收到最终 `result` 后，均更新本地缓存。
4. 最多保留当前会话最近 50 条消息；超出部分从最早消息开始裁剪。
5. 请求失败时保留本地消息，并显示“历史记录暂未同步”，不能清空现有对话。

建议本地缓存键：

```text
mf-ai-chat:{userId}:{sessionId}
```

本地消息仅保存展示所需字段：

```json
{
  "id": "conversationId 或前端临时 ID",
  "role": "user | ai",
  "content": "展示文本",
  "createdAt": "2026-07-13T03:30:00+08:00",
  "sources": []
}
```

严禁写入 `authToken`、订单 token、手机号、收货地址、身份证件或完整订单详情。

### 2.3 MF_EP 后端转发接口

浏览器不应直接调用 MF_DataCenter 的通用运营接口。请由 MF_EP 后端提供仅供当前登录用户调用的 BFF 接口：

```http
GET /api/client/ai/conversations?sessionId={sessionId}&page=1&pageSize=50
DELETE /api/client/ai/conversations/{sessionId}
```

要求：

- 从 MF_EP 当前登录态取得用户 ID，不采信前端传来的 `userId`；
- 将已认证用户身份或可信服务凭据转发给 DataCenter；
- `sessionId` 必须符合当前用户的命名前缀，或由 DataCenter 再次校验归属；
- `DELETE` 仅删除当前用户自己的展示历史；不允许调用 DataCenter 的运营查询接口；
- 返回统一的 MF_EP 响应结构。

建议响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "sessionId": "mf-ep-client-1001-default",
    "items": [
      {
        "id": 42,
        "question": "我的订单有哪些",
        "answer": "请先登录后再查询订单。",
        "intent": "order",
        "createdAt": "2026-07-13T03:30:00+08:00"
      }
    ],
    "page": 1,
    "pageSize": 50,
    "total": 1
  }
}
```

前端将每条服务端记录渲染为一条 `user` 消息和一条 `ai` 消息，按时间排序。

### 2.4 页面操作

本期至少增加：

- “新建会话”：切换到新的 sessionId，并显示初始欢迎语；
- “清空当前会话”：先调用删除接口，成功后删除对应 localStorage；
- 加载失败提示：不影响继续提问；
- 退出登录：清理当前用户的本地会话缓存。

## 3. 与其他服务的边界

```text
MF_EP 前端
  -> MF_EP 后端 /api/client/ai/conversations
  -> MF_DataCenter 用户会话历史接口

MF_EP 前端
  -> MF_AgentService /api/agent/chat/stream
  -> MF_AgentService 写入 MF_DataCenter 咨询日志
```

- 不在前端保存或下发 DeepSeek API Key；
- 不在 MF_EP 前端实现订单权限判断；
- 不直接读取 DataCenter 的工具调用、未解决问题、样本候选等运营数据；
- 历史回放只展示问答正文与允许公开的来源标题。

## 4. 验收清单

1. 登录用户连续聊天后切到订单页，再回 `/ai`，聊天记录仍显示。
2. 刷新 `/ai`，先显示本地记录，再被服务端历史正确补齐。
3. 更换浏览器或清空 localStorage 后，登录同一账号仍能从服务端恢复历史。
4. A 用户不能传入 B 用户 ID 或 B 用户 sessionId 读取 B 的记录。
5. 本地缓存和接口响应中均不包含 token、地址、手机号或完整订单明细。
6. DataCenter 不可用时，当前聊天仍可继续，页面仅提示同步失败。

