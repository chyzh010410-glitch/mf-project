# MF AI 知识补全闭环与一人运营方案

面向：MF_AgentService、MF_DataCenter、MF_EP 负责人  
更新时间：2026-07-13  
目标：让苗丰 AI 客服能够发现知识缺口、获取可信候选资料、自动生成内容草稿，并让单人运营者以最少操作完成审核与发布。

## 1. 结论与原则

苗丰不应把互联网搜索结果直接当作正式知识库，也不应要求运营人员逐篇手写、逐条审核。推荐建设“AI 知识补全闭环”：

```text
用户问题未被可靠解决
  -> DataCenter 聚合为知识缺口
  -> AgentService 检索可信公开资料并生成候选答案
  -> AI 预审来源、冲突、风险与重复度
  -> 自动生成 MF_EP 的 FAQ / 百科补充 / 科普文章草稿
  -> 运营者在一个工作台一键发布、退回或忽略
  -> MF_EP 发布内容
  -> AgentService 增量同步并进入检索范围
```

核心原则：

1. **MF_EP 是正式内容事实源。** 只有 MF_EP 中 `published` 状态的百科、FAQ、文章可被 Agent 作为正式知识回答。
2. **DataCenter 是运营决策台。** 用于聚合问题、查看来源、AI 预审和一键操作；不取代 MF_EP 的内容所有权。
3. **AgentService 是知识补全执行器。** 负责发现缺口、检索、生成候选、预审与同步；不直接绕过审核写入公开内容。
4. **“AI 发布”应理解为 AI 预审和自动草稿。** 高风险农技内容不能无条件自动公开。
5. **每一步可追溯、可下线、可回滚。** 必须保留来源、生成时间、模型/规则版本、审核人和发布记录。

## 2. 单人运营的实际工作方式

运营者不需要打开三个系统反复复制内容。日常只进入 MF_DataCenter 的“AI 知识补全”工作台：

```text
待补全主题：苹果树冬剪
近 7 天：18 次相似未解决咨询

AI 预审：可生成
可信度：92 / 100
风险：低
来源：3 个，均可查看
建议内容：
  - FAQ：五年苹果树冬剪重点
  - 文章：陕西苹果树冬剪与剪后管理
  - 百科补充：苹果树 - 冬季修剪

[查看依据] [一键生成草稿] [忽略]
```

生成草稿后，同一页面展示预览与 AI 检查结论：

```text
草稿状态：AI 预审通过
来源完整度：95
与已发布内容重复度：低
适用范围：陕西 / 五年左右苹果树
农技风险：低

[一键发布] [退回 AI 重写] [删除]
```

“一键发布”由 DataCenter 发起受控内部调用，将 MF_EP 中对应草稿改为 `published`；发布成功后触发 AgentService 增量同步。运营者不需要手动复制粘贴文章。

## 3. 风险分级与自动化规则

| 级别 | 示例 | AI 能力 | 人工动作 |
| --- | --- | --- | --- |
| 低风险 | 平台介绍、公开功能说明、基础养护常识、已有内容改写 | 自动检索、生成、预审；可配置自动生成草稿 | 可一键发布；成熟后可允许自动发布 |
| 中风险 | 施肥建议、地区/树龄差异、基础病害处理 | 自动检索、生成、预审 | 必须一键确认后发布 |
| 高风险 | 农药剂量、严重病害诊断、法规、订单/退款政策 | 仅生成候选草稿并标风险 | 必须人工核验；默认不可自动发布 |

任何级别都必须满足：来源可展示、未与现有已发布内容矛盾、内容包含适用范围与必要风险提示。没有可靠来源时，AI 只能保留为候选，不得生成公开内容。

## 4. MF_AgentService 负责的工作

### 4.1 发现和聚合知识缺口

- 当出现 `knowledge_not_enough`、低检索置信度、用户低评分或重复追问时，向 DataCenter 写入知识缺口事件。
- 事件至少包含：问题、标准化主题、作物、树龄、地区、季节、意图、会话 ID、出现次数、最近时间、风险等级建议。
- 对相似问题进行归并，避免“苹果树冬剪怎么剪”和“果树冬天修剪”生成大量重复任务。

### 4.2 检索与候选答案生成

- 仅在知识缺口场景启动外部资料检索；正常命中的本地知识不重复搜索网络。
- 检索来源使用可配置白名单和信誉分，不接受任意网页作为正式依据。
- 保存每个来源的标题、链接、抓取时间、发布/更新时间、摘要、来源类型和可信度；不复制整篇受版权保护文章。
- 基于候选来源生成：短 FAQ、百科补充段落、完整科普文章三个可选草稿。
- 草稿必须带标签：作物、树龄、季节、地区、主题、适用范围、风险等级、来源引用。

### 4.3 AI 预审

对每个草稿输出结构化审核结果：

```json
{
  "sourceCompleteness": 95,
  "sourceAuthority": 88,
  "conflictRisk": "low",
  "duplicateRisk": "low",
  "agricultureRisk": "medium",
  "recommendedAction": "manual_publish",
  "reasons": ["适用于陕西五年左右苹果树", "未给出农药剂量"]
}
```

- 预审只做推荐，不能替代高风险内容的人工责任。
- 检测与 MF_EP 已发布内容的重复/矛盾；发现冲突时默认退回候选。
- 对药剂、剂量、严重病害、法规和订单规则一律标记高风险。

### 4.4 同步与回答边界

- 只索引 MF_EP 中已发布的 FAQ、百科、文章；草稿和候选资料不得进入正式 RAG。
- 发布后调用增量同步，记录索引版本、文档数、失败原因。
- 回答用户时，网络候选只能以“参考公开资料”形式谨慎展示，不能伪装为苗丰官方结论；正式回答优先使用已发布内容。

## 5. MF_DataCenter 负责人工作清单

### 5.1 新增知识补全工作台

在 AI 分析/运营模块增加“AI 知识补全”页面，至少包含：

- 待补全主题列表：相似问题数量、最近出现时间、低评分数量、风险等级；
- 候选来源列表：标题、链接、时间、摘要、可信度；
- AI 草稿列表：FAQ / 百科 / 文章类型、预览、标签、审核结果；
- 操作：`一键生成草稿`、`退回 AI 重写`、`忽略`、`一键发布`、`下线`；
- 发布和下线审计记录。

### 5.2 建议数据模型

| 实体 | 关键字段 |
| --- | --- |
| `ai_knowledge_gap` | id、normalized_topic、sample_question、count、risk_level、status、last_seen_at |
| `ai_research_source` | gap_id、title、url、publisher、published_at、retrieved_at、summary、authority_score |
| `ai_content_candidate` | gap_id、content_type、title、content、tags、risk_level、ai_review_json、status |
| `ai_content_publish_log` | candidate_id、mf_ep_content_id、action、operator、created_at、remark |

建议状态：

```text
knowledge gap: pending -> researching -> draft_ready -> published / ignored
content candidate: generated -> ai_reviewed -> pending_publish -> published / rejected / offline
```

### 5.3 对外（内部）接口

建议提供受内部鉴权保护的接口：

```http
GET  /api/ai/knowledge-gaps
POST /api/ai/knowledge-gaps/{id}/research
POST /api/ai/knowledge-gaps/{id}/drafts
GET  /api/ai/content-candidates
POST /api/ai/content-candidates/{id}/ai-review
POST /api/ai/content-candidates/{id}/publish
POST /api/ai/content-candidates/{id}/reject
POST /api/ai/content-candidates/{id}/offline
```

- `publish` 只能由具备运营权限的用户操作；
- DataCenter 调用 MF_EP 内部草稿/发布接口时使用服务凭据；
- 所有动作记录操作者、时间、候选版本和 MF_EP 内容 ID；
- 不向普通用户前端开放该模块。

### 5.4 验收

1. 多条相似未解决问题能聚成一个知识缺口。
2. 运营者能看到来源、草稿、AI 预审和风险提示。
3. 一键发布后有可查询审计记录和 MF_EP 内容 ID。
4. 高风险候选不能绕过人工确认直接发布。
5. 发布失败时候选保留，不丢失来源和草稿。

## 6. MF_EP 负责人工作清单

### 6.1 内容草稿与版本能力

MF_EP 是 FAQ、百科、科普文章的正式内容所有者。请为三类内容统一支持：

```text
draft（草稿）
pending_publish（待发布）
published（已发布）
offline（下线）
```

每个内容版本至少保存：

```text
contentId、type、title、content、summary、tags、crop、treeAge、season、region、riskLevel、sourceReferences、status、version、createdBy、publishedBy、publishedAt
```

### 6.2 内部草稿接口

仅供 DataCenter 通过服务鉴权调用，不直接暴露给浏览器：

```http
POST /internal/ai-content/drafts
PUT  /internal/ai-content/drafts/{id}
POST /internal/ai-content/drafts/{id}/publish
POST /internal/ai-content/{id}/offline
GET  /internal/ai-content/{id}
```

行为要求：

- `POST /drafts` 创建 FAQ、百科补充或文章草稿，返回 MF_EP 内容 ID；
- `publish` 校验候选来源、风险级别和调用方权限，再切换为 `published`；
- 任何发布、下线、回滚产生新版本或审计记录；
- 对外公开接口与 Agent 同步接口只返回 `published` 内容；
- 发布成功后向 AgentService 发出增量同步事件，或由 DataCenter 调用 Agent 的同步接口。

### 6.3 内容后台页面

MF_EP 内容后台至少提供：

- AI 草稿标识与来源引用预览；
- 正式编辑、发布、下线、版本回滚；
- 标签/作物/树龄/季节/地区等结构化字段编辑；
- 与已发布文章/FAQ 的重复内容提醒；
- 发布后公开详情链接，供 Agent 在 `sources` 中引用。

### 6.4 验收

1. DataCenter 可创建草稿，但不能直接写成公开文章。
2. 草稿发布后仅从 MF_EP 的公开发布接口可读取。
3. 下线后内容不再出现在 AgentService 的下一次同步索引中。
4. 所有内容都有来源引用、风险等级、版本和发布审计。
5. 普通前端用户不能调用内部草稿/发布接口。

## 7. 推荐实施顺序

1. **先补 MF_EP 内容状态与内部草稿接口。** 没有草稿/发布边界，自动化会污染正式知识。
2. **DataCenter 建知识缺口与候选草稿工作台。** 先支持人工创建/查看，再接 AI 自动生成。
3. **AgentService 接入缺口归并、来源检索、草稿生成和 AI 预审。** 先覆盖低风险 FAQ。
4. **联调一键发布与 Agent 增量同步。** 验证“发布后可回答、下线后不再引用”。
5. **最后配置低风险自动发布。** 必须有来源白名单、审计、下线和回滚能力后再启用。

## 8. 首批试点范围

先做 10 个低风险、高频主题，而不是一次铺大量文章：

- 苹果树冬剪与剪后管理；
- 苹果树黄叶排查；
- 果树四季施肥原则；
- 果树浇水常见误区；
- 苗木移栽后的缓苗管理；
- 平台商品如何筛选；
- 平台订单查询边界；
- 商家入驻基础流程。

试点验收通过后，再逐步覆盖地区差异、树龄分层、病虫害专题和更多作物。

