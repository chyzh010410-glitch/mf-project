package com.mf.agentservice.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.mf.agentservice.agent.port.OrderToolPort;
import com.mf.agentservice.client.MfEpClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class OrderTools implements OrderToolPort {
    private final MfEpClient mfEpClient;
    private final ToolExecutor executor;

    public OrderTools(MfEpClient mfEpClient, ToolExecutor executor) {
        this.mfEpClient = mfEpClient;
        this.executor = executor;
    }

    @Tool(name = McpToolNames.ORDER_STATUS, description = "Read a user's own order status. Requires user Authorization.")
    public ToolResult<JsonNode> status(ToolExecutionContext context, Long orderId, String authToken) {
        return executor.execute(context, McpToolNames.ORDER_STATUS, "orderId=" + orderId,
                () -> {
                    if (authToken == null || authToken.isBlank()) {
                        return ToolResult.fail(ToolFailureReason.ORDER_AUTH_REQUIRED, "ORDER_AUTH_REQUIRED");
                    }
                    return ToolResult.ok(mfEpClient.orderStatus(orderId, authToken), "order status loaded");
                });
    }
}
