package com.mf.agentservice.agent.port;

import com.mf.agentservice.tools.ToolExecutionContext;
import com.mf.agentservice.tools.ToolResult;

public interface MerchantToolPort {
    ToolResult<String> guide(ToolExecutionContext context);
}
