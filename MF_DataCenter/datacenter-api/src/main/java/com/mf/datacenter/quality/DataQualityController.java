package com.mf.datacenter.quality;

import com.mf.datacenter.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/data-quality")
public class DataQualityController {

    private final DataQualityService dataQualityService;

    public DataQualityController(DataQualityService dataQualityService) {
        this.dataQualityService = dataQualityService;
    }

    @GetMapping("/checks")
    public ApiResponse<List<DataQualityService.DataQualityCheck>> checks() {
        return ApiResponse.ok(dataQualityService.latest());
    }

    @GetMapping("/summary")
    public ApiResponse<DataQualityService.QualitySummary> summary() {
        return ApiResponse.ok(dataQualityService.summary());
    }

    @GetMapping("/rules")
    public ApiResponse<List<DataQualityService.DataQualityRule>> rules() {
        return ApiResponse.ok(dataQualityService.rules());
    }

    @PostMapping("/rules")
    public ApiResponse<DataQualityService.DataQualityRule> createRule(
            @RequestBody DataQualityService.DataQualityRuleRequest request
    ) {
        return ApiResponse.ok(dataQualityService.createRule(request));
    }

    @PutMapping("/rules/{id}")
    public ApiResponse<DataQualityService.DataQualityRule> updateRule(
            @PathVariable("id") Long id,
            @RequestBody DataQualityService.DataQualityRuleRequest request
    ) {
        return ApiResponse.ok(dataQualityService.updateRule(id, request));
    }

    @PatchMapping("/rules/{id}/enabled")
    public ApiResponse<DataQualityService.DataQualityRule> setRuleEnabled(
            @PathVariable("id") Long id,
            @RequestBody EnabledRequest request
    ) {
        return ApiResponse.ok(dataQualityService.setRuleEnabled(id, Boolean.TRUE.equals(request.enabled())));
    }

    @GetMapping("/issues")
    public ApiResponse<List<DataQualityService.DataQualityIssue>> issues(
            @RequestParam(name = "status", required = false) String status
    ) {
        return ApiResponse.ok(dataQualityService.issues(status));
    }

    @GetMapping("/issues/trend")
    public ApiResponse<List<DataQualityService.IssueTrendPoint>> issueTrend() {
        return ApiResponse.ok(dataQualityService.issueTrend());
    }

    @GetMapping("/issues/{id}/history")
    public ApiResponse<List<DataQualityService.DataQualityIssueHistory>> issueHistory(
            @PathVariable("id") Long id
    ) {
        return ApiResponse.ok(dataQualityService.issueHistory(id));
    }

    @PatchMapping("/issues/{id}/status")
    public ApiResponse<DataQualityService.DataQualityIssue> updateIssueStatus(
            @PathVariable("id") Long id,
            @RequestBody DataQualityService.IssueStatusRequest request
    ) {
        return ApiResponse.ok(dataQualityService.updateIssueStatus(id, request));
    }

    @PostMapping("/run")
    public ApiResponse<List<DataQualityService.DataQualityCheck>> run() {
        return ApiResponse.ok(dataQualityService.runChecks());
    }

    public record EnabledRequest(Boolean enabled) {
    }
}
