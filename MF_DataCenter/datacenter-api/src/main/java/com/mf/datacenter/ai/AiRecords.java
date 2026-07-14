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

    public record UpdateSatisfactionRequest(@NotNull Integer satisfaction, String feedback) {
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

    public record ConversationTrace(
            Conversation conversation,
            java.util.List<ToolCall> toolCalls,
            java.util.List<UnresolvedQuestion> unresolvedQuestions,
            java.util.List<SampleCandidate> sampleCandidates
    ) {
    }

    public record UserConversation(Long id, String question, String answer, String intent, LocalDateTime createdAt) {}

    public record UserConversationPage(String sessionId, java.util.List<UserConversation> items, long page, long pageSize, long total) {}

    public record DeleteConversationResult(String sessionId, long deletedCount) {}

    public record CreateUnresolvedQuestionRequest(
            Long conversationId,
            @NotBlank String question,
            String reason,
            @NotBlank String status,
            String priority,
            LocalDateTime dueTime,
            String owner,
            String remark,
            String knowledgeAction
    ) {
    }

    public record UpdateUnresolvedStatusRequest(
            @NotBlank String status,
            String priority,
            LocalDateTime dueTime,
            String owner,
            String remark,
            String knowledgeAction
    ) {
    }

    public record UnresolvedQuestion(
            Long id,
            Long conversationId,
            String question,
            String reason,
            String status,
            String priority,
            LocalDateTime dueTime,
            String owner,
            String remark,
            String knowledgeAction,
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
            String reviewRemark,
            Boolean recommendedForKnowledge
    ) {
    }

    public record ReviewSampleRequest(
            @NotBlank String reviewStatus,
            String reviewer,
            String reviewRemark,
            Boolean recommendedForKnowledge
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
            Boolean recommendedForKnowledge,
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
