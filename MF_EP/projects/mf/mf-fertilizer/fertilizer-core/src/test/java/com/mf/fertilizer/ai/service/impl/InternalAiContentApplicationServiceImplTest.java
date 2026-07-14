package com.mf.fertilizer.ai.service.impl;

import com.mf.fertilizer.ai.dto.AiContentReviewDTO;
import com.mf.fertilizer.ai.dto.AiContentRollbackDTO;
import com.mf.fertilizer.ai.entity.AiContentDraft;
import com.mf.fertilizer.ai.entity.AiContentDraftVersion;
import com.mf.fertilizer.ai.service.AiContentDraftService;
import com.mf.fertilizer.ai.service.AiContentDraftVersionService;
import com.mf.fertilizer.ai.service.AiContentSyncEventService;
import com.mf.fertilizer.content.service.EncyclopediaArticleService;
import com.mf.fertilizer.content.service.EncyclopediaEntryService;
import com.mf.fertilizer.exception.BusinessException;
import com.mf.fertilizer.platform.entity.Faq;
import com.mf.fertilizer.platform.service.FaqService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalAiContentApplicationServiceImplTest {
    private final AiContentDraftService drafts = mock(AiContentDraftService.class);
    private final AiContentDraftVersionService versions = mock(AiContentDraftVersionService.class);
    private final AiContentSyncEventService syncEvents = mock(AiContentSyncEventService.class);
    private final FaqService faqs = mock(FaqService.class);
    private final EncyclopediaArticleService articles = mock(EncyclopediaArticleService.class);
    private final EncyclopediaEntryService entries = mock(EncyclopediaEntryService.class);

    @Test
    void highRiskDraftRequiresApprovalBeforePublishing() {
        var draft = draft();
        draft.setRiskLevel("high");
        draft.setStatus("pending_review");
        draft.setReviewStatus("pending");
        when(drafts.getById(1L)).thenReturn(draft);

        var service = service();
        assertThrows(BusinessException.class, () -> service.publishDraft(1L, "ops"));

        var review = new AiContentReviewDTO();
        review.setDecision("approved");
        review.setReviewer("reviewer-a");
        review.setRemark("来源已核验");
        service.reviewDraft(1L, review);
        assertEquals("approved", draft.getReviewStatus());
        assertEquals("approved", draft.getStatus());

        doAnswer(call -> {
            call.getArgument(0, Faq.class).setId(99L);
            return true;
        }).when(faqs).save(any(Faq.class));
        service.publishDraft(1L, "ops");

        assertEquals("published", draft.getStatus());
        assertEquals(99L, draft.getMfEpContentId());
        verify(syncEvents).save(any());
    }

    @Test
    void rollbackRestoresSnapshotAndCreatesSyncEvent() {
        var draft = draft();
        draft.setVersion(3);
        draft.setStatus("published");
        draft.setMfEpContentId(99L);
        draft.setContent("new content");
        when(drafts.getById(1L)).thenReturn(draft);

        var snapshot = new AiContentDraftVersion();
        snapshot.setDraftId(1L);
        snapshot.setVersion(1);
        snapshot.setContentType("faq");
        snapshot.setTitle("old title");
        snapshot.setContent("old content");
        snapshot.setRiskLevel("low");
        snapshot.setStatus("draft");
        snapshot.setReviewStatus("not_required");
        when(versions.getOne(any())).thenReturn(snapshot);
        when(faqs.getById(99L)).thenReturn(new Faq());

        var rollback = new AiContentRollbackDTO();
        rollback.setTargetVersion(1);
        rollback.setOperator("ops");
        rollback.setRemark("内容撤回");
        service().rollbackDraft(1L, rollback);

        assertEquals("old title", draft.getTitle());
        assertEquals("old content", draft.getContent());
        assertEquals("draft", draft.getStatus());
        assertEquals(4, draft.getVersion());
        verify(faqs).updateById(any(Faq.class));
        verify(syncEvents).save(any());
    }

    @Test
    void highRiskUpdateRequiresAnotherReview() {
        var draft = draft();
        draft.setRiskLevel("high");
        draft.setStatus("approved");
        draft.setReviewStatus("approved");
        draft.setReviewedBy("reviewer-a");
        when(drafts.getById(1L)).thenReturn(draft);

        var update = new com.mf.fertilizer.ai.dto.AiContentDraftDTO();
        update.setContentType("faq");
        update.setTitle("苹果树春季施肥注意事项");
        update.setContent("更新后的正文");
        update.setRiskLevel("high");

        var service = service();
        service.updateDraft(1L, update);

        assertEquals("pending_review", draft.getStatus());
        assertEquals("pending", draft.getReviewStatus());
        assertEquals(null, draft.getReviewedBy());
        assertThrows(BusinessException.class, () -> service.publishDraft(1L, "ops"));
    }

    @Test
    void acknowledgedEventCanBeReadAndAcknowledgedAgainSafely() {
        var event = new com.mf.fertilizer.ai.entity.AiContentSyncEvent();
        event.setId(7L);
        event.setDeliveryStatus("acknowledged");
        event.setConsumer("mf-agent-service");
        when(syncEvents.getById(7L)).thenReturn(event);

        var service = service();
        assertEquals(event, service.getSyncEvent(7L));
        service.acknowledgeSyncEvent(7L, "mf-agent-service");

        verify(syncEvents, never()).updateById(any());
    }

    @Test
    void failedEventCanBeRecordedAndManuallyRetried() {
        var event = new com.mf.fertilizer.ai.entity.AiContentSyncEvent();
        event.setId(8L);
        event.setDeliveryStatus("pending");
        when(syncEvents.getById(8L)).thenReturn(event);

        var service = service();
        service.recordSyncEventFailure(8L, "mf-agent-service", "failed", 5, "index refresh failed");

        assertEquals("failed", event.getDeliveryStatus());
        assertEquals(5, event.getFailureAttempts());
        assertEquals("index refresh failed", event.getLastFailureReason());

        service.retrySyncEvent(8L, "ops");

        assertEquals("pending", event.getDeliveryStatus());
        assertEquals(1, event.getRetryCount());
        verify(syncEvents, org.mockito.Mockito.times(2)).updateById(event);
    }

    private InternalAiContentApplicationServiceImpl service() {
        return new InternalAiContentApplicationServiceImpl(drafts, versions, syncEvents, faqs, articles, entries);
    }

    private AiContentDraft draft() {
        var draft = new AiContentDraft();
        draft.setId(1L);
        draft.setVersion(1);
        draft.setContentType("faq");
        draft.setTitle("苹果树冬剪注意事项");
        draft.setContent("正文");
        draft.setRiskLevel("low");
        return draft;
    }
}
