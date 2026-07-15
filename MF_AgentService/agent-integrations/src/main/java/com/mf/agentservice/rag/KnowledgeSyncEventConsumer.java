package com.mf.agentservice.rag;

import com.mf.agentservice.client.MfEpClient;
import com.mf.agentservice.config.MfAgentProperties;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeSyncEventConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(KnowledgeSyncEventConsumer.class);
    private static final int MAX_STATUS_RECORDS = 200;

    private final MfEpClient mfEpClient;
    private final KnowledgeRefreshService refreshService;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final HybridRagService hybridRagService;
    private final RagProperties ragProperties;
    private final MfAgentProperties properties;
    private final ConcurrentHashMap<Long, SyncEventStatus> statuses = new ConcurrentHashMap<>();

    public KnowledgeSyncEventConsumer(
            MfEpClient mfEpClient,
            KnowledgeRefreshService refreshService,
            KnowledgeRetrievalService knowledgeRetrievalService,
            HybridRagService hybridRagService,
            RagProperties ragProperties,
            MfAgentProperties properties
    ) {
        this.mfEpClient = mfEpClient;
        this.refreshService = refreshService;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
        this.hybridRagService = hybridRagService;
        this.ragProperties = ragProperties;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${mf.agent.knowledge-sync-poll-interval:30s}")
    public void pollPendingEvents() {
        try {
            processPendingEvents();
        } catch (RuntimeException exception) {
            LOG.warn("Knowledge sync event polling failed: {}", exception.getMessage());
        }
    }

    public synchronized List<EventSyncResult> processPendingEvents() {
        return process(false);
    }

    public synchronized List<EventSyncResult> retryFailedEvents() {
        return process(true);
    }

    public List<SyncEventStatus> statuses() {
        return statuses.values().stream()
                .sorted(Comparator.comparing(SyncEventStatus::lastAttemptAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private List<EventSyncResult> process(boolean retryFailed) {
        List<EventSyncResult> results = new ArrayList<>();
        for (var event : mfEpClient.pendingKnowledgeSyncEvents(100)) {
            var current = statuses.get(event.id());
            if (retryFailed && (current == null || !("failed".equals(current.state()) || "timed_out".equals(current.state())))) {
                continue;
            }
            if (!retryFailed && current != null && ("failed".equals(current.state()) || "timed_out".equals(current.state()))) {
                results.add(new EventSyncResult(event.id(), false, 0, current.failureAttempts(), current.state(), current.lastError()));
                continue;
            }
            results.add(process(event));
        }
        return results;
    }

    private EventSyncResult process(MfEpClient.KnowledgeSyncEvent event) {
        var current = statuses.computeIfAbsent(event.id(), ignored -> SyncEventStatus.pending(event));
        try {
            int indexedDocuments = refreshService.refreshNow();
            refreshVectorIndex();
            verify(event);
            mfEpClient.acknowledgeKnowledgeSyncEvent(event.id());
            statuses.put(event.id(), current.acknowledged());
            trimStatuses();
            return new EventSyncResult(event.id(), true, indexedDocuments, 0, "acknowledged", null);
        } catch (RuntimeException exception) {
            var failed = current.failed(exception.getMessage(), properties.agent().knowledgeSyncMaxRetries(),
                    properties.agent().knowledgeSyncPendingTimeout());
            statuses.put(event.id(), failed);
            trimStatuses();
            reportFailure(event.id(), failed);
            LOG.warn("Knowledge sync event {} is {} after attempt {}: {}", event.id(), failed.state(), failed.failureAttempts(), exception.getMessage());
            return new EventSyncResult(event.id(), false, 0, failed.failureAttempts(), failed.state(), exception.getMessage());
        }
    }

    private void refreshVectorIndex() {
        if (!ragProperties.enabled()) {
            return;
        }
        try {
            hybridRagService.reindex();
        } catch (RuntimeException exception) {
            LOG.warn("Vector index refresh skipped after keyword refresh: {}", exception.getMessage());
        }
    }

    private void verify(MfEpClient.KnowledgeSyncEvent event) {
        boolean present = knowledgeRetrievalService.documents().stream().anyMatch(document ->
                event.contentType().equals(document.sourceType())
                        && document.sourceId().endsWith("-" + event.mfEpContentId()));
        if ("offline".equals(event.action()) && present) {
            throw new IllegalStateException("Offline content remains in the knowledge index");
        }
        if (!"offline".equals(event.action()) && !present) {
            throw new IllegalStateException("Published or rolled back content is missing from the knowledge index");
        }
    }

    private void trimStatuses() {
        if (statuses.size() >= MAX_STATUS_RECORDS) {
            statuses.entrySet().stream()
                    .min(Comparator.comparing(entry -> entry.getValue().firstObservedAt()))
                    .ifPresent(entry -> statuses.remove(entry.getKey()));
        }
    }

    private void reportFailure(long eventId, SyncEventStatus failed) {
        try {
            mfEpClient.reportKnowledgeSyncFailure(eventId, failed.state(), failed.failureAttempts(), failed.lastError());
        } catch (RuntimeException reportException) {
            LOG.warn("Knowledge sync failure report for event {} was not persisted to MF_EP: {}", eventId, reportException.getMessage());
        }
    }

    public record EventSyncResult(long eventId, boolean acknowledged, int indexedDocuments, int failureAttempts, String state, String error) {
    }

    public record SyncEventStatus(
            long eventId,
            String action,
            String state,
            int failureAttempts,
            String lastError,
            Instant firstObservedAt,
            Instant lastAttemptAt
    ) {
        private static SyncEventStatus pending(MfEpClient.KnowledgeSyncEvent event) {
            var now = Instant.now();
            return new SyncEventStatus(event.id(), event.action(), "pending", 0, null, now, now);
        }

        private SyncEventStatus acknowledged() {
            return new SyncEventStatus(eventId, action, "acknowledged", 0, null, firstObservedAt, Instant.now());
        }

        private SyncEventStatus failed(String error, int maxRetries, java.time.Duration pendingTimeout) {
            var now = Instant.now();
            int attempts = failureAttempts + 1;
            String nextState = now.isAfter(firstObservedAt.plus(pendingTimeout)) ? "timed_out"
                    : attempts >= maxRetries ? "failed" : "pending";
            return new SyncEventStatus(eventId, action, nextState, attempts, error, firstObservedAt, now);
        }
    }
}
