package com.mf.agentservice.tools;

public record ToolExecutionRecord(
        String name,
        String requestSummary,
        String responseSummary,
        Boolean success,
        String errorMessage,
        ToolFailureReason failureReason,
        Long durationMs
) {
    public ToolCallSummary summary() {
        return new ToolCallSummary(name, success, durationMs,
                failureReason == null ? null : failureReason.name().toLowerCase());
    }
}
