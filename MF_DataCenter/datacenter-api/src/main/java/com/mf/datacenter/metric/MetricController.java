package com.mf.datacenter.metric;

import com.mf.datacenter.common.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
public class MetricController {

    private final MetricSnapshotService metricSnapshotService;
    private final MetricDefinitionService metricDefinitionService;
    private final MetricComputeRegistryService metricComputeRegistryService;

    public MetricController(
            MetricSnapshotService metricSnapshotService,
            MetricDefinitionService metricDefinitionService,
            MetricComputeRegistryService metricComputeRegistryService
    ) {
        this.metricSnapshotService = metricSnapshotService;
        this.metricDefinitionService = metricDefinitionService;
        this.metricComputeRegistryService = metricComputeRegistryService;
    }

    @GetMapping("/dictionary")
    public ApiResponse<List<MetricDefinitionService.MetricDefinition>> dictionary() {
        return ApiResponse.ok(metricDefinitionService.definitions());
    }

    @GetMapping("/compute-registry")
    public ApiResponse<List<MetricComputeRegistryService.MetricComputeRegistry>> computeRegistry() {
        return ApiResponse.ok(metricComputeRegistryService.registries());
    }

    @PatchMapping("/compute-registry/{id}/enabled")
    public ApiResponse<MetricComputeRegistryService.MetricComputeRegistry> setComputeRegistryEnabled(
            @PathVariable("id") Long id,
            @RequestBody EnabledRequest request
    ) {
        return ApiResponse.ok(metricComputeRegistryService.setEnabled(id, Boolean.TRUE.equals(request.enabled())));
    }

    @PostMapping("/dictionary")
    public ApiResponse<MetricDefinitionService.MetricDefinition> createDefinition(
            @RequestBody MetricDefinitionService.MetricDefinitionRequest request
    ) {
        return ApiResponse.ok(metricDefinitionService.create(request));
    }

    @PutMapping("/dictionary/{id}")
    public ApiResponse<MetricDefinitionService.MetricDefinition> updateDefinition(
            @PathVariable("id") Long id,
            @RequestBody MetricDefinitionService.MetricDefinitionRequest request
    ) {
        return ApiResponse.ok(metricDefinitionService.update(id, request));
    }

    @PatchMapping("/dictionary/{id}/enabled")
    public ApiResponse<MetricDefinitionService.MetricDefinition> setDefinitionEnabled(
            @PathVariable("id") Long id,
            @RequestBody EnabledRequest request
    ) {
        return ApiResponse.ok(metricDefinitionService.setEnabled(id, Boolean.TRUE.equals(request.enabled())));
    }

    @PostMapping("/snapshots/daily/refresh")
    public ApiResponse<List<MetricSnapshotService.MetricSnapshot>> refreshDailySnapshot() {
        return ApiResponse.ok(metricSnapshotService.refreshDaily());
    }

    @PostMapping("/snapshots/hourly/refresh")
    public ApiResponse<List<MetricSnapshotService.MetricSnapshot>> refreshHourlySnapshot() {
        return ApiResponse.ok(metricSnapshotService.refreshHourly());
    }

    @GetMapping("/snapshots")
    public ApiResponse<List<MetricSnapshotService.MetricSnapshot>> snapshots(
            @RequestParam(name = "metricCode", required = false) String metricCode
    ) {
        return ApiResponse.ok(metricSnapshotService.latest(metricCode));
    }

    @GetMapping("/latest")
    public ApiResponse<Map<String, MetricSnapshotService.MetricSnapshot>> latest() {
        return ApiResponse.ok(metricSnapshotService.latestDashboardMetrics());
    }

    @GetMapping("/query")
    public ApiResponse<List<MetricSnapshotService.MetricSnapshot>> query(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "codes", required = false) List<String> codes,
            @RequestParam(name = "period", required = false) String period,
            @RequestParam(name = "dimensionKey", required = false) String dimensionKey,
            @RequestParam(name = "dimensionValue", required = false) String dimensionValue,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        return ApiResponse.ok(metricSnapshotService.query(codes, code, period, dimensionKey, dimensionValue, startDate, endDate, limit));
    }

    public record EnabledRequest(Boolean enabled) {
    }
}
