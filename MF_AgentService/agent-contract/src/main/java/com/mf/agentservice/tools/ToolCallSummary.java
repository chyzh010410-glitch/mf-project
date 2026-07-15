package com.mf.agentservice.tools;

public record ToolCallSummary(
        String name,
        Boolean success,
        Long durationMs,
        String failureReason
) {
}
