package com.mf.agentservice.agent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KnowledgeGapAdvisorTest {
    private final KnowledgeGapAdvisor advisor = new KnowledgeGapAdvisor();

    @Test
    void createsAgricultureKnowledgeGapWithSuggestedContentTypes() {
        var gap = advisor.assess(AgentIntent.ENCYCLOPEDIA, "knowledge_not_enough", "苹果树炭疽病怎么处理");

        assertThat(gap).isPresent();
        assertThat(gap.get().riskLevel()).isEqualTo("medium");
        assertThat(gap.get().suggestedContentTypes()).containsExactly("faq", "article", "encyclopedia");
    }

    @Test
    void doesNotTurnUpstreamFailureIntoKnowledgeGap() {
        assertThat(advisor.assess(AgentIntent.ENCYCLOPEDIA, "upstream_timeout", "苹果树黄叶怎么办")).isEmpty();
    }
}
