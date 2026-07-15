package com.mf.agentservice.tools;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

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
                    result.success() ? summarize(result.summary()) : result.failureReason().name().toLowerCase(),
                    result.success(),
                    result.errorMessage(),
                    result.failureReason(),
                    elapsedMs(startedAt)
            ));
            return result;
        } catch (RuntimeException ex) {
            context.add(new ToolExecutionRecord(
                    toolName,
                    requestSummary,
                    null,
                    false,
                    safeMessage(ex),
                    failureReason(ex),
                    elapsedMs(startedAt)
            ));
            return ToolResult.fail(failureReason(ex), safeMessage(ex));
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

    private ToolFailureReason failureReason(RuntimeException exception) {
        if (hasCause(exception, SocketTimeoutException.class) || hasCause(exception, TimeoutException.class)) {
            return ToolFailureReason.UPSTREAM_TIMEOUT;
        }
        if (exception instanceof RestClientResponseException) {
            return ToolFailureReason.UPSTREAM_BUSINESS_ERROR;
        }
        if (exception instanceof ResourceAccessException) {
            return ToolFailureReason.UPSTREAM_UNAVAILABLE;
        }
        return ToolFailureReason.UNKNOWN;
    }

    private boolean hasCause(Throwable exception, Class<? extends Throwable> type) {
        Throwable current = exception;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : summarize(message);
    }
}
