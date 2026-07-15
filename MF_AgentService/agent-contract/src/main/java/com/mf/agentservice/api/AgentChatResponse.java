package com.mf.agentservice.api;

import com.mf.agentservice.tools.ToolCallSummary;
import java.util.List;

public record AgentChatResponse(
        String answer,
        String intent,
        Boolean resolved,
        List<ToolCallSummary> usedTools,
        Long conversationId,
        String fallbackReason,
        List<AgentSource> sources,
        Integer confidence,
        Boolean reviewRequired,
        KnowledgeGap knowledgeGap
) {
}
