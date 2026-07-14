# MF_AgentService 负责人交接记忆文档

项目位置：`F:\20260518-xiangmu\MF_Project\MF_AgentService`  
服务端口：`8092`  
更新时间：2026-07-13  
当前负责人范围：AI 客服编排、DeepSeek 受控调用、知识检索、MF_EP 只读业务工具、DataCenter 运营沉淀。

## 1. 当前结论

MF_AgentService 已是 MF_EP AI 客服的唯一 Agent 入口，不再使用 MF_EP 旧 `mf-ai` 模块。

```text
MF_EP 客户端（5174）
  -> /agent-api 代理
  -> MF_AgentService（8092）
  -> MF_EP 后端（8080）：商品、百科、FAQ、文章、订单等只读查询
  -> MF_DataCenter（8091）：咨询、工具调用、问题池、样本候选等运营沉淀
```

服务不直接连业务数据库；商品、订单、百科等事实必须来自 MF_EP 工具接口。DataCenter 是运营记录与质量闭环，不是回答事实源。

## 2. 启动与运行状态

### 2.1 默认规则模式

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_AgentService
mvn spring-boot:run
```

默认 `MF_AGENT_LLM_ENABLED=false`。规则、工具和本地知识检索仍可运行；开放式聊天质量会明显弱于启用 DeepSeek 的模式。

### 2.2 启用 DeepSeek

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_AgentService
.\scripts\start-deepseek.ps1
```

脚本使用隐藏输入读取 API Key，并只写入当前进程环境变量。**任何 API Key 都不得写入源码、YAML、Markdown、前端包、日志或截图。**

若端口被旧进程占用：

```powershell
$conn = Get-NetTCPConnection -LocalPort 8092 -State Listen -ErrorAction SilentlyContinue
if ($conn) { Stop-Process -Id $conn.OwningProcess -Force }
```

启动后检查：

```powershell
Invoke-RestMethod http://localhost:8092/api/agent/runtime/status
```

启用模型时应确认：

```text
llmEnabled            True
apiKeyConfigured      True
chatGatewayAvailable  True
```

如果 `chatGatewayAvailable=False`，不要先归因 MF_EP 页面；先检查当前运行进程是否是最新构建、`MF_AGENT_SPRING_AI_CHAT_MODEL=openai` 是否设置、以及启动日志中是否有 DeepSeek/Spring AI 错误。`SpringAiCompletionGateway` 会对失败做脱敏日志并回退到受控回答。

### 2.3 重要环境变量

| 变量 | 含义 | 默认/建议 |
| --- | --- | --- |
| `MF_AGENT_LLM_ENABLED` | 是否允许真实模型调用 | `false`；真实环境显式设 `true` |
| `DEEPSEEK_API_KEY` | DeepSeek 密钥 | 仅环境变量 |
| `MF_AGENT_SPRING_AI_CHAT_MODEL` | Spring AI chat provider | DeepSeek 时 `openai` |
| `MF_AGENT_OPENAI_BASE_URL` | DeepSeek OpenAI 兼容地址 | `https://api.deepseek.com` |
| `MF_AGENT_OPENAI_MODEL` | 模型名称 | 由启动脚本参数传入 |
| `MF_RAG_ENABLED` | 是否启用 Qdrant + Embedding 混合检索 | 默认 `false` |
| `MF_QDRANT_URL` / `MF_EMBEDDING_BASE_URL` | V2 RAG 基础设施地址 | 启用 RAG 时设置 |
| `MF_AGENT_EVALUATION_KEY` | 评测、索引管理内部接口密钥 | 非空且不下发前端 |

## 3. 对外接口契约

### 3.1 聊天

```http
POST /api/agent/chat
POST /api/agent/chat/stream
```

请求字段：

```json
{
  "sessionId": "mf-ep-client-1001-default",
  "message": "苹果树冬剪五要点",
  "userId": "1001",
  "userType": "client",
  "authToken": "Bearer 用户登录Token"
}
```

订单问题必须携带当前登录用户 token；Agent 不以订单号绕过身份验证，也不执行退款、改订单、取消订单、确认收货或商家审核。

普通响应核心字段：

```text
answer / intent / resolved / usedTools / conversationId / fallbackReason
sources / confidence / reviewRequired / knowledgeGap
```

`knowledgeGap` 是本轮新增的可选字段，仅在知识不足或未知意图时返回：

```json
{
  "topic": "苹果树炭疽病怎么处理",
  "reason": "knowledge_not_enough",
  "riskLevel": "medium",
  "suggestedContentTypes": ["faq", "article", "encyclopedia"]
}
```

该字段为向后兼容的新增字段，旧前端忽略即可；后续 DataCenter 工作台可用它做知识缺口聚合。

### 3.2 流式聊天

`POST /api/agent/chat/stream` 为 POST SSE，MF_EP 前端必须使用 `fetch` + `ReadableStream`，不能用仅支持 GET 的原生 `EventSource`。

事件顺序：

```text
status: thinking
status: working
token: 文本片段（多次）
result: 完整 AgentChatResponse
status: success | doubt | error
```

当前 `token` 是获取完整回答后切片推送的展示流；并非 DeepSeek 原生 token 流。若要首 token 实时到达，需要将 `SpringAiCompletionGateway` 改为 Spring AI reactive streaming 调用，并保持相同 SSE 事件契约。

### 3.3 运维与知识接口

```http
GET  /api/agent/runtime/status
GET  /api/agent/knowledge/status
POST /api/agent/knowledge/reindex
POST /api/agent/knowledge/sync
POST /api/agent/evaluate
```

除 runtime/status 外，评测和知识管理接口需要 `X-MF-Evaluation-Key`。前端不得调用这些接口。

## 4. 回答与安全编排

主编排类：`agent/CustomerAgentService.java`。

当前意图包括：

```text
GREETING / HELP / COMPANY / DIRECT
PRODUCT / ENCYCLOPEDIA / ORDER / MERCHANT
UNSAFE_ACTION / UNKNOWN
```

规则职责是识别安全边界和必须查询事实的业务类型；DeepSeek 仅负责开放对话的自然理解与表达，不能取代商品、订单、商家等工具事实。

已处理的体验问题：

- `你会什么` 命中 `HELP`；
- `你是哪个公司的客服` 命中 `COMPANY`，稳定回复平台介绍；
- `UNKNOWN` 不再无条件继承上一个意图；只有“那… / 这个… / 还有… / 继续…”等短追问才继承；
- 会话记忆保存“意图 + 上一轮问题文本”，而非只保存意图；
- “果树冬剪五要点”后补充“陕西、五年的苹果树”时，知识检索会合并上一轮主题，而不是重新检索成施肥问题；
- 订单无 token 保持安全拒答，不进入“知识不足”逻辑。

会话记忆仅在内存中，默认 20 分钟、4 轮。它用于短期上下文，不负责 MF_EP 页面历史回放；用户聊天历史需要由 MF_EP + DataCenter 的会话历史接口完成。

## 5. 平台事实卡与自然聊天

受版本控制的平台事实卡位置：

```text
src/main/resources/agent/platform-profile.md
```

加载类：`agent/PlatformProfileService.java`。它会在开放式 DeepSeek 对话时附加到系统提示词。

事实卡仅维护：平台身份、可协助事项、服务边界、隐私要求、风险提示和回复风格。商品价格、库存、订单状态、平台临时活动、未发布百科内容等实时事实不得写入事实卡，必须通过 MF_EP 查询。

修改规则：产品/运营确认后修改 Markdown，代码评审并重启服务生效。

## 6. 知识检索与 RAG

### 6.1 当前知识来源

`KnowledgeRefreshService` 会在启动时和默认每 10 分钟从 MF_EP 拉取：

```text
已发布百科 / FAQ / 文章
```

若 MF_EP 内容很少，客服回答也会不完整；Agent 无法凭空补充权威农技事实。优先补充 MF_EP 的高频、结构化内容，而不是在 Agent 写死业务知识。

### 6.2 轻量检索安全修正

`rag/KnowledgeRetrievalService.java` 已收紧泛标签匹配：

- “苹果树”“果树”“养护”等只是弱信号；
- 必须再命中具体病害、作业主题或标题，才作为正式检索命中；
- 例如“苹果树炭疽病”不会再错误命中“苹果树腐烂病”。

无可靠命中时，客服返回“无法可靠判断”，并产生 `knowledge_not_enough`，而不是拿相近文章硬答。

### 6.3 混合 RAG

`MF_RAG_ENABLED=false` 时，只用 Agent 内存中的关键词知识索引。

启用后由 `HybridRagService` 合并关键词召回与 Qdrant 向量召回；Embedding 服务和 Qdrant 仅保存公开知识切片与非敏感元数据，绝不保存用户问题、订单 token、个人资料或完整会话。

混合 RAG 基础设施位于项目根目录的 `MF_EmbeddingService` 与 `docker-compose.rag.yml`。使用前需完成单独部署和联调；不要仅设置 `MF_RAG_ENABLED=true` 就假设向量检索可用。

## 7. 知识缺口与 DataCenter 问题池

新增类：`agent/KnowledgeGapAdvisor.java`。

触发条件：

- `knowledge_not_enough`；
- `UNKNOWN` / `intent_unknown`。

不会把 `upstream_timeout`、`upstream_unavailable` 等运行故障误判为内容缺口。

知识缺口包含：

```text
topic / reason / riskLevel / suggestedContentTypes
```

风险规则：农药、药剂、剂量、中毒、法律、退款、支付为 `high`；病虫害、施肥、用药、修剪为 `medium`；其他为 `low`。

在保持现有 DataCenter 接口不变的前提下，Agent 仍调用 `/api/ai/unresolved-questions`，并把 topic、risk、建议内容类型写入 `remark`。这让 DataCenter 后续能先读取现有问题池，再逐步升级为“知识运营工作台”。

Agent 不具备、也不应擅自获得以下权限：联网结果自动发布、直接写 MF_EP 公开内容、直接自动审核高风险农技内容。相关工作流见：

```text
Folder_description/MF_AI知识补全闭环与一人运营方案.md
```

## 8. 与 MF_EP / DataCenter 的待联调事项

### MF_EP

- 内容端补充已发布百科、FAQ、科普文章，并提供作物、树龄、季节、地区、主题等标签；
- 实现 AI 内容草稿、审核、发布、下线和版本回滚；
- 只有 `published` 内容可被 Agent 同步和公开检索；
- MF_EP 前端继续消费 stream 的 `token` 和 `result`，并实现聊天历史本地缓存/服务端恢复。

### MF_DataCenter

- 将问题池升级为知识运营工作台：相似问题聚合、来源、AI 草稿、AI 预审、发布审计；
- 提供用户会话历史受控查询/删除能力；
- 读取 `knowledgeGap` 或 unresolved remark 的 topic/risk/suggested 信息；
- 不能把运营查询接口直接暴露给普通用户前端。

对应交接文件：

```text
Folder_description/MF_EP_AI客服会话历史接入任务书.md
Folder_description/MF_DataCenter_AI客服会话历史接口任务书.md
Folder_description/MF_AI知识补全闭环与一人运营方案.md
```

## 9. 验证记录

最近一次验证：

```text
cd F:\20260518-xiangmu\MF_Project\MF_AgentService
mvn test

BUILD SUCCESS
Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
```

已覆盖：商品查询、订单无 token 安全边界、百科检索、上游失败、DataCenter 写入失败、问候/帮助/公司介绍、会话主题补充、知识缺口、泛作物标签不误命中病害、平台事实卡加载。

构建中存在 Spring Boot `RestClientConfig` 过时 API 警告，但本次测试成功；该警告不是当前运行阻塞项，应在独立依赖升级任务中处理。

## 10. 后续负责人优先顺序

1. 重启后用 `/api/agent/runtime/status` 验证当前进程是否真的启用 DeepSeek。
2. 与 MF_EP 补齐已发布内容和标签，优先 10～30 个高频主题。
3. 与 DataCenter 联调知识缺口聚合和聊天历史，不要把问题池当正式知识库。
4. 建立 MF_EP 草稿/发布闭环后，再实现联网研究、AI 预审和一键发布。
5. 最后才将 Spring AI 改为原生 token 流和考虑低风险自动发布。

