package com.mf.agentservice.tools;

public record ToolResult<T>(
        T value,
        Boolean success,
        String summary,
        String errorMessage
) {
    public static <T> ToolResult<T> ok(T value, String summary) {
        return new ToolResult<>(value, true, summary, null);
    }

    public static <T> ToolResult<T> fail(String errorMessage) {
        return new ToolResult<>(null, false, null, errorMessage);
    }
}
