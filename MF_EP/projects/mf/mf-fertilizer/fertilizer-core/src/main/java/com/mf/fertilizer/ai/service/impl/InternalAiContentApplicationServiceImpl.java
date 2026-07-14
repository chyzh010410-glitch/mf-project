package com.mf.fertilizer.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mf.fertilizer.ai.dto.AiContentDraftDTO;
import com.mf.fertilizer.ai.dto.AiContentReviewDTO;
import com.mf.fertilizer.ai.dto.AiContentRollbackDTO;
import com.mf.fertilizer.ai.entity.AiContentDraft;
import com.mf.fertilizer.ai.entity.AiContentDraftVersion;
import com.mf.fertilizer.ai.entity.AiContentSyncEvent;
import com.mf.fertilizer.ai.service.AiContentDraftService;
import com.mf.fertilizer.ai.service.AiContentDraftVersionService;
import com.mf.fertilizer.ai.service.AiContentSyncEventService;
import com.mf.fertilizer.ai.service.InternalAiContentApplicationService;
import com.mf.fertilizer.content.entity.EncyclopediaArticle;
import com.mf.fertilizer.content.entity.EncyclopediaEntry;
import com.mf.fertilizer.content.service.EncyclopediaArticleService;
import com.mf.fertilizer.content.service.EncyclopediaEntryService;
import com.mf.fertilizer.exception.BusinessException;
import com.mf.fertilizer.platform.entity.Faq;
import com.mf.fertilizer.platform.service.FaqService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InternalAiContentApplicationServiceImpl implements InternalAiContentApplicationService {

    private static final String TYPE_FAQ = "faq";
    private static final String TYPE_ARTICLE = "article";
    private static final String TYPE_ENCYCLOPEDIA = "encyclopedia";
    private static final Set<String> SUPPORTED_TYPES = Set.of(TYPE_FAQ, TYPE_ARTICLE, TYPE_ENCYCLOPEDIA);
    private static final Set<String> HIGH_RISKS = Set.of("high", "urgent", "高", "高风险");
    private static final Set<String> FAILURE_STATES = Set.of("pending", "failed", "timed_out");

    private final AiContentDraftService draftService;
    private final AiContentDraftVersionService versionService;
    private final AiContentSyncEventService syncEventService;
    private final FaqService faqService;
    private final EncyclopediaArticleService articleService;
    private final EncyclopediaEntryService entryService;

    @Override
    @Transactional
    public AiContentDraft createDraft(AiContentDraftDTO dto) {
        AiContentDraft draft = new AiContentDraft();
        applyDto(dto, draft);
        draft.setStatus(isHighRisk(draft) ? "pending_review" : normalizeStatus(dto.getStatus()));
        draft.setReviewStatus(isHighRisk(draft) ? "pending" : "not_required");
        draft.setVersion(1);
        draftService.save(draft);
        saveVersion(draft, "created", draft.getCreatedBy(), draft.getRemark());
        return draft;
    }

    @Override
    @Transactional
    public AiContentDraft updateDraft(Long id, AiContentDraftDTO dto) {
        AiContentDraft draft = requireDraft(id);
        if ("published".equals(draft.getStatus())) {
            throw new BusinessException(400, "已发布草稿不能直接修改，请先下线或回滚");
        }
        applyDto(dto, draft);
        if (isHighRisk(draft)) {
            draft.setStatus("pending_review");
            draft.setReviewStatus("pending");
            draft.setReviewedBy(null);
            draft.setReviewedAt(null);
            draft.setReviewRemark(null);
        }
        nextVersion(draft);
        draftService.updateById(draft);
        saveVersion(draft, "updated", draft.getCreatedBy(), draft.getRemark());
        return draft;
    }

    @Override
    @Transactional
    public AiContentDraft reviewDraft(Long id, AiContentReviewDTO dto) {
        AiContentDraft draft = requireDraft(id);
        String decision = dto.getDecision().trim().toLowerCase();
        if (!Set.of("approved", "rejected").contains(decision)) {
            throw new BusinessException(400, "审核结论仅支持approved或rejected");
        }
        draft.setReviewStatus(decision);
        draft.setReviewedBy(dto.getReviewer());
        draft.setReviewedAt(LocalDateTime.now());
        draft.setReviewRemark(dto.getRemark());
        draft.setStatus("approved".equals(decision) ? "approved" : "rejected");
        nextVersion(draft);
        draftService.updateById(draft);
        saveVersion(draft, "reviewed", dto.getReviewer(), dto.getRemark());
        return draft;
    }

    @Override
    @Transactional
    public AiContentDraft publishDraft(Long id, String operator) {
        AiContentDraft draft = requireDraft(id);
        if ("published".equals(draft.getStatus())) {
            return draft;
        }
        if (isHighRisk(draft) && !"approved".equals(draft.getReviewStatus())) {
            throw new BusinessException(400, "高风险内容必须经人工审核通过后才能发布");
        }
        Long contentId = switch (draft.getContentType()) {
            case TYPE_FAQ -> publishFaq(draft);
            case TYPE_ARTICLE -> publishArticle(draft);
            case TYPE_ENCYCLOPEDIA -> publishEntry(draft);
            default -> throw new BusinessException(400, "不支持的内容类型");
        };
        draft.setMfEpContentId(contentId);
        draft.setStatus("published");
        draft.setPublishedBy(operator);
        draft.setPublishedAt(LocalDateTime.now());
        nextVersion(draft);
        draftService.updateById(draft);
        saveVersion(draft, "published", operator, null);
        saveSyncEvent(draft, "publish", operator);
        return draft;
    }

    @Override
    @Transactional
    public AiContentDraft offlineContent(Long id, String operator) {
        AiContentDraft draft = requireDraft(id);
        updateFormalContent(draft, false);
        draft.setStatus("offline");
        draft.setRemark(operator == null ? draft.getRemark() : "offline by " + operator);
        nextVersion(draft);
        draftService.updateById(draft);
        saveVersion(draft, "offline", operator, draft.getRemark());
        saveSyncEvent(draft, "offline", operator);
        return draft;
    }

    @Override
    @Transactional
    public AiContentDraft rollbackDraft(Long id, AiContentRollbackDTO dto) {
        AiContentDraft draft = requireDraft(id);
        AiContentDraftVersion target = versionService.getOne(new LambdaQueryWrapper<AiContentDraftVersion>()
                .eq(AiContentDraftVersion::getDraftId, id)
                .eq(AiContentDraftVersion::getVersion, dto.getTargetVersion()));
        if (target == null) {
            throw new BusinessException(404, "目标版本不存在");
        }
        Long currentContentId = draft.getMfEpContentId();
        restoreVersion(target, draft);
        if (draft.getMfEpContentId() == null) {
            draft.setMfEpContentId(currentContentId);
        }
        nextVersion(draft);
        draftService.updateById(draft);
        updateFormalContent(draft, "published".equals(draft.getStatus()));
        saveVersion(draft, "rolled_back", dto.getOperator(), dto.getRemark());
        saveSyncEvent(draft, "rollback", dto.getOperator());
        return draft;
    }

    @Override
    public AiContentDraft getContent(Long id) {
        return requireDraft(id);
    }

    @Override
    public List<AiContentDraftVersion> listVersions(Long id) {
        requireDraft(id);
        return versionService.list(new LambdaQueryWrapper<AiContentDraftVersion>()
                .eq(AiContentDraftVersion::getDraftId, id)
                .orderByDesc(AiContentDraftVersion::getVersion));
    }

    @Override
    public List<AiContentSyncEvent> listPendingSyncEvents(Integer limit) {
        int safeLimit = limit == null ? 100 : Math.min(Math.max(limit, 1), 500);
        return syncEventService.list(new LambdaQueryWrapper<AiContentSyncEvent>()
                .eq(AiContentSyncEvent::getDeliveryStatus, "pending")
                .orderByAsc(AiContentSyncEvent::getId)
                .last("LIMIT " + safeLimit));
    }

    @Override
    public AiContentSyncEvent getSyncEvent(Long id) {
        AiContentSyncEvent event = syncEventService.getById(id);
        if (event == null) {
            throw new BusinessException(404, "同步事件不存在");
        }
        return event;
    }

    @Override
    @Transactional
    public void acknowledgeSyncEvent(Long id, String consumer) {
        AiContentSyncEvent event = getSyncEvent(id);
        if ("acknowledged".equals(event.getDeliveryStatus())) {
            return;
        }
        event.setDeliveryStatus("acknowledged");
        event.setConsumer(consumer == null || consumer.isBlank() ? "agent-service" : consumer);
        event.setAcknowledgedAt(LocalDateTime.now());
        syncEventService.updateById(event);
    }

    @Override
    @Transactional
    public void recordSyncEventFailure(Long id, String consumer, String state, Integer failureAttempts, String reason) {
        AiContentSyncEvent event = getSyncEvent(id);
        String normalizedState = state == null ? "pending" : state.trim().toLowerCase();
        if (!FAILURE_STATES.contains(normalizedState)) {
            throw new BusinessException(400, "同步失败状态仅支持pending/failed/timed_out");
        }
        if ("acknowledged".equals(event.getDeliveryStatus())) {
            throw new BusinessException(400, "已确认事件不能回写失败状态");
        }
        if (blank(reason)) {
            throw new BusinessException(400, "失败原因不能为空");
        }
        event.setDeliveryStatus(normalizedState);
        event.setConsumer(blank(consumer) ? "agent-service" : consumer);
        event.setFailureAttempts(Math.max(failureAttempts == null ? 1 : failureAttempts, 1));
        event.setLastFailureReason(reason.trim());
        event.setLastFailedAt(LocalDateTime.now());
        syncEventService.updateById(event);
    }

    @Override
    @Transactional
    public void retrySyncEvent(Long id, String operator) {
        AiContentSyncEvent event = getSyncEvent(id);
        if (!Set.of("failed", "timed_out").contains(event.getDeliveryStatus())) {
            throw new BusinessException(400, "仅失败或超时事件可以人工重试");
        }
        event.setDeliveryStatus("pending");
        event.setRetryCount((event.getRetryCount() == null ? 0 : event.getRetryCount()) + 1);
        event.setLastRetryAt(LocalDateTime.now());
        event.setRemark("retry by " + (blank(operator) ? "operator" : operator));
        syncEventService.updateById(event);
    }

    private void applyDto(AiContentDraftDTO dto, AiContentDraft draft) {
        String type = dto.getContentType().trim().toLowerCase();
        if (!SUPPORTED_TYPES.contains(type)) {
            throw new BusinessException(400, "contentType仅支持faq/article/encyclopedia");
        }
        BeanUtils.copyProperties(dto, draft);
        draft.setContentType(type);
        if (draft.getRiskLevel() == null || draft.getRiskLevel().isBlank()) {
            draft.setRiskLevel("low");
        }
    }

    private Long publishFaq(AiContentDraft draft) {
        Faq faq = draft.getMfEpContentId() == null ? new Faq() : faqService.getById(draft.getMfEpContentId());
        if (faq == null) faq = new Faq();
        faq.setQuestion(draft.getTitle());
        faq.setAnswer(draft.getContent());
        faq.setCategory(blank(draft.getTags()) ? "AI知识补全" : draft.getTags());
        faq.setSortOrder(0);
        faq.setIsPublished(1);
        faq.setViewCount(faq.getViewCount() == null ? 0 : faq.getViewCount());
        saveOrUpdate(faq, faqService);
        return faq.getId();
    }

    private Long publishArticle(AiContentDraft draft) {
        EncyclopediaArticle article = draft.getMfEpContentId() == null ? new EncyclopediaArticle() : articleService.getById(draft.getMfEpContentId());
        if (article == null) article = new EncyclopediaArticle();
        article.setTitle(draft.getTitle());
        article.setSummary(draft.getSummary());
        article.setContent(draft.getContent());
        article.setTags(draft.getTags());
        article.setIsPublished(1);
        article.setIsTop(0);
        article.setIsRecommend(0);
        article.setViewCount(article.getViewCount() == null ? 0 : article.getViewCount());
        saveOrUpdate(article, articleService);
        return article.getId();
    }

    private Long publishEntry(AiContentDraft draft) {
        EncyclopediaEntry entry = draft.getMfEpContentId() == null ? new EncyclopediaEntry() : entryService.getById(draft.getMfEpContentId());
        if (entry == null) entry = new EncyclopediaEntry();
        entry.setName(draft.getTitle());
        entry.setDescription(draft.getSummary());
        entry.setCareGuide(draft.getContent());
        entry.setTags(draft.getTags());
        entry.setIsPublished(1);
        entry.setViewCount(entry.getViewCount() == null ? 0 : entry.getViewCount());
        saveOrUpdate(entry, entryService);
        return entry.getId();
    }

    private void updateFormalContent(AiContentDraft draft, boolean published) {
        if (draft.getMfEpContentId() == null) return;
        switch (draft.getContentType()) {
            case TYPE_FAQ -> {
                Faq faq = faqService.getById(draft.getMfEpContentId());
                if (faq != null) {
                    faq.setQuestion(draft.getTitle()); faq.setAnswer(draft.getContent()); faq.setIsPublished(published ? 1 : 0); faqService.updateById(faq);
                }
            }
            case TYPE_ARTICLE -> {
                EncyclopediaArticle article = articleService.getById(draft.getMfEpContentId());
                if (article != null) {
                    article.setTitle(draft.getTitle()); article.setSummary(draft.getSummary()); article.setContent(draft.getContent()); article.setTags(draft.getTags()); article.setIsPublished(published ? 1 : 0); articleService.updateById(article);
                }
            }
            case TYPE_ENCYCLOPEDIA -> {
                EncyclopediaEntry entry = entryService.getById(draft.getMfEpContentId());
                if (entry != null) {
                    entry.setName(draft.getTitle()); entry.setDescription(draft.getSummary()); entry.setCareGuide(draft.getContent()); entry.setTags(draft.getTags()); entry.setIsPublished(published ? 1 : 0); entryService.updateById(entry);
                }
            }
            default -> throw new BusinessException(400, "不支持的内容类型");
        }
    }

    private <T> void saveOrUpdate(T entity, com.baomidou.mybatisplus.extension.service.IService<T> service) {
        if (entity instanceof com.mf.fertilizer.entity.BaseEntity base && base.getId() != null) service.updateById(entity); else service.save(entity);
    }

    private void saveVersion(AiContentDraft draft, String action, String operator, String remark) {
        AiContentDraftVersion snapshot = new AiContentDraftVersion();
        BeanUtils.copyProperties(draft, snapshot);
        snapshot.setId(null);
        snapshot.setDraftId(draft.getId());
        snapshot.setAction(action);
        snapshot.setOperator(operator);
        snapshot.setRemark(remark);
        versionService.save(snapshot);
    }

    private void saveSyncEvent(AiContentDraft draft, String action, String operator) {
        AiContentSyncEvent event = new AiContentSyncEvent();
        event.setDraftId(draft.getId());
        event.setMfEpContentId(draft.getMfEpContentId());
        event.setContentType(draft.getContentType());
        event.setAction(action);
        event.setVersion(draft.getVersion());
        event.setDeliveryStatus("pending");
        event.setFailureAttempts(0);
        event.setRetryCount(0);
        event.setRemark(operator);
        syncEventService.save(event);
    }

    private void restoreVersion(AiContentDraftVersion source, AiContentDraft target) {
        target.setContentType(source.getContentType()); target.setTitle(source.getTitle()); target.setSummary(source.getSummary()); target.setContent(source.getContent());
        target.setTags(source.getTags()); target.setCrop(source.getCrop()); target.setTreeAge(source.getTreeAge()); target.setSeason(source.getSeason());
        target.setRegion(source.getRegion()); target.setRiskLevel(source.getRiskLevel()); target.setSourceReferences(source.getSourceReferences());
        target.setAiReviewJson(source.getAiReviewJson()); target.setStatus(source.getStatus()); target.setMfEpContentId(source.getMfEpContentId());
        target.setReviewStatus(source.getReviewStatus()); target.setReviewedBy(source.getReviewedBy()); target.setReviewRemark(source.getReviewRemark());
    }

    private void nextVersion(AiContentDraft draft) { draft.setVersion(draft.getVersion() == null ? 1 : draft.getVersion() + 1); }
    private boolean isHighRisk(AiContentDraft draft) { return draft.getRiskLevel() != null && HIGH_RISKS.contains(draft.getRiskLevel().trim().toLowerCase()); }
    private String normalizeStatus(String status) { return blank(status) ? "draft" : status.trim().toLowerCase(); }
    private boolean blank(String value) { return value == null || value.isBlank(); }

    private AiContentDraft requireDraft(Long id) {
        AiContentDraft draft = draftService.getById(id);
        if (draft == null) throw new BusinessException(404, "AI内容草稿不存在");
        return draft;
    }
}
