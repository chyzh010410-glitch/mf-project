package com.mf.datacenter.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class AiRecords {

    public record CreateConversationRequest(
            @NotBlank String source,
            String sessionId,
            String userId,
            String userType,
            @NotBlank String question,
            String answer,
            String intent,
            Boolean resolved,
            Integer satisfaction
    ) {
    }

    public record Conversation(
            Long id,
            String source,
            String sessionId,
            String userId,
            String userType,
            String question,
            String answer,
            String intent,
            Boolean resolved,
            Integer satisfaction,
            LocalDateTime createTime
    ) {
    }

    public record CreateToolCallRequest(
            Long conversationId,
            @NotBlank String toolName,
            String requestSummary,
            String responseSummary,
            Boolean success,
            String errorMessage,
            Long durationMs
    ) {
    }

    public record ToolCall(
            Long id,
            Long conversationId,
            String toolName,
            String requestSummary,
            String responseSummary,
            Boolean success,
            String errorMessage,
            Long durationMs,
            LocalDateTime createTime
    ) {
    }

    public record CreateUnresolvedQuestionRequest(
            Long conversationId,
            @NotBlank String question,
            String reason,
            @NotBlank String status,
            String owner,
            String remark
    ) {
    }

    public record UpdateUnresolvedStatusRequest(
            @NotBlank String status,
            String owner,
            String remark
    ) {
    }

    public record UnresolvedQuestion(
            Long id,
            Long conversationId,
            String question,
            String reason,
            String status,
            String owner,
            String remark,
            LocalDateTime createTime,
            LocalDateTime updateTime
    ) {
    }

    public record CreateSampleCandidateRequest(
            Long conversationId,
            @NotBlank String question,
            @NotBlank String answer,
            String source,
            String qualityStatus,
            @NotBlank String reviewStatus,
            String reviewer,
            String reviewRemark
    ) {
    }

    public record ReviewSampleRequest(
            @NotBlank String reviewStatus,
            String reviewer,
            String reviewRemark
    ) {
    }

    public record SampleCandidate(
            Long id,
            Long conversationId,
            String question,
            String answer,
            String source,
            String qualityStatus,
            String reviewStatus,
            String reviewer,
            String reviewRemark,
            LocalDateTime createTime,
            LocalDateTime updateTime
    ) {
    }

    public record AiStats(
            Integer conversationTotal,
            Integer uniqueUserTotal,
            Long unresolvedTotal,
            Integer toolCallTotal,
            Integer sampleCandidateTotal,
            java.util.List<QuestionCount> frequentQuestions
    ) {
    }

    public record QuestionCount(String question, Long count) {
    }

    public record MetricSnapshot(
            @NotNull Long id,
            String metricCode,
            String metricName,
            String metricValue,
            String dimensionKey,
            String dimensionValue,
            String snapshotGranularity,
            String snapshotDate,
            LocalDateTime snapshotTime,
            LocalDateTime createTime
    ) {
    }
}
