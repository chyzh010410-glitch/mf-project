# MF_EP AI知识补全草稿发布接口进度说明

面向：MF_DataCenter、MF_AgentService、MF_EP 负责人  
更新时间：2026-07-13  
对应方案：`MF_AI知识补全闭环与一人运营方案.md`

## 1. 当前结论

MF_EP 已经补上 DataCenter 所需的第一层“受控内容落点”：

- DataCenter 可以先建设“AI 知识补全”工作台前半段，包括问题池、知识缺口聚合、候选来源、AI 草稿、预审结果、风险等级和审计展示。
- DataCenter 现在可以通过 MF_EP 的内部接口创建 AI 内容草稿、更新草稿、查询草稿、发布草稿、下线已发布内容。
- “一键发布 / 下线 / 回滚”中的发布和下线已经有 MF_EP 内部接口落点，可以进入联调；完整版本回滚和更细的内容后台人工编辑还未完成。
- MF_EP 仍是正式内容事实源。DataCenter 不应直接绕过 MF_EP 写公开 FAQ、百科或文章。

## 2. MF_EP 已完成内容

### 2.1 AI 内容草稿表

新增数据库脚本：

```text
MF_EP/projects/mf/mf-fertilizer/fertilizer-api/src/main/resources/db/ai-content-draft-v1.sql
```

新增表：

```text
ai_content_draft
```

关键字段：

```text
content_type        faq / article / encyclopedia
title               标题；FAQ 场景下作为 question
summary             摘要
content             正文；FAQ 场景下作为 answer
tags                标签
crop                作物
tree_age            树龄
season              季节
region              地区
risk_level          风险等级
source_references   来源引用 JSON
ai_review_json      AI 预审 JSON
status              draft / pending_publish / published / offline / rejected
version             草稿版本
mf_ep_content_id    发布后对应的 MF_EP 正式内容 ID
published_by        发布操作者
published_at        发布时间
```

### 2.2 内部接口

新增控制器：

```text
MF_EP/projects/mf/mf-fertilizer/fertilizer-api/src/main/java/com/mf/fertilizer/ai/controller/internal/InternalAiContentController.java
```

接口：

```http
POST /internal/ai-content/drafts
PUT  /internal/ai-content/drafts/{id}
POST /internal/ai-content/drafts/{id}/publish
POST /internal/ai-content/{id}/offline
GET  /internal/ai-content/{id}
```

内部鉴权：

```http
X-MF-Internal-Token: <与 MF_EP 配置一致的服务凭据>
```

MF_EP 配置项：

```yaml
mf:
  internal-token: change-me
```

### 2.3 发布落点

MF_EP 发布草稿时会根据 `contentType` 写入现有正式内容表：

| contentType | 正式内容表 | 发布行为 |
| --- | --- | --- |
| `faq` | `faq` | `title` 写入 question，`content` 写入 answer，`isPublished=1` |
| `article` | `encyclopedia_article` | 写入科普文章，`isPublished=1` |
| `encyclopedia` | `encyclopedia_entry` | 写入百科条目，`isPublished=1` |

发布后，草稿表会记录：

```text
status=published
mf_ep_content_id=<正式内容ID>
published_by=<operator>
published_at=<当前时间>
```

### 2.4 下线能力

`POST /internal/ai-content/{id}/offline` 已支持：

- 根据草稿记录找到 `mf_ep_content_id`；
- 将正式内容的 `isPublished` 置为 `0`；
- 将草稿状态改为 `offline`。

## 3. DataCenter 可开始联调的部分

DataCenter 可以先建设工作台前半段，不必完全等待 MF_EP 后续后台页面：

1. 知识缺口列表：问题池、相似问题聚合、风险等级、最近出现时间。
2. 候选来源列表：标题、链接、摘要、可信度、抓取时间。
3. AI 草稿列表：FAQ / 百科 / 文章类型、标题、内容预览、标签、来源、AI 预审结果。
4. 操作按钮：生成草稿、更新草稿、查看草稿、发布草稿、下线内容。
5. 审计记录：记录 DataCenter 操作者、候选版本、MF_EP 草稿 ID、MF_EP 正式内容 ID、操作时间。

建议 DataCenter 先把“一键发布”按钮做成真实调用 MF_EP 内部接口，不要模拟发布。

## 4. 接口示例

### 4.1 创建草稿

```http
POST http://localhost:8080/internal/ai-content/drafts
X-MF-Internal-Token: change-me
Content-Type: application/json
```

```json
{
  "contentType": "faq",
  "title": "苹果树冬剪需要注意什么？",
  "summary": "苹果树冬季修剪基础问答",
  "content": "苹果树冬剪应优先去除病弱枝、交叉枝和过密枝，保留主枝结构，并结合树龄、树势和当地气候调整修剪强度。",
  "tags": "苹果树,冬剪,养护",
  "crop": "苹果树",
  "treeAge": "五年左右",
  "season": "冬季",
  "region": "陕西",
  "riskLevel": "low",
  "sourceReferences": "[{\"title\":\"来源标题\",\"url\":\"https://example.com\"}]",
  "aiReviewJson": "{\"sourceCompleteness\":95,\"recommendedAction\":\"manual_publish\"}",
  "createdBy": "datacenter"
}
```

### 4.2 发布草稿

```http
POST http://localhost:8080/internal/ai-content/drafts/{id}/publish
X-MF-Internal-Token: change-me
Content-Type: application/json
```

```json
{
  "operator": "datacenter-admin"
}
```

高风险内容保护：

```text
riskLevel = high / HIGH / 高 / 高风险
```

以上风险等级当前禁止通过内部接口直接发布。

### 4.3 下线内容

```http
POST http://localhost:8080/internal/ai-content/{id}/offline
X-MF-Internal-Token: change-me
Content-Type: application/json
```

```json
{
  "operator": "datacenter-admin"
}
```

## 5. MF_EP 尚未完成内容

以下能力还未完成，DataCenter 工作台可以预留入口，但不要把它们当作已可用能力：

- 完整版本回滚：当前有草稿 `version` 字段，但没有独立版本历史表。
- MF_EP 管理后台里的 AI 草稿列表、人工编辑、来源引用预览页面。
- 发布后主动通知 AgentService 增量同步。当前可由 DataCenter 发布成功后再调用 AgentService 同步接口。
- 更细粒度的来源校验：当前 MF_EP 保存来源 JSON，但不对来源可信度做深度判断；来源预审仍应由 AgentService / DataCenter 完成。
- 高风险内容人工复核工作流：当前只做“禁止直接发布”的硬保护。

## 6. 验证结果

已在 MF_EP 后端执行：

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_EP\projects\mf\mf-fertilizer
mvn -pl fertilizer-api -am test -DskipTests
```

结果：

```text
BUILD SUCCESS
```

## 7. 给 DataCenter 的对接建议

DataCenter 当前可以并行推进工作台，但发布按钮建议按以下状态设计：

```text
候选草稿未写入 MF_EP：显示“创建 MF_EP 草稿”
已创建 MF_EP 草稿：显示“发布到 MF_EP”
已发布：显示 MF_EP contentId，并允许“下线”
高风险：禁用直接发布，提示人工复核
发布失败：保留候选来源、草稿和错误信息，不丢任务
```

DataCenter 侧需要保存 MF_EP 返回的两个 ID：

```text
aiContentDraft.id      MF_EP 草稿 ID
mfEpContentId          MF_EP 正式内容 ID
```

这两个 ID 后续用于审计、下线、重新同步和效果追踪。
