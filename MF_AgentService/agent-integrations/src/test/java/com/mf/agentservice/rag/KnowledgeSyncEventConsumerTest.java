package com.mf.agentservice.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mf.agentservice.client.MfEpClient;
import com.mf.agentservice.config.MfAgentProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class KnowledgeSyncEventConsumerTest {

    @Test
    void acknowledgesPublishedContentOnlyAfterItAppearsInTheKeywordIndex() {
        var ep = mock(MfEpClient.class);
        var refresh = mock(KnowledgeRefreshService.class);
        var keyword = mock(KnowledgeRetrievalService.class);
        when(ep.pendingKnowledgeSyncEvents(100)).thenReturn(List.of(new MfEpClient.KnowledgeSyncEvent(8, 3, 128, "faq", "publish", 4)));
        when(refresh.refreshNow()).thenReturn(9);
        when(keyword.documents()).thenReturn(List.of(new RagDocument("faq", "faq-128", "退款规则", "内容", List.of())));
        doNothing().when(ep).acknowledgeKnowledgeSyncEvent(8);

        var consumer = consumer(ep, refresh, keyword);
        var result = consumer.processPendingEvents();

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.acknowledged()).isTrue();
            assertThat(item.indexedDocuments()).isEqualTo(9);
        });
        verify(ep).acknowledgeKnowledgeSyncEvent(8);
    }

    @Test
    void leavesEventPendingWhenOfflineContentStillExistsInTheKeywordIndex() {
        var ep = mock(MfEpClient.class);
        var refresh = mock(KnowledgeRefreshService.class);
        var keyword = mock(KnowledgeRetrievalService.class);
        when(ep.pendingKnowledgeSyncEvents(100)).thenReturn(List.of(new MfEpClient.KnowledgeSyncEvent(9, 3, 128, "faq", "offline", 4)));
        when(refresh.refreshNow()).thenReturn(9);
        when(keyword.documents()).thenReturn(List.of(new RagDocument("faq", "faq-128", "退款规则", "内容", List.of())));

        var consumer = consumer(ep, refresh, keyword);
        var result = consumer.processPendingEvents();

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.acknowledged()).isFalse();
            assertThat(item.failureAttempts()).isEqualTo(1);
            assertThat(item.error()).contains("Offline content");
        });
        org.mockito.Mockito.verify(ep, org.mockito.Mockito.never()).acknowledgeKnowledgeSyncEvent(anyLong());
        verify(ep).reportKnowledgeSyncFailure(9, "pending", 1, "Offline content remains in the knowledge index");
    }

    @Test
    void stopsAutomaticRetriesAtTheConfiguredThresholdButAllowsManualRetry() {
        var ep = mock(MfEpClient.class);
        var refresh = mock(KnowledgeRefreshService.class);
        var keyword = mock(KnowledgeRetrievalService.class);
        var event = new MfEpClient.KnowledgeSyncEvent(10, 3, 128, "faq", "publish", 4);
        when(ep.pendingKnowledgeSyncEvents(100)).thenReturn(List.of(event));
        when(refresh.refreshNow()).thenThrow(
                new IllegalStateException("MF_EP unavailable"),
                new IllegalStateException("MF_EP unavailable"),
                new IllegalStateException("MF_EP unavailable"),
                new IllegalStateException("MF_EP unavailable"),
                new IllegalStateException("MF_EP unavailable"))
                .thenReturn(9);
        var consumer = consumer(ep, refresh, keyword);

        for (int attempt = 0; attempt < 5; attempt++) {
            consumer.processPendingEvents();
        }
        var blocked = consumer.processPendingEvents();
        when(keyword.documents()).thenReturn(List.of(new RagDocument("faq", "faq-128", "退款规则", "内容", List.of())));
        var retried = consumer.retryFailedEvents();

        assertThat(blocked).singleElement().satisfies(item -> {
            assertThat(item.state()).isEqualTo("failed");
            assertThat(item.failureAttempts()).isEqualTo(5);
        });
        assertThat(retried).singleElement().satisfies(item -> assertThat(item.acknowledged()).isTrue());
        verify(ep).acknowledgeKnowledgeSyncEvent(10);
        verify(ep).reportKnowledgeSyncFailure(10, "failed", 5, "MF_EP unavailable");
    }

    @Test
    void manualRetryDoesNotProcessEventsStillInAutomaticRetryState() {
        var ep = mock(MfEpClient.class);
        var refresh = mock(KnowledgeRefreshService.class);
        var keyword = mock(KnowledgeRetrievalService.class);
        var event = new MfEpClient.KnowledgeSyncEvent(11, 3, 128, "faq", "publish", 4);
        when(ep.pendingKnowledgeSyncEvents(100)).thenReturn(List.of(event));
        when(refresh.refreshNow()).thenThrow(new IllegalStateException("MF_EP unavailable"));
        var consumer = consumer(ep, refresh, keyword);

        consumer.processPendingEvents();
        var retried = consumer.retryFailedEvents();

        assertThat(retried).isEmpty();
        verify(refresh).refreshNow();
    }

    private KnowledgeSyncEventConsumer consumer(MfEpClient ep, KnowledgeRefreshService refresh, KnowledgeRetrievalService keyword) {
        return new KnowledgeSyncEventConsumer(ep, refresh, keyword, mock(HybridRagService.class),
                new RagProperties(false, "", "", "", 0.62), new MfAgentProperties(null, null, null));
    }
}
