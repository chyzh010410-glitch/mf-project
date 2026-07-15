package com.mf.agentservice.agent.port;

import com.fasterxml.jackson.databind.JsonNode;
import com.mf.agentservice.tools.ToolExecutionContext;
import com.mf.agentservice.tools.ToolResult;

public interface OrderToolPort {
    ToolResult<JsonNode> status(ToolExecutionContext context, Long orderId, String authToken);
}
