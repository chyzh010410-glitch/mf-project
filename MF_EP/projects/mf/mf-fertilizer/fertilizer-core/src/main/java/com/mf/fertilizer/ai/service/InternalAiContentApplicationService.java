package com.mf.fertilizer.ai.service;

import com.mf.fertilizer.ai.dto.AiContentDraftDTO;
import com.mf.fertilizer.ai.dto.AiContentReviewDTO;
import com.mf.fertilizer.ai.dto.AiContentRollbackDTO;
import com.mf.fertilizer.ai.entity.AiContentDraft;
import com.mf.fertilizer.ai.entity.AiContentDraftVersion;
import com.mf.fertilizer.ai.entity.AiContentSyncEvent;

import java.util.List;

public interface InternalAiContentApplicationService {
    AiContentDraft createDraft(AiContentDraftDTO dto);

    AiContentDraft updateDraft(Long id, AiContentDraftDTO dto);

    AiContentDraft publishDraft(Long id, String operator);

    AiContentDraft offlineContent(Long id, String operator);

    AiContentDraft reviewDraft(Long id, AiContentReviewDTO dto);

    AiContentDraft rollbackDraft(Long id, AiContentRollbackDTO dto);

    AiContentDraft getContent(Long id);

    List<AiContentDraftVersion> listVersions(Long id);

    List<AiContentSyncEvent> listPendingSyncEvents(Integer limit);

    AiContentSyncEvent getSyncEvent(Long id);

    void acknowledgeSyncEvent(Long id, String consumer);

    void recordSyncEventFailure(Long id, String consumer, String state, Integer failureAttempts, String reason);

    void retrySyncEvent(Long id, String operator);
}
