package com.mf.agentservice.agent.port;

import com.mf.agentservice.api.AgentChatRequest;
import com.mf.agentservice.api.KnowledgeGap;
import com.mf.agentservice.tools.ToolExecutionContext;
import com.mf.agentservice.tools.ToolExecutionRecord;
import com.mf.agentservice.tools.ToolResult;
import java.util.List;

public interface ConversationAuditPort {
    ToolResult<Long> logConversation(ToolExecutionContext context, AgentChatRequest request, String answer, String intent, boolean resolved);

    void logToolCalls(Long conversationId, List<ToolExecutionRecord> records);

    void reportUnresolved(Long conversationId, String question, String reason, KnowledgeGap knowledgeGap);

    void saveSampleCandidate(Long conversationId, String question, String answer);
}
