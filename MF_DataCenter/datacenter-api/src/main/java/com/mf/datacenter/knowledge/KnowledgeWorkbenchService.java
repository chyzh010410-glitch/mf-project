package com.mf.datacenter.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mf.datacenter.ai.AiDataStore;
import com.mf.datacenter.ai.entity.SampleCandidateEntity;
import com.mf.datacenter.ai.mapper.SampleCandidateMapper;
import com.mf.datacenter.knowledge.entity.ContentCandidateEntity;
import com.mf.datacenter.knowledge.entity.ContentPublishLogEntity;
import com.mf.datacenter.knowledge.entity.ContentSyncLogEntity;
import com.mf.datacenter.knowledge.entity.KnowledgeGapEntity;
import com.mf.datacenter.knowledge.entity.ResearchSourceEntity;
import com.mf.datacenter.knowledge.mapper.ContentCandidateMapper;
import com.mf.datacenter.knowledge.mapper.ContentPublishLogMapper;
import com.mf.datacenter.knowledge.mapper.ContentSyncLogMapper;
import com.mf.datacenter.knowledge.mapper.KnowledgeGapMapper;
import com.mf.datacenter.knowledge.mapper.ResearchSourceMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeWorkbenchService {
    private final KnowledgeGapMapper gaps;
    private final ResearchSourceMapper sources;
    private final ContentCandidateMapper candidates;
    private final AiDataStore aiDataStore;
    private final MfEpAiContentClient mfEp;
    private final ObjectMapper objectMapper;
    private final SampleCandidateMapper sampleCandidates;
    private final ContentPublishLogMapper publishLogs;
    private final ContentSyncLogMapper syncLogs;
    private final MfAgentKnowledgeSyncClient agentSync;

    public KnowledgeWorkbenchService(ObjectProvider<KnowledgeGapMapper> gaps, ObjectProvider<ResearchSourceMapper> sources,
                                     ObjectProvider<ContentCandidateMapper> candidates, ObjectProvider<SampleCandidateMapper> sampleCandidates, ObjectProvider<ContentPublishLogMapper> publishLogs, ObjectProvider<ContentSyncLogMapper> syncLogs,
                                     AiDataStore aiDataStore, MfEpAiContentClient mfEp, MfAgentKnowledgeSyncClient agentSync) {
        this.gaps = gaps.getIfAvailable(); this.sources = sources.getIfAvailable(); this.candidates = candidates.getIfAvailable();
        this.sampleCandidates = sampleCandidates.getIfAvailable(); this.publishLogs = publishLogs.getIfAvailable(); this.syncLogs = syncLogs.getIfAvailable(); this.aiDataStore = aiDataStore; this.mfEp = mfEp; this.agentSync = agentSync; this.objectMapper = new ObjectMapper();
    }

    public List<KnowledgeGapEntity> gaps() { requireMappers(); return gaps.selectList(new LambdaQueryWrapper<KnowledgeGapEntity>().orderByAsc(KnowledgeGapEntity::getStatus).orderByDesc(KnowledgeGapEntity::getLastSeenAt)); }
    public int aggregateFromQuestionPool() {
        requireMappers(); int count = 0;
        for (var question : aiDataStore.unresolvedQuestions(null, null)) {
            if (!List.of("pending", "processing").contains(question.status())) continue;
            String topic = topicOf(question.remark(), question.question());
            var existing = gaps.selectOne(new LambdaQueryWrapper<KnowledgeGapEntity>().eq(KnowledgeGapEntity::getNormalizedTopic, topic));
            if (existing == null) { createGap(new GapRequest(topic, question.question(), 1, 1, question.priority())); }
            else { existing.setOccurrenceCount(existing.getOccurrenceCount() + 1); existing.setLowScoreCount(existing.getLowScoreCount() + 1); existing.setSampleQuestion(question.question()); existing.setLastSeenAt(LocalDateTime.now()); existing.setUpdateTime(LocalDateTime.now()); gaps.updateById(existing); }
            count++;
        }
        return count;
    }
    public KnowledgeGapEntity createGap(GapRequest r) { requireMappers(); if (r.topic() == null || r.topic().isBlank()) throw new IllegalArgumentException("主题不能为空"); var now = LocalDateTime.now(); var e = new KnowledgeGapEntity(); e.setNormalizedTopic(r.topic().trim()); e.setSampleQuestion(r.sampleQuestion()); e.setOccurrenceCount(r.occurrenceCount() == null ? 1 : r.occurrenceCount()); e.setLowScoreCount(r.lowScoreCount() == null ? 0 : r.lowScoreCount()); e.setRiskLevel(r.riskLevel() == null ? "low" : r.riskLevel()); e.setStatus("pending"); e.setLastSeenAt(now); e.setCreateTime(now); e.setUpdateTime(now); gaps.insert(e); return e; }
    public KnowledgeGapEntity research(Long id) { var e = requireGap(id); e.setStatus("researching"); e.setUpdateTime(LocalDateTime.now()); gaps.updateById(e); return e; }
    public KnowledgeGapEntity ignore(Long id) { var e = requireGap(id); e.setStatus("ignored"); e.setUpdateTime(LocalDateTime.now()); gaps.updateById(e); return e; }
    public List<ResearchSourceEntity> sources(Long gapId) { requireMappers(); return sources.selectList(new LambdaQueryWrapper<ResearchSourceEntity>().eq(ResearchSourceEntity::getGapId, gapId).orderByDesc(ResearchSourceEntity::getAuthorityScore)); }
    public ResearchSourceEntity addSource(Long gapId, SourceRequest r) { requireGap(gapId); if (blank(r.title()) || blank(r.url())) throw new IllegalArgumentException("来源标题和链接不能为空"); var now = LocalDateTime.now(); var e = new ResearchSourceEntity(); e.setGapId(gapId); e.setTitle(r.title()); e.setUrl(r.url()); e.setPublisher(r.publisher()); e.setSummary(r.summary()); e.setAuthorityScore(r.authorityScore()); e.setRetrievedAt(now); e.setCreateTime(now); sources.insert(e); return e; }
    public List<ContentCandidateEntity> candidates(Long gapId) { requireMappers(); return candidates.selectList(new LambdaQueryWrapper<ContentCandidateEntity>().eq(gapId != null, ContentCandidateEntity::getGapId, gapId).orderByDesc(ContentCandidateEntity::getUpdateTime)); }
    public ContentCandidateEntity addCandidate(Long gapId, CandidateRequest r) { var gap = requireGap(gapId); if (blank(r.contentType()) || blank(r.title()) || blank(r.content())) throw new IllegalArgumentException("草稿类型、标题和正文不能为空"); var now = LocalDateTime.now(); var e = new ContentCandidateEntity(); e.setGapId(gapId); e.setContentType(r.contentType()); e.setTitle(r.title()); e.setContent(r.content()); e.setTags(r.tags()); e.setRiskLevel(gap.getRiskLevel()); e.setAiReviewJson(r.aiReviewJson()); e.setStatus("ai_reviewed"); e.setCreateTime(now); e.setUpdateTime(now); candidates.insert(e); gap.setStatus("draft_ready"); gap.setUpdateTime(now); gaps.updateById(gap); return e; }
    @Transactional
    public SampleDraftResult createFromSample(Long sampleId, SampleDraftRequest request) {
        requireMappers(); if (sampleCandidates == null) throw new IllegalStateException("样本候选池需要启用 MySQL 数据源");
        var sample = sampleCandidates.selectById(sampleId);
        if (sample == null) throw new IllegalArgumentException("样本不存在");
        if (!"approved".equals(sample.getReviewStatus()) || !Boolean.TRUE.equals(sample.getRecommendedForKnowledge())) throw new IllegalArgumentException("样本须审核通过并标记推荐入库后才能创建知识草稿");
        var existing = candidates.selectOne(new LambdaQueryWrapper<ContentCandidateEntity>().eq(ContentCandidateEntity::getSourceSampleId, sampleId));
        if (existing != null) return new SampleDraftResult(existing.getGapId(), existing.getId(), false);
        if (blank(request.contentType()) || blank(request.title())) throw new IllegalArgumentException("草稿类型和标题不能为空");
        var now = LocalDateTime.now(); var topic = request.title().trim();
        var gap = gaps.selectOne(new LambdaQueryWrapper<KnowledgeGapEntity>().eq(KnowledgeGapEntity::getNormalizedTopic, topic));
        if (gap == null) { gap = new KnowledgeGapEntity(); gap.setNormalizedTopic(topic); gap.setSampleQuestion(sample.getQuestion()); gap.setOccurrenceCount(1); gap.setLowScoreCount(0); gap.setRiskLevel(blank(request.riskLevel()) ? "low" : request.riskLevel()); gap.setStatus("draft_ready"); gap.setLastSeenAt(now); gap.setCreateTime(now); gap.setUpdateTime(now); gaps.insert(gap); }
        var candidate = new ContentCandidateEntity(); candidate.setGapId(gap.getId()); candidate.setSourceSampleId(sampleId); candidate.setContentType(request.contentType()); candidate.setTitle(request.title()); candidate.setContent(sample.getAnswer()); candidate.setTags(request.tags()); candidate.setRiskLevel(blank(request.riskLevel()) ? "low" : request.riskLevel()); candidate.setAiReviewJson(json(Map.of("sampleId", sampleId, "conversationId", sample.getConversationId() == null ? "" : sample.getConversationId(), "source", valueOrEmpty(sample.getSource()), "reviewer", valueOrEmpty(sample.getReviewer()), "reviewRemark", valueOrEmpty(sample.getReviewRemark())))); candidate.setStatus("ai_reviewed"); candidate.setCreateTime(now); candidate.setUpdateTime(now); candidates.insert(candidate);
        return new SampleDraftResult(gap.getId(), candidate.getId(), true);
    }
    public ContentCandidateEntity createMfEpDraft(Long id) { var e = requireCandidate(id); try { var result = mfEp.createDraft(toMfEpRequest(e)); e.setMfEpDraftId(result.id()); e.setStatus("pending_publish"); e.setLastError(null); e.setUpdateTime(LocalDateTime.now()); candidates.updateById(e); audit(e, "draft_created", "datacenter", "mfEpDraftId=" + result.id()); return e; } catch (RuntimeException ex) { return fail(e, ex); } }
    public ContentCandidateEntity publish(Long id, String operator) { var e = requireCandidate(id); if (highRisk(e.getRiskLevel())) throw new IllegalArgumentException("高风险草稿需人工复核，禁止直接发布"); if (e.getMfEpDraftId() == null) throw new IllegalArgumentException("请先创建 MF_EP 草稿"); var actor = operatorOrDefault(operator); try { var result = mfEp.publish(e.getMfEpDraftId(), actor); e.setMfEpContentId(result.contentId()); e.setStatus("published"); e.setLastError(null); e.setUpdateTime(LocalDateTime.now()); candidates.updateById(e); audit(e, "published", actor, "mfEpDraftId=" + e.getMfEpDraftId()); registerEvent(e, "publish"); observeSyncEvents(); return e; } catch (RuntimeException ex) { return fail(e, ex, actor); } }
    public ContentCandidateEntity offline(Long id, String operator) { var e = requireCandidate(id); if (e.getMfEpDraftId() == null || !"published".equals(e.getStatus())) throw new IllegalArgumentException("仅已发布草稿可以下线"); var actor = operatorOrDefault(operator); try { mfEp.offline(e.getMfEpDraftId(), actor); e.setStatus("offline"); e.setLastError(null); e.setUpdateTime(LocalDateTime.now()); candidates.updateById(e); audit(e, "offline", actor, "mfEpDraftId=" + e.getMfEpDraftId()); registerEvent(e, "offline"); observeSyncEvents(); return e; } catch (RuntimeException ex) { return fail(e, ex, actor); } }
    public List<ContentPublishLogEntity> publishLogs(Long candidateId) { requireMappers(); return publishLogs == null ? List.of() : publishLogs.selectList(new LambdaQueryWrapper<ContentPublishLogEntity>().eq(ContentPublishLogEntity::getCandidateId, candidateId).orderByDesc(ContentPublishLogEntity::getCreateTime)); }
    public List<ContentSyncLogEntity> syncLogs(Long candidateId) { requireMappers(); return syncLogs == null ? List.of() : syncLogs.selectList(new LambdaQueryWrapper<ContentSyncLogEntity>().eq(ContentSyncLogEntity::getCandidateId, candidateId).orderByDesc(ContentSyncLogEntity::getUpdateTime)); }
    public ContentSyncLogEntity retrySync(Long candidateId) { var candidate = requireCandidate(candidateId); if (!"published".equals(candidate.getStatus()) && !"offline".equals(candidate.getStatus())) throw new IllegalArgumentException("仅已发布或已下线内容可以同步"); return sync(candidate, "offline".equals(candidate.getStatus()) ? "offline" : "publish"); }
    @Scheduled(fixedDelayString = "${datacenter.agent.knowledge-sync.observe-interval:15000}")
    public void observeSyncEvents() { if (syncLogs == null) return; var pending = syncLogs.selectList(new LambdaQueryWrapper<ContentSyncLogEntity>().eq(ContentSyncLogEntity::getDeliveryStatus, "pending")); if (pending.isEmpty()) return; try { var events = mfEp.pendingSyncEvents(); for (var log : pending) { var matched = events.stream().filter(event -> event.draftId().equals(log.getMfEpDraftId()) && event.action().equals(log.getAction())).findFirst(); if (matched.isPresent()) { log.setMfEpEventId(matched.get().id()); log.setUpdateTime(LocalDateTime.now()); syncLogs.updateById(log); } else if (log.getMfEpEventId() != null) { var event = mfEp.syncEvent(log.getMfEpEventId()); var now = LocalDateTime.now(); if ("acknowledged".equals(event.deliveryStatus())) { log.setDeliveryStatus("acknowledged"); log.setSuccess(true); log.setCompletedAt(now); log.setUpdateTime(now); log.setError(null); syncLogs.updateById(log); audit(candidateFor(log), "agent_event_acknowledged", event.consumer() == null ? "mf-agent-service" : event.consumer(), "mfEpEventId=" + log.getMfEpEventId()); } else if ("failed".equals(event.deliveryStatus()) || "timed_out".equals(event.deliveryStatus())) { log.setDeliveryStatus(event.deliveryStatus()); log.setSuccess(false); log.setCompletedAt(now); log.setUpdateTime(now); log.setError(event.lastFailureReason() == null ? event.deliveryStatus() : event.lastFailureReason()); syncLogs.updateById(log); audit(candidateFor(log), "agent_event_failed", event.consumer() == null ? "mf-agent-service" : event.consumer(), "mfEpEventId=" + log.getMfEpEventId() + "; " + log.getError()); } } } } catch (RuntimeException ignored) { }
    }
    public ContentCandidateEntity reject(Long id) { var e = requireCandidate(id); e.setStatus("rejected"); e.setUpdateTime(LocalDateTime.now()); candidates.updateById(e); return e; }

    private Map<String, Object> toMfEpRequest(ContentCandidateEntity e) { var sourceReferences = sources(e.getGapId()).stream().map(s -> Map.of("title", s.getTitle(), "url", s.getUrl())).toList(); var body = new LinkedHashMap<String, Object>(); body.put("contentType", e.getContentType()); body.put("title", e.getTitle()); body.put("content", e.getContent()); body.put("tags", e.getTags()); body.put("riskLevel", e.getRiskLevel()); body.put("sourceReferences", json(sourceReferences)); body.put("aiReviewJson", e.getAiReviewJson()); body.put("createdBy", "datacenter"); return body; }
    private String json(Object value) { try { return objectMapper.writeValueAsString(value); } catch (JsonProcessingException ex) { throw new IllegalStateException("候选来源序列化失败", ex); } }
    private ContentCandidateEntity fail(ContentCandidateEntity e, RuntimeException ex) { return fail(e, ex, "datacenter"); }
    private ContentCandidateEntity fail(ContentCandidateEntity e, RuntimeException ex, String operator) { e.setLastError(ex.getMessage()); e.setStatus("sync_failed"); e.setUpdateTime(LocalDateTime.now()); candidates.updateById(e); audit(e, "failed", operator, ex.getMessage()); return e; }
    private void audit(ContentCandidateEntity candidate, String action, String operator, String remark) { if (publishLogs == null) return; var log = new ContentPublishLogEntity(); log.setCandidateId(candidate.getId()); log.setMfEpContentId(candidate.getMfEpContentId()); log.setAction(action); log.setOperator(operator); log.setRemark(remark); log.setCreateTime(LocalDateTime.now()); publishLogs.insert(log); }
    private ContentSyncLogEntity sync(ContentCandidateEntity candidate, String action) {
        if (syncLogs == null) throw new IllegalStateException("知识同步审计需要启用 MySQL 数据源");
        String requestId = "candidate-" + candidate.getId() + "-" + action + "-manual-sync-draft-" + candidate.getMfEpDraftId();
        var log = syncLogs.selectOne(new LambdaQueryWrapper<ContentSyncLogEntity>().eq(ContentSyncLogEntity::getRequestId, requestId));
        if (log != null && Boolean.TRUE.equals(log.getSuccess())) return log;
        var now = LocalDateTime.now();
        if (log == null) { log = new ContentSyncLogEntity(); log.setCandidateId(candidate.getId()); log.setMfEpDraftId(candidate.getMfEpDraftId()); log.setMfEpContentId(candidate.getMfEpContentId()); log.setAction(action); log.setRequestId(requestId); log.setCreateTime(now); }
        try { var result = agentSync.sync(requestId, candidate.getMfEpContentId()); log.setSuccess(result.success()); log.setReused(result.reused()); log.setIndexedDocuments(result.indexedDocuments()); log.setError(result.error()); log.setDeliveryStatus(result.success() ? "manual_success" : "manual_failed"); log.setCompletedAt(now); log.setUpdateTime(now); saveSyncLog(log); audit(candidate, result.success() ? "agent_sync_success" : "agent_sync_failed", "mf-agent-service", result.error()); }
        catch (RuntimeException ex) { log.setSuccess(false); log.setReused(false); log.setIndexedDocuments(0); log.setError(ex.getMessage()); log.setDeliveryStatus("manual_failed"); log.setCompletedAt(now); log.setUpdateTime(now); saveSyncLog(log); audit(candidate, "agent_sync_failed", "mf-agent-service", ex.getMessage()); }
        return log;
    }
    private void saveSyncLog(ContentSyncLogEntity log) { if (log.getId() == null) syncLogs.insert(log); else syncLogs.updateById(log); }
    private void registerEvent(ContentCandidateEntity candidate, String action) { if (syncLogs == null) return; var now = LocalDateTime.now(); var log = new ContentSyncLogEntity(); log.setCandidateId(candidate.getId()); log.setMfEpDraftId(candidate.getMfEpDraftId()); log.setMfEpContentId(candidate.getMfEpContentId()); log.setAction(action); log.setRequestId("candidate-" + candidate.getId() + "-" + action + "-event-draft-" + candidate.getMfEpDraftId()); log.setSuccess(false); log.setReused(false); log.setIndexedDocuments(0); log.setDeliveryStatus("pending"); log.setCreateTime(now); log.setUpdateTime(now); syncLogs.insert(log); audit(candidate, "agent_event_pending", "mf-agent-service", "等待 MF_EP 事件确认"); }
    private ContentCandidateEntity candidateFor(ContentSyncLogEntity log) { var candidate = candidates.selectById(log.getCandidateId()); if (candidate == null) throw new IllegalStateException("同步候选不存在"); return candidate; }
    private KnowledgeGapEntity requireGap(Long id) { requireMappers(); var e = gaps.selectById(id); if (e == null) throw new IllegalArgumentException("知识缺口不存在"); return e; }
    private ContentCandidateEntity requireCandidate(Long id) { requireMappers(); var e = candidates.selectById(id); if (e == null) throw new IllegalArgumentException("草稿不存在"); return e; }
    private void requireMappers() { if (gaps == null || sources == null || candidates == null) throw new IllegalStateException("知识运营工作台需要启用 MySQL 数据源"); }
    private String topicOf(String remark, String question) { if (remark != null && remark.contains("topic=")) { var rest = remark.substring(remark.indexOf("topic=") + 6); return rest.split(";", 2)[0].trim(); } return question == null ? "未命名知识缺口" : question.trim(); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private boolean highRisk(String risk) { return "high".equalsIgnoreCase(risk) || "urgent".equalsIgnoreCase(risk) || "高".equals(risk) || "高风险".equals(risk); }
    private String operatorOrDefault(String operator) { return blank(operator) ? "datacenter-admin" : operator; }
    private String valueOrEmpty(String value) { return value == null ? "" : value; }
    public record GapRequest(String topic, String sampleQuestion, Integer occurrenceCount, Integer lowScoreCount, String riskLevel) { }
    public record SourceRequest(String title, String url, String publisher, String summary, Integer authorityScore) { }
    public record CandidateRequest(String contentType, String title, String content, String tags, String aiReviewJson) { }
    public record SampleDraftRequest(String contentType, String title, String tags, String riskLevel) { }
    public record SampleDraftResult(Long gapId, Long candidateId, boolean created) { }
}
