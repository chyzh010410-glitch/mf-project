package com.mf.agentservice.rag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KnowledgeRetrievalServiceTest {
    private final KnowledgeRetrievalService service = new KnowledgeRetrievalService();

    @Test
    void genericCropNameDoesNotMatchAnUnrelatedDiseaseArticle() {
        assertThat(service.retrieve("苹果树炭疽病怎么处理"))
                .noneMatch(document -> "apple-rot".equals(document.sourceId()));
    }

    @Test
    void specificDiseaseStillMatchesTheCorrectKnowledge() {
        assertThat(service.retrieve("苹果树腐烂病怎么处理"))
                .extracting(RagDocument::sourceId)
                .contains("apple-rot");
    }
}
