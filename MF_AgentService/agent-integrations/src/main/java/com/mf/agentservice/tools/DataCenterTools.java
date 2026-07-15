package com.mf.agentservice.tools;

import com.mf.agentservice.api.AgentChatRequest;
import com.mf.agentservice.api.KnowledgeGap;
import com.mf.agentservice.agent.port.ConversationAuditPort;
import com.mf.agentservice.client.DataCenterClient;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class DataCenterTools implements ConversationAuditPort {
    private final DataCenterClient dataCenterClient;
    private final ToolExecutor executor;

    public DataCenterTools(DataCenterClient dataCenterClient, ToolExecutor executor) {
        this.dataCenterClient = dataCenterClient;
        this.executor = executor;
    }

    @Tool(name = McpToolNames.DATACENTER_LOG_CONVERSATION, description = "Write an Agent conversation log to MF_DataCenter.")
    public ToolResult<Long> logConversation(
            ToolExecutionContext context,
            AgentChatRequest request,
            String answer,
            String intent,
            boolean resolved
    ) {
        return executor.execute(context, McpToolNames.DATACENTER_LOG_CONVERSATION, "sessionId=" + request.sessionId(),
                () -> {
                    var body = new LinkedHashMap<String, Object>();
                    body.put("source", "MF_AgentService");
                    body.put("sessionId", request.sessionId());
                    body.put("userId", request.userId());
                    body.put("userType", request.userType());
                    body.put("question", request.message());
                    body.put("answer", answer);
                    body.put("intent", intent);
                    body.put("resolved", resolved);
                    body.put("satisfaction", null);
                    Long id = dataCenterClient.logConversation(body);
                    return ToolResult.ok(id, "conversation logged id=" + id);
                });
    }

    @Tool(name = McpToolNames.DATACENTER_LOG_TOOL_CALL, description = "Write Agent tool call logs to MF_DataCenter.")
    public void logToolCalls(Long conversationId, List<ToolExecutionRecord> records) {
        for (var record : records) {
            if (McpToolNames.DATACENTER_LOG_TOOL_CALL.equals(record.name())) {
                continue;
            }
            var body = new LinkedHashMap<String, Object>();
            body.put("conversationId", conversationId);
            body.put("toolName", record.name());
            body.put("requestSummary", record.requestSummary());
            body.put("responseSummary", record.responseSummary());
            body.put("success", record.success());
            body.put("errorMessage", record.errorMessage());
            body.put("durationMs", record.durationMs());
            dataCenterClient.logToolCall(body);
        }
    }

    @Tool(name = McpToolNames.DATACENTER_REPORT_UNRESOLVED, description = "Write an unresolved question to MF_DataCenter.")
    public void reportUnresolved(Long conversationId, String question, String reason, KnowledgeGap knowledgeGap) {
        var body = new LinkedHashMap<String, Object>();
        body.put("conversationId", conversationId);
        body.put("question", question);
        body.put("reason", reason);
        body.put("status", "pending");
        body.put("owner", "content-ops");
        body.put("remark", knowledgeGap == null ? "Created by MF_AgentService"
                : "Created by MF_AgentService; topic=" + knowledgeGap.topic()
                + "; risk=" + knowledgeGap.riskLevel()
                + "; suggested=" + String.join(",", knowledgeGap.suggestedContentTypes()));
        dataCenterClient.reportUnresolved(body);
    }

    @Tool(name = McpToolNames.DATACENTER_SAVE_SAMPLE_CANDIDATE, description = "Write a high quality answer candidate to MF_DataCenter.")
    public void saveSampleCandidate(Long conversationId, String question, String answer) {
        var body = new LinkedHashMap<String, Object>();
        body.put("conversationId", conversationId);
        body.put("question", question);
        body.put("answer", answer);
        body.put("source", "conversation");
        body.put("qualityStatus", "good");
        body.put("reviewStatus", "pending");
        body.put("reviewer", "");
        body.put("reviewRemark", "");
        dataCenterClient.saveSampleCandidate(body);
    }
}
