package com.mf.datacenter.knowledge;

import com.mf.datacenter.ai.AiDataStore;
import com.mf.datacenter.ai.entity.SampleCandidateEntity;
import com.mf.datacenter.ai.mapper.SampleCandidateMapper;
import com.mf.datacenter.knowledge.entity.ContentCandidateEntity;
import com.mf.datacenter.knowledge.entity.ContentSyncLogEntity;
import com.mf.datacenter.knowledge.entity.KnowledgeGapEntity;
import com.mf.datacenter.knowledge.mapper.ContentCandidateMapper;
import com.mf.datacenter.knowledge.mapper.ContentPublishLogMapper;
import com.mf.datacenter.knowledge.mapper.ContentSyncLogMapper;
import com.mf.datacenter.knowledge.mapper.KnowledgeGapMapper;
import com.mf.datacenter.knowledge.mapper.ResearchSourceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KnowledgeWorkbenchServiceTest {
    @Test
    void createsCandidateFromApprovedRecommendedSample() {
        var gaps = mock(KnowledgeGapMapper.class); var sources = mock(ResearchSourceMapper.class); var candidates = mock(ContentCandidateMapper.class); var samples = mock(SampleCandidateMapper.class);
        var sample = sample("approved", true); when(samples.selectById(7L)).thenReturn(sample); when(candidates.selectOne(any())).thenReturn(null); when(gaps.selectOne(any())).thenReturn(null);
        doAnswer(call -> { call.getArgument(0, KnowledgeGapEntity.class).setId(11L); return 1; }).when(gaps).insert(any(KnowledgeGapEntity.class));
        doAnswer(call -> { call.getArgument(0, ContentCandidateEntity.class).setId(22L); return 1; }).when(candidates).insert(any(ContentCandidateEntity.class));

        var result = service(gaps, sources, candidates, samples).createFromSample(7L, new KnowledgeWorkbenchService.SampleDraftRequest("encyclopedia", "苹果树冬剪", "苹果,冬剪", "low"));

        assertEquals(11L, result.gapId()); assertEquals(22L, result.candidateId()); assertTrue(result.created());
        verify(candidates).insert(argThat((ContentCandidateEntity candidate) -> candidate.getSourceSampleId().equals(7L) && candidate.getContent().equals(sample.getAnswer()) && candidate.getAiReviewJson().contains("conversationId")));
    }

    @Test
    void rejectsSampleThatIsNotApprovedAndRecommended() {
        var samples = mock(SampleCandidateMapper.class); when(samples.selectById(7L)).thenReturn(sample("pending", false));
        var service = service(mock(KnowledgeGapMapper.class), mock(ResearchSourceMapper.class), mock(ContentCandidateMapper.class), samples);
        var error = assertThrows(IllegalArgumentException.class, () -> service.createFromSample(7L, new KnowledgeWorkbenchService.SampleDraftRequest("faq", "标题", "", "low")));
        assertTrue(error.getMessage().contains("审核通过"));
    }

    @Test
    void returnsExistingCandidateForSameSample() {
        var candidates = mock(ContentCandidateMapper.class); var samples = mock(SampleCandidateMapper.class); when(samples.selectById(7L)).thenReturn(sample("approved", true));
        var existing = new ContentCandidateEntity(); existing.setId(22L); existing.setGapId(11L); when(candidates.selectOne(any())).thenReturn(existing);
        var result = service(mock(KnowledgeGapMapper.class), mock(ResearchSourceMapper.class), candidates, samples).createFromSample(7L, new KnowledgeWorkbenchService.SampleDraftRequest("faq", "标题", "", "low"));
        assertEquals(11L, result.gapId()); assertEquals(22L, result.candidateId()); assertFalse(result.created()); verify(candidates, never()).insert(any(ContentCandidateEntity.class));
    }

    @Test
    void publishesThenRegistersAgentSyncEvent() {
        var gaps = mock(KnowledgeGapMapper.class); var sources = mock(ResearchSourceMapper.class); var candidates = mock(ContentCandidateMapper.class); var samples = mock(SampleCandidateMapper.class);
        var publishLogs = mock(ContentPublishLogMapper.class); var syncLogs = mock(ContentSyncLogMapper.class); var mfEp = mock(MfEpAiContentClient.class); var agentSync = mock(MfAgentKnowledgeSyncClient.class);
        var candidate = new ContentCandidateEntity(); candidate.setId(8L); candidate.setMfEpDraftId(10L); candidate.setRiskLevel("low"); candidate.setStatus("pending_publish");
        when(candidates.selectById(8L)).thenReturn(candidate); when(mfEp.publish(10L, "ops")).thenReturn(new MfEpAiContentClient.MfEpDraft(10L, 24L, "published"));
        var service = new KnowledgeWorkbenchService(provider(gaps), provider(sources), provider(candidates), provider(samples), provider(publishLogs), provider(syncLogs), mock(AiDataStore.class), mfEp, agentSync);

        var result = service.publish(8L, "ops");

        assertEquals("published", result.getStatus()); assertEquals(24L, result.getMfEpContentId());
        verify(agentSync, never()).sync(any(), any());
        verify(syncLogs).insert(argThat((ContentSyncLogEntity log) -> log.getAction().equals("publish") && "pending".equals(log.getDeliveryStatus()) && log.getMfEpContentId().equals(24L)));
    }

    @Test
    void recordsFailedMfEpEventInsteadOfTreatingItAsAcknowledged() {
        var gaps = mock(KnowledgeGapMapper.class); var sources = mock(ResearchSourceMapper.class); var candidates = mock(ContentCandidateMapper.class); var samples = mock(SampleCandidateMapper.class);
        var publishLogs = mock(ContentPublishLogMapper.class); var syncLogs = mock(ContentSyncLogMapper.class); var mfEp = mock(MfEpAiContentClient.class);
        var candidate = new ContentCandidateEntity(); candidate.setId(8L); candidate.setMfEpContentId(24L); when(candidates.selectById(8L)).thenReturn(candidate);
        var log = new ContentSyncLogEntity(); log.setCandidateId(8L); log.setMfEpDraftId(10L); log.setMfEpEventId(20L); log.setAction("publish"); log.setDeliveryStatus("pending");
        when(syncLogs.selectList(any())).thenReturn(java.util.List.of(log)); when(mfEp.pendingSyncEvents()).thenReturn(java.util.List.of()); when(mfEp.syncEvent(20L)).thenReturn(new MfEpAiContentClient.SyncEventStatus(20L, "failed", "mf-agent-service", null, "content was already offline"));
        var service = new KnowledgeWorkbenchService(provider(gaps), provider(sources), provider(candidates), provider(samples), provider(publishLogs), provider(syncLogs), mock(AiDataStore.class), mfEp, mock(MfAgentKnowledgeSyncClient.class));

        service.observeSyncEvents();

        assertEquals("failed", log.getDeliveryStatus()); assertFalse(log.getSuccess()); assertEquals("content was already offline", log.getError()); verify(syncLogs).updateById(log);
    }

    private KnowledgeWorkbenchService service(KnowledgeGapMapper gaps, ResearchSourceMapper sources, ContentCandidateMapper candidates, SampleCandidateMapper samples) {
        return new KnowledgeWorkbenchService(provider(gaps), provider(sources), provider(candidates), provider(samples), provider(mock(ContentPublishLogMapper.class)), provider(mock(ContentSyncLogMapper.class)), mock(AiDataStore.class), mock(MfEpAiContentClient.class), mock(MfAgentKnowledgeSyncClient.class));
    }
    private SampleCandidateEntity sample(String reviewStatus, boolean recommended) { var sample = new SampleCandidateEntity(); sample.setId(7L); sample.setConversationId(99L); sample.setQuestion("苹果树冬剪怎么做"); sample.setAnswer("这是一条经过审核的优质回答，包含足够完整的修剪建议和适用说明。"); sample.setSource("MF_AgentService"); sample.setReviewStatus(reviewStatus); sample.setRecommendedForKnowledge(recommended); sample.setReviewer("ops"); sample.setReviewRemark("可沉淀"); return sample; }
    private <T> ObjectProvider<T> provider(T value) { @SuppressWarnings("unchecked") ObjectProvider<T> provider = mock(ObjectProvider.class); when(provider.getIfAvailable()).thenReturn(value); return provider; }
}
