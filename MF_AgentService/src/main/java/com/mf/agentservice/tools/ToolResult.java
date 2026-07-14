package com.mf.agentservice.tools;

public record ToolResult<T>(
        T value,
        Boolean success,
        String summary,
        String errorMessage,
        ToolFailureReason failureReason
) {
    public static <T> ToolResult<T> ok(T value, String summary) {
        return new ToolResult<>(value, true, summary, null, null);
    }

    public static <T> ToolResult<T> fail(ToolFailureReason failureReason, String errorMessage) {
        return new ToolResult<>(null, false, null, errorMessage, failureReason);
    }
}
