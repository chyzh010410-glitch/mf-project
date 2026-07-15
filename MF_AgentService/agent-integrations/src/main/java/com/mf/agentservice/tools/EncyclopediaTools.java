package com.mf.agentservice.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.mf.agentservice.agent.port.EncyclopediaToolPort;
import com.mf.agentservice.client.MfEpClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class EncyclopediaTools implements EncyclopediaToolPort {
    private final MfEpClient mfEpClient;
    private final ToolExecutor executor;

    public EncyclopediaTools(MfEpClient mfEpClient, ToolExecutor executor) {
        this.mfEpClient = mfEpClient;
        this.executor = executor;
    }

    @Tool(name = McpToolNames.ENCYCLOPEDIA_SEARCH, description = "Search MF encyclopedia entries by keyword.")
    public ToolResult<JsonNode> search(ToolExecutionContext context, String keyword) {
        return executor.execute(context, McpToolNames.ENCYCLOPEDIA_SEARCH, "keyword=" + keyword,
                () -> ToolResult.ok(mfEpClient.searchEncyclopedia(keyword, 1, 5), "encyclopedia search completed"));
    }
}
