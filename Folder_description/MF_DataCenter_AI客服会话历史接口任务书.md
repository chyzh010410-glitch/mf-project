# MF_DataCenter：AI 客服会话历史接口任务书

面向：MF_DataCenter 后端负责人  
调用方：MF_EP 后端（用户侧 BFF）  
协作方：MF_AgentService  
更新时间：2026-07-13

## 1. 目标

当前 `POST /api/ai/conversations` 已由 MF_AgentService 写入 AI 咨询日志，主要服务运营分析。现需补充“当前用户读取自己的聊天历史”的受控能力，用于 MF_EP AI 客服页面在切页、刷新、换设备后恢复会话。

运营查询接口与用户历史接口必须分离：用户侧绝不能使用或暴露 `GET /api/ai/conversations` 这类通用运营查询能力。

## 2. 数据前提与最小字段

请确认 AI 咨询日志至少可保存并查询以下字段：

```text
id / conversationId
sessionId
userId
userType
question
answer
intent
createdAt
```

若当前表没有 `session_id`、`user_id` 或 `user_type`，请做最小增量迁移并为以下组合建立索引：

```sql
(user_id, session_id, created_at)
```

不得把 `authToken`、订单 token、完整收货地址、手机号或证件信息写入该表。订单类问答仅保存客服最终展示文案；如文案含敏感信息，应在写入前脱敏。

## 3. 新增用户侧受控接口

推荐由 MF_EP 后端以服务调用方式访问，DataCenter 不直接信任浏览器传入的用户 ID。

### 3.1 查询当前用户会话

```http
GET /api/ai/my-conversations?sessionId={sessionId}&page=1&pageSize=50
Authorization: Bearer {MF_EP 已认证用户 token 或内部可信身份}
```

行为要求：

- 根据认证上下文获得当前用户 ID；禁止通过查询参数指定任意 `userId`；
- 仅查询该用户、该 `sessionId` 的记录；
- 按 `createdAt ASC` 返回，便于直接回放聊天；
- 默认 `page=1`、`pageSize=50`，最大 `pageSize=100`；
- 返回总数、分页信息和允许展示的字段；
- 未登录返回 `401`，身份不匹配返回 `403`，不存在返回空列表而非报错。

示例：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "sessionId": "mf-ep-client-1001-default",
    "items": [
      {
        "id": 42,
        "question": "苹果树黄叶怎么办？",
        "answer": "可以先检查浇水、根系和叶片情况……",
        "intent": "encyclopedia",
        "createdAt": "2026-07-13T03:30:00+08:00"
      }
    ],
    "page": 1,
    "pageSize": 50,
    "total": 1
  }
}
```

### 3.2 删除当前用户一个会话的展示历史

```http
DELETE /api/ai/my-conversations/{sessionId}
Authorization: Bearer {MF_EP 已认证用户 token 或内部可信身份}
```

删除语义：

- 只允许删除认证用户自己的该会话记录；
- 返回 `deletedCount`；
- 不允许按前端提供的 `userId` 删除；
- 建议采用软删除，例如 `user_deleted_at`，保留最短必要的运营审计能力；
- 软删除后的记录不能再出现在用户侧历史和默认运营列表中。若运营确有合规保留需求，应另设具备权限的审计查询。

## 4. 鉴权要求

二选一实现，并在联调前明确方案：

1. **推荐：MF_EP 后端服务转发。** DataCenter 校验 MF_EP 的内部服务凭据；MF_EP 后端已从用户 JWT 得到用户 ID，并把可信用户 ID 放入受签名的内部身份头。
2. **备选：DataCenter 直接校验 MF_EP 用户 JWT。** 必须复用同一签名密钥或公钥，并从 JWT claims 中读取用户 ID。

无论使用哪种方案：

- 前端提供的 `userId` 都不是授权依据；
- `sessionId` 是筛选条件，不是权限凭证；
- 所有未授权读取、跨用户读取、删除尝试需要记录安全审计日志；
- 通用运营接口继续仅限后台运营人员。

## 5. 与 AgentService 的数据契约

MF_AgentService 写入咨询时会携带：

```json
{
  "sessionId": "mf-ep-client-1001-default",
  "userId": "1001",
  "userType": "client",
  "question": "用户问题",
  "answer": "Agent 最终回答",
  "intent": "product | encyclopedia | order | merchant | ...",
  "resolved": true
}
```

请确保写入接口能够稳定保存前三个归属字段。若现有字段名不同，请向 MF_AgentService 负责人给出映射，禁止悄悄丢弃 `sessionId` 或用户归属字段。

## 6. 不属于本期范围

- 不向用户侧返回工具调用轨迹、未解决问题、样本候选、模型提示词或内部错误；
- 不存储或回放订单 token；
- 不改写 MF_EP 商品、订单、百科等业务数据；
- 不让用户侧接口承担运营分析或全量数据导出。

## 7. 验收清单

1. MF_AgentService 写入一轮咨询后，使用同一用户与 sessionId 能查到该记录。
2. A 用户请求 B 用户的 sessionId，返回空结果或 `403`，绝不返回 B 的内容。
3. 无认证请求返回 `401`。
4. `pageSize` 越界被限制，查询不会全表扫描；确认索引命中。
5. 删除会话后，该用户再次查询为空；其他用户数据不受影响。
6. 返回体、日志和数据库不出现 token、地址、手机号等敏感值。
7. 现有运营端 AI 分析与 Agent 写入接口回归通过。

