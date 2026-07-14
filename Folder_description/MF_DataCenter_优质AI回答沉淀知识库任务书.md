# MF_DataCenter 优质 AI 回答沉淀知识库任务书

## 1. 任务背景

`MF_AgentService` 已经把电商平台 AI 客服的问答数据写入数据中台：

- 全量会话写入 `dc_ai_conversation_log`；
- 无法解决或知识不足的问题写入 `dc_unresolved_question`；
- 百科、商家类且答案长度不少于 40 字的优质回答，写入 `dc_sample_candidate`。

数据中台现有知识工作台能够管理知识缺口、内容候选、MF_EP 草稿和发布，但优质回答样本尚不能进入该工作台。因此目前“数据已留存”，但“审核后的优秀回答可成为正式知识”这条链路没有闭环。

本任务由 `MF_DataCenter` 负责人完成。`MF_AgentService` 无需修改，也不应直接写入正式知识库。

## 2. 目标与边界

### 2.1 目标

在数据中台的样本候选池中，为符合条件的优质 AI 回答提供“创建知识草稿”能力，使其进入已有的知识工作台和 MF_EP 发布流程。

完成后的业务流如下：

```text
MF_AgentService
  -> dc_sample_candidate（待审核样本）
  -> 人工审核：approved + recommendedForKnowledge=true
  -> 数据中台：创建知识缺口 + 内容候选草稿
  -> 数据中台：创建 MF_EP 草稿
  -> 人工复核并发布到 MF_EP
```

### 2.2 非目标

- 不允许 AgentService 的回答自动发布到正式百科、FAQ 或文章。
- 不修改 `MF_EP` 业务库表，也不让 AgentService 直连数据中台数据库。
- 不在本任务中引入自动生成、自动审核或自动发布机制。
- 不改变现有未解决问题聚合为知识缺口的逻辑；本任务是补充优质回答样本的人工转入路径。

## 3. 当前已具备的能力

### 3.1 AgentService 到数据中台

AgentService 已调用以下接口：

```text
POST /api/ai/conversations
POST /api/ai/unresolved-questions
POST /api/ai/sample-candidates
```

其中样本候选包含 `conversationId`、`question`、`answer`、`source`、`qualityStatus`、`reviewStatus` 和 `recommendedForKnowledge`。

### 3.2 数据中台已有知识工作台

当前已有接口：

```text
POST /api/ai/knowledge/gaps
POST /api/ai/knowledge/gaps/{gapId}/candidates
POST /api/ai/knowledge/candidates/{candidateId}/mf-ep-draft
POST /api/ai/knowledge/candidates/{candidateId}/publish
```

内容候选创建必须绑定一个知识缺口，因此样本转草稿时需要同时创建或选择一个知识缺口。

## 4. 需要实现的功能

### 4.1 样本候选池操作入口

在数据中台“样本候选池”页面中，为每条样本增加“创建知识草稿”操作。

该操作仅在以下条件全部满足时可用：

```text
reviewStatus = approved
recommendedForKnowledge = true
```

不满足条件时，按钮应禁用或不展示，并明确体现该条数据尚未审核通过或尚未推荐入库。

### 4.2 创建知识草稿接口

新增一个由数据中台内部实现的接口，建议：

```text
POST /api/ai/knowledge/sample-candidates/{sampleId}/draft
```

请求体建议：

```json
{
  "contentType": "encyclopedia",
  "title": "草稿标题",
  "tags": "标签1,标签2",
  "riskLevel": "low"
}
```

接口执行规则：

1. 查询 `dc_sample_candidate`，样本不存在时返回明确错误。
2. 校验该样本已经 `approved`，且 `recommendedForKnowledge=true`；否则拒绝创建。
3. 防重复：同一个 `sampleId` 已成功生成内容候选时，返回已有候选或明确提示，不得重复创建。
4. 创建一条 `dc_ai_knowledge_gap`：
   - `normalized_topic` 使用样本问题或人工填写的标题；
   - `sample_question` 使用样本 `question`；
   - `occurrence_count=1`；
   - `risk_level` 使用请求中的值，默认 `low`；
   - 初始状态按现有工作台约定设置。
5. 创建一条 `dc_ai_content_candidate`：
   - `gap_id` 为上一步知识缺口 ID；
   - `content_type`、`title`、`tags` 取请求值；
   - `content` 直接使用样本 `answer`；
   - `ai_review_json` 至少记录 `sampleId`、`conversationId`、`source`、审核人和审核备注，便于追溯；
   - 状态复用既有 `ai_reviewed`。
6. 返回新建的知识缺口和内容候选 ID，供页面跳转到知识工作台。

### 4.3 数据关联与迁移

为 `dc_ai_content_candidate` 新增来源样本关联字段：

```text
source_sample_id BIGINT NULL
```

新增 Flyway 迁移脚本，不能修改已有迁移文件。建议增加唯一索引：

```text
UNIQUE KEY uk_dc_ai_content_candidate_source_sample (source_sample_id)
```

这样可在数据库层防止同一优质回答被重复沉淀。

### 4.4 与既有发布链路衔接

新建内容候选后，后续一律复用已有流程：

```text
内容候选
  -> 创建 MF_EP 草稿
  -> 人工复核
  -> 发布
```

高风险内容必须保留现有的人工复核限制，不允许通过本功能绕过。

## 5. 建议改动位置

```text
MF_DataCenter/datacenter-api/src/main/java/com/mf/datacenter/ai/
  AiController.java
  AiDataStore.java

MF_DataCenter/datacenter-api/src/main/java/com/mf/datacenter/knowledge/
  KnowledgeWorkbenchController.java
  KnowledgeWorkbenchService.java
  entity/ContentCandidateEntity.java

MF_DataCenter/datacenter-api/src/main/resources/db/migration/
  Vxx__link_content_candidate_to_sample.sql

MF_DataCenter/datacenter-web/src/
  样本候选池页面及知识工作台页面
```

可以将新接口放在 `KnowledgeWorkbenchController`，以保证知识草稿创建逻辑集中在知识工作台；样本查询和审核状态校验可复用 `AiDataStore` 或对应 Mapper。

## 6. 验收标准

### 6.1 正向验收

1. AgentService 写入一条样本候选后，数据中台样本候选池可查询到 `question` 和 `answer`。
2. 管理员将该样本审核为 `approved` 且勾选推荐入库后，可执行“创建知识草稿”。
3. 操作成功后：
   - 新增一条 `dc_ai_knowledge_gap`；
   - 新增一条 `dc_ai_content_candidate`；
   - 候选正文与样本 `answer` 一致；
   - 候选记录可追溯到原始 `sampleId` 和会话 ID。
4. 内容候选可继续调用现有接口创建 MF_EP 草稿；低风险内容按原流程可发布。

### 6.2 反向验收

1. `pending`、`rejected` 或 `recommendedForKnowledge=false` 的样本不能创建知识草稿。
2. 对同一 `sampleId` 连续调用两次创建接口，不得生成两份内容候选。
3. 高风险内容不能跳过现有人工复核限制直接发布。
4. 未解决问题聚合和原有知识工作台接口保持可用。

### 6.3 最低验证

```text
1. datacenter-api：mvn test
2. datacenter-web：npm run build
3. 使用一条 approved + recommendedForKnowledge=true 的样本完成一次端到端手工验证
```

## 7. 交付物

请交付：

1. 后端接口、服务逻辑、实体字段和 Flyway 迁移。
2. 样本候选池页面操作入口及成功后跳转/提示。
3. 覆盖资格校验、防重复和成功创建的自动化测试。
4. 一份简短说明，记录新增接口、请求示例、状态流转和验证结果。

## 8. 对接约定

本任务完成后，`MF_AgentService` 不需要新增调用。它继续只负责写入会话、未解决问题和样本候选；数据中台负责人负责在中台内完成“审核 -> 知识草稿 -> MF_EP 发布”的治理闭环。
