package com.mf.agentservice.rag;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeSyncService {
    private static final int MAX_REQUEST_RECORDS = 200;

    private final KnowledgeRefreshService refreshService;
    private final ConcurrentHashMap<String, SyncResult> completedRequests = new ConcurrentHashMap<>();

    public KnowledgeSyncService(KnowledgeRefreshService refreshService) {
        this.refreshService = refreshService;
    }

    public synchronized SyncResult sync(String requestId, List<Long> contentIds) {
        var existing = completedRequests.get(requestId);
        if (existing != null) {
            return existing.withReused(true);
        }
        var requestedIds = contentIds == null ? List.<Long>of() : contentIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        try {
            int indexedDocuments = refreshService.refreshNow();
            var result = new SyncResult(requestId, requestedIds, "full_refresh", true, false,
                    indexedDocuments, null, Instant.now());
            if (completedRequests.size() >= MAX_REQUEST_RECORDS) {
                completedRequests.clear();
            }
            completedRequests.put(requestId, result);
            return result;
        } catch (RuntimeException exception) {
            return new SyncResult(requestId, requestedIds, "full_refresh", false, false,
                    0, exception.getMessage(), Instant.now());
        }
    }

    public record SyncResult(
            String requestId,
            List<Long> contentIds,
            String refreshMode,
            boolean success,
            boolean reused,
            int indexedDocuments,
            String error,
            Instant completedAt
    ) {
        private SyncResult withReused(boolean value) {
            return new SyncResult(requestId, contentIds, refreshMode, success, value, indexedDocuments, error, completedAt);
        }
    }
}
