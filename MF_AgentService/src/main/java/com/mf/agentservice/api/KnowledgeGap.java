package com.mf.agentservice.api;

import java.util.List;

public record KnowledgeGap(
        String topic,
        String reason,
        String riskLevel,
        List<String> suggestedContentTypes
) {
}
