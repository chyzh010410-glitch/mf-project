package com.mf.datacenter.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public class EvaluationRecords {
    public record CreateSuiteRequest(@NotBlank String name, String description) {}
    public record Suite(Long id, String name, String description, LocalDateTime createTime) {}
    public record CreateCaseRequest(@NotNull Long suiteId, @NotBlank String question, String expectedIntent,
                                    String expectedTool, String expectedSafetyResult, String tags, Boolean enabled) {}
    public record EvaluationCase(Long id, Long suiteId, String question, String expectedIntent, String expectedTool,
                                 String expectedSafetyResult, String tags, Boolean enabled, LocalDateTime createTime) {}
    public record CreateResultRequest(@NotNull Long caseId, String actualIntent, String actualTools,
                                      String actualFallbackReason, String answerSnapshot, Boolean passed, String failureReason) {}
    public record Result(Long id, Long caseId, String actualIntent, String actualTools, String actualFallbackReason,
                         String answerSnapshot, Boolean passed, String failureReason, LocalDateTime executedAt) {}
    public record Summary(long total, long passed, double passRate, List<Result> failedResults) {}
}
