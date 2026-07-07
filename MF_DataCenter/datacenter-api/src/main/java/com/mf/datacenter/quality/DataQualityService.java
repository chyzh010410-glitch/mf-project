package com.mf.datacenter.quality;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mf.datacenter.metric.MetricDefinitionService;
import com.mf.datacenter.metric.MetricSnapshotService;
import com.mf.datacenter.quality.entity.DataQualityCheckEntity;
import com.mf.datacenter.quality.entity.DataQualityIssueHistoryEntity;
import com.mf.datacenter.quality.entity.DataQualityIssueEntity;
import com.mf.datacenter.quality.entity.DataQualityRuleEntity;
import com.mf.datacenter.quality.mapper.DataQualityCheckMapper;
import com.mf.datacenter.quality.mapper.DataQualityIssueHistoryMapper;
import com.mf.datacenter.quality.mapper.DataQualityIssueMapper;
import com.mf.datacenter.quality.mapper.DataQualityRuleMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DataQualityService {

    private final DataQualityCheckMapper dataQualityCheckMapper;
    private final DataQualityRuleMapper dataQualityRuleMapper;
    private final DataQualityIssueMapper dataQualityIssueMapper;
    private final DataQualityIssueHistoryMapper dataQualityIssueHistoryMapper;
    private final MetricDefinitionService metricDefinitionService;
    private final MetricSnapshotService metricSnapshotService;

    public DataQualityService(
            ObjectProvider<DataQualityCheckMapper> dataQualityCheckMapper,
            ObjectProvider<DataQualityRuleMapper> dataQualityRuleMapper,
            ObjectProvider<DataQualityIssueMapper> dataQualityIssueMapper,
            ObjectProvider<DataQualityIssueHistoryMapper> dataQualityIssueHistoryMapper,
            MetricDefinitionService metricDefinitionService,
            MetricSnapshotService metricSnapshotService
    ) {
        this.dataQualityCheckMapper = dataQualityCheckMapper.getIfAvailable();
        this.dataQualityRuleMapper = dataQualityRuleMapper.getIfAvailable();
        this.dataQualityIssueMapper = dataQualityIssueMapper.getIfAvailable();
        this.dataQualityIssueHistoryMapper = dataQualityIssueHistoryMapper.getIfAvailable();
        this.metricDefinitionService = metricDefinitionService;
        this.metricSnapshotService = metricSnapshotService;
    }

    public List<DataQualityCheck> runChecks() {
        if (dataQualityCheckMapper == null || !metricSnapshotService.available()) {
            return List.of();
        }
        var now = LocalDateTime.now();
        var latest = metricSnapshotService.latestDashboardMetrics();
        var rows = new ArrayList<DataQualityCheckEntity>();
        for (var rule : activeRules()) {
            if ("daily_snapshot_presence".equals(rule.checkType())) {
                var dailyRows = metricSnapshotService.query(null, "daily", LocalDate.now(), LocalDate.now());
                rows.add(row(rule.ruleCode(), rule.ruleName(), null, dailyRows.isEmpty() ? "failed" : "passed",
                        rule.severity(), "daily snapshot exists", String.valueOf(dailyRows.size()),
                        dailyRows.isEmpty() ? "今天还没有日快照" : "今天已有日快照", now));
                continue;
            }

            for (var definition : metricDefinitionService.activeDefinitions()) {
                if ("category_sales".equals(definition.metricCode())) {
                    continue;
                }
                if (rule.metricCode() != null && !rule.metricCode().isBlank()
                        && !rule.metricCode().equals(definition.metricCode())) {
                    continue;
                }
                var snapshot = latest.get(definition.metricCode());
                if ("snapshot_missing".equals(rule.checkType())) {
                    rows.add(row(rule.ruleCode(), rule.ruleName(), definition.metricCode(), snapshot == null ? "failed" : "passed",
                            rule.severity(), "latest snapshot", snapshot == null ? "missing" : "exists",
                            snapshot == null ? "指标没有可用快照" : "指标已有可用快照", now));
                } else if ("snapshot_freshness".equals(rule.checkType()) && snapshot != null) {
                    var thresholdMinutes = parseLong(rule.thresholdValue(), 120L);
                    var ageMinutes = Duration.between(snapshot.snapshotTime(), now).toMinutes();
                    rows.add(row(rule.ruleCode(), rule.ruleName(), definition.metricCode(), ageMinutes <= thresholdMinutes ? "passed" : "failed",
                            rule.severity(), "<=" + thresholdMinutes + "m", ageMinutes + "m",
                            ageMinutes <= thresholdMinutes ? "快照在可接受窗口内" : "快照超过新鲜度窗口", now));
                } else if ("negative_value".equals(rule.checkType()) && snapshot != null) {
                    var passed = snapshot.metricValue().compareTo(BigDecimal.ZERO) >= 0;
                    rows.add(row(rule.ruleCode(), rule.ruleName(), definition.metricCode(), passed ? "passed" : "failed",
                            rule.severity(), ">=0", snapshot.metricValue().toPlainString(),
                            passed ? "指标值正常" : "指标值为负数", now));
                }
            }
        }
        for (var row : rows) {
            dataQualityCheckMapper.insert(row);
            syncIssue(row, now);
        }
        return rows.stream().map(this::toRecord).toList();
    }

    public List<DataQualityCheck> latest() {
        if (dataQualityCheckMapper == null) {
            return List.of();
        }
        var rows = dataQualityCheckMapper.selectList(new LambdaQueryWrapper<DataQualityCheckEntity>()
                .orderByDesc(DataQualityCheckEntity::getCheckTime)
                .orderByAsc(DataQualityCheckEntity::getMetricCode)
                .last("LIMIT 300"));
        if (rows.isEmpty()) {
            return List.of();
        }
        var latestByKey = rows.stream()
                .collect(Collectors.toMap(
                        row -> row.getCheckCode() + ":" + (row.getMetricCode() == null ? "" : row.getMetricCode()),
                        Function.identity(),
                        (left, right) -> left.getCheckTime().isAfter(right.getCheckTime()) ? left : right
                ));
        return latestByKey.values().stream()
                .sorted(Comparator.comparing(DataQualityCheckEntity::getStatus).thenComparing(DataQualityCheckEntity::getMetricCode, Comparator.nullsLast(String::compareTo)))
                .map(this::toRecord)
                .toList();
    }

    public QualitySummary summary() {
        var checks = latest();
        var failed = checks.stream().filter(item -> "failed".equals(item.status())).count();
        var warning = checks.stream().filter(item -> "warning".equals(item.severity())).count();
        var passed = checks.stream().filter(item -> "passed".equals(item.status())).count();
        var latestTime = checks.stream().map(DataQualityCheck::checkTime).max(LocalDateTime::compareTo).orElse(null);
        return new QualitySummary(checks.size(), passed, failed, warning, latestTime);
    }

    public List<DataQualityRule> rules() {
        if (dataQualityRuleMapper == null) {
            return fallbackRules();
        }
        return dataQualityRuleMapper.selectList(new LambdaQueryWrapper<DataQualityRuleEntity>()
                        .orderByDesc(DataQualityRuleEntity::getEnabled)
                        .orderByAsc(DataQualityRuleEntity::getRuleCode))
                .stream()
                .map(this::toRuleRecord)
                .toList();
    }

    public DataQualityRule createRule(DataQualityRuleRequest request) {
        ensureRuleMapper();
        validateRule(request);
        var exists = dataQualityRuleMapper.selectOne(new LambdaQueryWrapper<DataQualityRuleEntity>()
                .eq(DataQualityRuleEntity::getRuleCode, request.ruleCode().trim()));
        if (exists != null) {
            throw new IllegalArgumentException("规则编码已存在");
        }
        var now = LocalDateTime.now();
        var entity = new DataQualityRuleEntity();
        applyRule(entity, request);
        entity.setEnabled(request.enabled() == null || request.enabled());
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        dataQualityRuleMapper.insert(entity);
        return toRuleRecord(entity);
    }

    public DataQualityRule updateRule(Long id, DataQualityRuleRequest request) {
        ensureRuleMapper();
        validateRule(request);
        var entity = requireRule(id);
        var exists = dataQualityRuleMapper.selectOne(new LambdaQueryWrapper<DataQualityRuleEntity>()
                .eq(DataQualityRuleEntity::getRuleCode, request.ruleCode().trim())
                .ne(DataQualityRuleEntity::getId, id));
        if (exists != null) {
            throw new IllegalArgumentException("规则编码已存在");
        }
        applyRule(entity, request);
        entity.setEnabled(request.enabled() == null || request.enabled());
        entity.setUpdateTime(LocalDateTime.now());
        dataQualityRuleMapper.updateById(entity);
        return toRuleRecord(entity);
    }

    public DataQualityRule setRuleEnabled(Long id, boolean enabled) {
        ensureRuleMapper();
        var entity = requireRule(id);
        var now = LocalDateTime.now();
        dataQualityRuleMapper.update(null, new LambdaUpdateWrapper<DataQualityRuleEntity>()
                .eq(DataQualityRuleEntity::getId, id)
                .set(DataQualityRuleEntity::getEnabled, enabled)
                .set(DataQualityRuleEntity::getUpdateTime, now));
        entity.setEnabled(enabled);
        entity.setUpdateTime(now);
        return toRuleRecord(entity);
    }

    public List<DataQualityIssue> issues(String status) {
        if (dataQualityIssueMapper == null) {
            return List.of();
        }
        var wrapper = new LambdaQueryWrapper<DataQualityIssueEntity>()
                .orderByDesc(DataQualityIssueEntity::getLastSeenTime)
                .last("LIMIT 200");
        if (status != null && !status.isBlank()) {
            wrapper.eq(DataQualityIssueEntity::getStatus, status);
        }
        return dataQualityIssueMapper.selectList(wrapper).stream().map(this::toIssueRecord).toList();
    }

    public DataQualityIssue updateIssueStatus(Long id, IssueStatusRequest request) {
        if (dataQualityIssueMapper == null) {
            throw new IllegalArgumentException("当前未启用质量问题数据库");
        }
        var entity = dataQualityIssueMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("质量问题不存在");
        }
        var fromStatus = entity.getStatus();
        var status = normalizeStatus(request.status());
        entity.setStatus(status);
        entity.setOwner(request.owner());
        entity.setResolutionNote(request.resolutionNote());
        if ("resolved".equals(status) || "ignored".equals(status)) {
            entity.setResolvedBy(blank(request.resolvedBy()) ? request.owner() : request.resolvedBy());
            entity.setResolvedTime(LocalDateTime.now());
        } else {
            entity.setResolvedBy(null);
            entity.setResolvedTime(null);
        }
        entity.setUpdateTime(LocalDateTime.now());
        dataQualityIssueMapper.updateById(entity);
        writeIssueHistory(entity.getId(), fromStatus, status, blank(request.resolvedBy()) ? request.owner() : request.resolvedBy(), request.resolutionNote());
        return toIssueRecord(entity);
    }

    public List<DataQualityIssueHistory> issueHistory(Long issueId) {
        if (dataQualityIssueHistoryMapper == null) {
            return List.of();
        }
        return dataQualityIssueHistoryMapper.selectList(new LambdaQueryWrapper<DataQualityIssueHistoryEntity>()
                        .eq(DataQualityIssueHistoryEntity::getIssueId, issueId)
                        .orderByDesc(DataQualityIssueHistoryEntity::getCreateTime)
                        .last("LIMIT 100"))
                .stream()
                .map(this::toIssueHistoryRecord)
                .toList();
    }

    public List<IssueTrendPoint> issueTrend() {
        if (dataQualityIssueMapper == null) {
            return List.of();
        }
        var rows = dataQualityIssueMapper.selectList(new LambdaQueryWrapper<DataQualityIssueEntity>()
                .ge(DataQualityIssueEntity::getCreateTime, LocalDate.now().minusDays(13).atStartOfDay())
                .orderByAsc(DataQualityIssueEntity::getCreateTime));
        var grouped = rows.stream().collect(Collectors.groupingBy(
                row -> row.getCreateTime().toLocalDate(),
                Collectors.groupingBy(DataQualityIssueEntity::getStatus, Collectors.counting())
        ));
        var points = new ArrayList<IssueTrendPoint>();
        for (var i = 13; i >= 0; i--) {
            var day = LocalDate.now().minusDays(i);
            var counts = grouped.getOrDefault(day, Map.of());
            points.add(new IssueTrendPoint(
                    day,
                    counts.getOrDefault("open", 0L),
                    counts.getOrDefault("processing", 0L),
                    counts.getOrDefault("resolved", 0L),
                    counts.getOrDefault("ignored", 0L)
            ));
        }
        return points;
    }

    private List<DataQualityRule> activeRules() {
        return rules().stream().filter(DataQualityRule::enabled).toList();
    }

    private void syncIssue(DataQualityCheckEntity row, LocalDateTime now) {
        if (dataQualityIssueMapper == null) {
            return;
        }
        var openIssue = dataQualityIssueMapper.selectOne(new LambdaQueryWrapper<DataQualityIssueEntity>()
                .eq(DataQualityIssueEntity::getCheckCode, row.getCheckCode())
                .eq(row.getMetricCode() == null, DataQualityIssueEntity::getMetricCode, "")
                .eq(row.getMetricCode() != null, DataQualityIssueEntity::getMetricCode, row.getMetricCode())
                .in(DataQualityIssueEntity::getStatus, List.of("open", "processing"))
                .last("LIMIT 1"));
        if ("passed".equals(row.getStatus())) {
            if (openIssue != null) {
                openIssue.setStatus("resolved");
                openIssue.setLatestCheckId(row.getId());
                openIssue.setMessage(row.getMessage());
                openIssue.setResolvedBy("system");
                openIssue.setResolutionNote("检查已恢复通过");
                openIssue.setResolvedTime(now);
                openIssue.setLastSeenTime(now);
                openIssue.setUpdateTime(now);
                dataQualityIssueMapper.updateById(openIssue);
                writeIssueHistory(openIssue.getId(), "open", "resolved", "system", "检查已恢复通过");
            }
            return;
        }
        if (openIssue == null) {
            var issue = new DataQualityIssueEntity();
            issue.setCheckCode(row.getCheckCode());
            issue.setMetricCode(row.getMetricCode() == null ? "" : row.getMetricCode());
            issue.setTitle(row.getCheckName() + (row.getMetricCode() == null ? "" : " - " + row.getMetricCode()));
            issue.setSeverity(row.getSeverity());
            issue.setStatus("open");
            issue.setLatestCheckId(row.getId());
            issue.setMessage(row.getMessage());
            issue.setFirstSeenTime(now);
            issue.setLastSeenTime(now);
            issue.setCreateTime(now);
            issue.setUpdateTime(now);
            dataQualityIssueMapper.insert(issue);
            writeIssueHistory(issue.getId(), null, "open", "system", row.getMessage());
        } else {
            openIssue.setSeverity(row.getSeverity());
            openIssue.setLatestCheckId(row.getId());
            openIssue.setMessage(row.getMessage());
            openIssue.setLastSeenTime(now);
            openIssue.setUpdateTime(now);
            dataQualityIssueMapper.updateById(openIssue);
        }
    }

    private void writeIssueHistory(Long issueId, String fromStatus, String toStatus, String operator, String note) {
        if (dataQualityIssueHistoryMapper == null || issueId == null) {
            return;
        }
        var history = new DataQualityIssueHistoryEntity();
        history.setIssueId(issueId);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setOperator(blank(operator) ? "system" : operator);
        history.setNote(note);
        history.setCreateTime(LocalDateTime.now());
        dataQualityIssueHistoryMapper.insert(history);
    }

    private DataQualityCheckEntity row(
            String checkCode,
            String checkName,
            String metricCode,
            String status,
            String severity,
            String expectedValue,
            String actualValue,
            String message,
            LocalDateTime checkTime
    ) {
        var entity = new DataQualityCheckEntity();
        entity.setCheckCode(checkCode);
        entity.setCheckName(checkName);
        entity.setMetricCode(metricCode);
        entity.setStatus(status);
        entity.setSeverity(severity);
        entity.setExpectedValue(expectedValue);
        entity.setActualValue(actualValue);
        entity.setMessage(message);
        entity.setCheckTime(checkTime);
        entity.setCreateTime(LocalDateTime.now());
        return entity;
    }

    private void applyRule(DataQualityRuleEntity entity, DataQualityRuleRequest request) {
        entity.setRuleCode(request.ruleCode().trim());
        entity.setRuleName(request.ruleName().trim());
        entity.setCheckType(request.checkType().trim());
        entity.setMetricCode(blank(request.metricCode()) ? null : request.metricCode().trim());
        entity.setThresholdValue(blank(request.thresholdValue()) ? null : request.thresholdValue().trim());
        entity.setSeverity(request.severity().trim());
        entity.setDescription(request.description());
    }

    private DataQualityRuleEntity requireRule(Long id) {
        var entity = dataQualityRuleMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("质量规则不存在");
        }
        return entity;
    }

    private void ensureRuleMapper() {
        if (dataQualityRuleMapper == null) {
            throw new IllegalArgumentException("当前未启用质量规则数据库");
        }
    }

    private void validateRule(DataQualityRuleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (blank(request.ruleCode()) || !request.ruleCode().trim().matches("[a-z][a-z0-9_]{2,63}")) {
            throw new IllegalArgumentException("规则编码需为小写字母、数字或下划线，且以字母开头");
        }
        if (blank(request.ruleName()) || blank(request.checkType()) || blank(request.severity())) {
            throw new IllegalArgumentException("规则名称、检查类型、等级不能为空");
        }
        if (!List.of("snapshot_missing", "snapshot_freshness", "negative_value", "daily_snapshot_presence").contains(request.checkType())) {
            throw new IllegalArgumentException("不支持的检查类型");
        }
        if (!List.of("info", "warning", "error").contains(request.severity())) {
            throw new IllegalArgumentException("不支持的质量等级");
        }
    }

    private String normalizeStatus(String status) {
        if (!List.of("open", "processing", "resolved", "ignored").contains(status)) {
            throw new IllegalArgumentException("不支持的问题状态");
        }
        return status;
    }

    private long parseLong(String value, long defaultValue) {
        if (blank(value)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private DataQualityCheck toRecord(DataQualityCheckEntity entity) {
        return new DataQualityCheck(
                entity.getId(),
                entity.getCheckCode(),
                entity.getCheckName(),
                entity.getMetricCode(),
                entity.getStatus(),
                entity.getSeverity(),
                entity.getExpectedValue(),
                entity.getActualValue(),
                entity.getMessage(),
                entity.getCheckTime(),
                entity.getCreateTime()
        );
    }

    private DataQualityRule toRuleRecord(DataQualityRuleEntity entity) {
        return new DataQualityRule(
                entity.getId(),
                entity.getRuleCode(),
                entity.getRuleName(),
                entity.getCheckType(),
                entity.getMetricCode(),
                entity.getThresholdValue(),
                entity.getSeverity(),
                Boolean.TRUE.equals(entity.getEnabled()),
                entity.getDescription()
        );
    }

    private DataQualityIssue toIssueRecord(DataQualityIssueEntity entity) {
        return new DataQualityIssue(
                entity.getId(),
                entity.getCheckCode(),
                entity.getMetricCode(),
                entity.getTitle(),
                entity.getSeverity(),
                entity.getStatus(),
                entity.getLatestCheckId(),
                entity.getMessage(),
                entity.getOwner(),
                entity.getResolvedBy(),
                entity.getResolutionNote(),
                entity.getFirstSeenTime(),
                entity.getLastSeenTime(),
                entity.getResolvedTime()
        );
    }

    private DataQualityIssueHistory toIssueHistoryRecord(DataQualityIssueHistoryEntity entity) {
        return new DataQualityIssueHistory(
                entity.getId(),
                entity.getIssueId(),
                entity.getFromStatus(),
                entity.getToStatus(),
                entity.getOperator(),
                entity.getNote(),
                entity.getCreateTime()
        );
    }

    private List<DataQualityRule> fallbackRules() {
        return List.of(
                new DataQualityRule(null, "snapshot_missing_default", "快照缺失检查", "snapshot_missing", null, null, "error", true, "检查启用指标是否存在最新快照"),
                new DataQualityRule(null, "snapshot_freshness_120m", "快照新鲜度检查", "snapshot_freshness", null, "120", "warning", true, "最新快照时间不能超过 120 分钟"),
                new DataQualityRule(null, "negative_value_default", "负值检查", "negative_value", null, "0", "error", true, "指标值不能为负数"),
                new DataQualityRule(null, "daily_snapshot_presence", "今日日快照存在性", "daily_snapshot_presence", null, null, "warning", true, "检查当天是否已生成日快照")
        );
    }

    public record DataQualityCheck(
            Long id,
            String checkCode,
            String checkName,
            String metricCode,
            String status,
            String severity,
            String expectedValue,
            String actualValue,
            String message,
            LocalDateTime checkTime,
            LocalDateTime createTime
    ) {
    }

    public record DataQualityRule(
            Long id,
            String ruleCode,
            String ruleName,
            String checkType,
            String metricCode,
            String thresholdValue,
            String severity,
            Boolean enabled,
            String description
    ) {
    }

    public record DataQualityIssue(
            Long id,
            String checkCode,
            String metricCode,
            String title,
            String severity,
            String status,
            Long latestCheckId,
            String message,
            String owner,
            String resolvedBy,
            String resolutionNote,
            LocalDateTime firstSeenTime,
            LocalDateTime lastSeenTime,
            LocalDateTime resolvedTime
    ) {
    }

    public record DataQualityIssueHistory(
            Long id,
            Long issueId,
            String fromStatus,
            String toStatus,
            String operator,
            String note,
            LocalDateTime createTime
    ) {
    }

    public record IssueTrendPoint(
            LocalDate date,
            long open,
            long processing,
            long resolved,
            long ignored
    ) {
    }

    public record DataQualityRuleRequest(
            String ruleCode,
            String ruleName,
            String checkType,
            String metricCode,
            String thresholdValue,
            String severity,
            String description,
            Boolean enabled
    ) {
    }

    public record IssueStatusRequest(
            String status,
            String owner,
            String resolvedBy,
            String resolutionNote
    ) {
    }

    public record QualitySummary(
            long total,
            long passed,
            long failed,
            long warning,
            LocalDateTime latestCheckTime
    ) {
    }
}
