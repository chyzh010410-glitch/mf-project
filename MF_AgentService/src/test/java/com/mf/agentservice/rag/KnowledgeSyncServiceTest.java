package com.mf.agentservice.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

class KnowledgeSyncServiceTest {

    @Test
    void sameRequestIdReusesTheCompletedRefresh() {
        var refreshService = mock(KnowledgeRefreshService.class);
        when(refreshService.refreshNow()).thenReturn(12);
        var service = new KnowledgeSyncService(refreshService);

        var first = service.sync("publish-101", List.of(101L));
        var second = service.sync("publish-101", List.of(101L));

        assertThat(first.success()).isTrue();
        assertThat(first.reused()).isFalse();
        assertThat(first.contentIds()).containsExactly(101L);
        assertThat(second.reused()).isTrue();
        assertThat(second.indexedDocuments()).isEqualTo(12);
        verify(refreshService, times(1)).refreshNow();
    }

    @Test
    void failedRefreshReturnsTheReasonAndCanBeRetried() {
        var refreshService = mock(KnowledgeRefreshService.class);
        doThrow(new IllegalStateException("MF_EP unavailable")).doReturn(9).when(refreshService).refreshNow();
        var service = new KnowledgeSyncService(refreshService);

        var failed = service.sync("offline-101", List.of(101L));
        var retried = service.sync("offline-101", List.of(101L));

        assertThat(failed.success()).isFalse();
        assertThat(failed.error()).contains("MF_EP unavailable");
        assertThat(retried.success()).isTrue();
        assertThat(retried.indexedDocuments()).isEqualTo(9);
        verify(refreshService, times(2)).refreshNow();
    }
}
