package com.mf.datacenter.ai;

import com.mf.datacenter.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/evaluations")
public class EvaluationController {
    private final EvaluationStore store;
    public EvaluationController(EvaluationStore store) { this.store = store; }
    @PostMapping("/suites") public ApiResponse<EvaluationRecords.Suite> createSuite(@Valid @RequestBody EvaluationRecords.CreateSuiteRequest request) { return ApiResponse.ok(store.createSuite(request)); }
    @GetMapping("/suites") public ApiResponse<?> suites() { return ApiResponse.ok(store.suites()); }
    @PostMapping("/cases") public ApiResponse<EvaluationRecords.EvaluationCase> createCase(@Valid @RequestBody EvaluationRecords.CreateCaseRequest request) { return ApiResponse.ok(store.createCase(request)); }
    @GetMapping("/cases") public ApiResponse<?> cases(@RequestParam(required = false) Long suiteId) { return ApiResponse.ok(store.cases(suiteId)); }
    @PostMapping("/results") public ApiResponse<EvaluationRecords.Result> record(@Valid @RequestBody EvaluationRecords.CreateResultRequest request) { return ApiResponse.ok(store.record(request)); }
    @GetMapping("/results") public ApiResponse<?> results() { return ApiResponse.ok(store.results()); }
    @GetMapping("/summary") public ApiResponse<EvaluationRecords.Summary> summary() { return ApiResponse.ok(store.summary()); }
}
