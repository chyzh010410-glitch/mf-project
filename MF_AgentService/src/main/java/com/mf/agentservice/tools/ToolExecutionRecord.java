package com.mf.agentservice.tools;

public record ToolExecutionRecord(
        String name,
        String requestSummary,
        String responseSummary,
        Boolean success,
        String errorMessage,
        Long durationMs
) {
    public ToolCallSummary summary() {
        return new ToolCallSummary(name, success, durationMs);
    }
}
