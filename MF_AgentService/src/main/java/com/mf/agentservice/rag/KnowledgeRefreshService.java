package com.mf.agentservice.rag;

import com.mf.agentservice.client.MfEpClient;
import com.mf.agentservice.config.MfAgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeRefreshService {
    private static final Logger LOG = LoggerFactory.getLogger(KnowledgeRefreshService.class);

    private final MfEpClient mfEpClient;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final MfAgentProperties properties;

    public KnowledgeRefreshService(
            MfEpClient mfEpClient,
            KnowledgeRetrievalService knowledgeRetrievalService,
            MfAgentProperties properties
    ) {
        this.mfEpClient = mfEpClient;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void refreshOnStartup() {
        refresh();
    }

    @Scheduled(fixedDelayString = "${mf.agent.knowledge-refresh-interval:10m}")
    public void refresh() {
        try {
            refreshNow();
        } catch (RuntimeException exception) {
            LOG.warn("Knowledge refresh skipped; keeping the last successful index: {}", exception.getMessage());
        }
    }

    public int refreshNow() {
        knowledgeRetrievalService.refresh(
                mfEpClient.searchEncyclopedia("", 1, 100),
                mfEpClient.listFaq(),
                mfEpClient.listArticles(1, 100));
        return knowledgeRetrievalService.documents().size();
    }
}
