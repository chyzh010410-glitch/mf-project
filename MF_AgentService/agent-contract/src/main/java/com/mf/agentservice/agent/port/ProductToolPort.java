package com.mf.agentservice.agent.port;

import com.fasterxml.jackson.databind.JsonNode;
import com.mf.agentservice.tools.ToolExecutionContext;
import com.mf.agentservice.tools.ToolResult;

public interface ProductToolPort {
    ToolResult<JsonNode> search(ToolExecutionContext context, String keyword, String productType);
}
