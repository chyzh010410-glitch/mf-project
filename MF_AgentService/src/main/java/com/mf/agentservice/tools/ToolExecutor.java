package com.mf.agentservice.tools;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class ToolExecutor {

    public <T> ToolResult<T> execute(
            ToolExecutionContext context,
            String toolName,
            String requestSummary,
            Supplier<ToolResult<T>> action
    ) {
        long startedAt = System.nanoTime();
        try {
            var result = action.get();
            context.add(new ToolExecutionRecord(
                    toolName,
                    requestSummary,
                    summarize(result.summary()),
                    result.success(),
                    result.errorMessage(),
                    elapsedMs(startedAt)
            ));
            return result;
        } catch (RuntimeException ex) {
            context.add(new ToolExecutionRecord(
                    toolName,
                    requestSummary,
                    null,
                    false,
                    summarize(ex.getMessage()),
                    elapsedMs(startedAt)
            ));
            return ToolResult.fail(ex.getMessage());
        }
    }

    private long elapsedMs(long startedAt) {
        return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
    }

    private String summarize(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
