package com.mf.datacenter.metric;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mf.datacenter.ai.AiDataStore;
import com.mf.datacenter.dashboard.DashboardController;
import com.mf.datacenter.dashboard.MfEpDashboardReadService;
import com.mf.datacenter.metric.entity.MetricSnapshotEntity;
import com.mf.datacenter.metric.mapper.MetricSnapshotMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MetricSnapshotService {

    private static final List<String> DASHBOARD_CODES = List.of(
            "user_total", "product_total", "order_total", "gmv_total", "merchant_total",
            "ai_conversation_total", "ai_unresolved_total"
    );

    private final boolean mysqlEnabled;
    private final MetricSnapshotMapper snapshotMapper;
    private final MfEpDashboardReadService mfEpDashboardReadService;
    private final AiDataStore aiDataStore;
    private final MetricComputeRegistryService metricComputeRegistryService;

    public MetricSnapshotService(
            @Value("${datacenter.mysql.enabled:false}") boolean mysqlEnabled,
            ObjectProvider<MetricSnapshotMapper> snapshotMapper,
            MfEpDashboardReadService mfEpDashboardReadService,
            AiDataStore aiDataStore,
            MetricComputeRegistryService metricComputeRegistryService
    ) {
        this.mysqlEnabled = mysqlEnabled;
        this.snapshotMapper = snapshotMapper.getIfAvailable();
        this.mfEpDashboardReadService = mfEpDashboardReadService;
        this.aiDataStore = aiDataStore;
        this.metricComputeRegistryService = metricComputeRegistryService;
    }

    public boolean available() {
        return mysqlEnabled && snapshotMapper != null;
    }

    public List<MetricSnapshot> refreshDaily() {
        return refresh("daily");
    }

    public List<MetricSnapshot> refreshHourly() {
        return refresh("hourly");
    }

    public List<MetricSnapshot> refresh(String granularity) {
        requireAvailable();
        var normalizedGranularity = normalizeGranularity(granularity);
        var rows = currentRows(normalizedGranularity, LocalDateTime.now());
        for (var row : rows) {
            snapshotMapper.insert(row);
        }
        return rows.stream().map(this::toRecord).toList();
    }

    public List<MetricSnapshot> latest(String metricCode) {
        if (!available()) {
            return List.of();
        }
        var query = new LambdaQueryWrapper<MetricSnapshotEntity>()
                .eq(hasText(metricCode), MetricSnapshotEntity::getMetricCode, metricCode)
                .orderByDesc(MetricSnapshotEntity::getSnapshotTime)
                .orderByDesc(MetricSnapshotEntity::getCreateTime)
                .last("LIMIT 100");
        return snapshotMapper.selectList(query).stream().map(this::toRecord).toList();
    }

    public List<MetricSnapshot> query(String metricCode, String granularity, LocalDate startDate, LocalDate endDate) {
        return query(List.of(), metricCode, granularity, null, null, startDate, endDate, 500);
    }

    public List<MetricSnapshot> query(
            List<String> metricCodes,
            String metricCode,
            String granularity,
            String dimensionKey,
            String dimensionValue,
            LocalDate startDate,
            LocalDate endDate,
            Integer limit
    ) {
        if (!available()) {
            return List.of();
        }
        var codes = normalizeCodes(metricCodes, metricCode);
        var boundedLimit = limit == null ? 500 : Math.max(1, Math.min(limit, 1000));
        var query = new LambdaQueryWrapper<MetricSnapshotEntity>()
                .in(!codes.isEmpty(), MetricSnapshotEntity::getMetricCode, codes)
                .eq(hasText(granularity), MetricSnapshotEntity::getSnapshotGranularity, granularity)
                .eq(hasText(dimensionKey), MetricSnapshotEntity::getDimensionKey, dimensionKey)
                .eq(hasText(dimensionValue), MetricSnapshotEntity::getDimensionValue, dimensionValue)
                .ge(startDate != null, MetricSnapshotEntity::getSnapshotDate, startDate)
                .le(endDate != null, MetricSnapshotEntity::getSnapshotDate, endDate)
                .orderByAsc(MetricSnapshotEntity::getSnapshotTime)
                .orderByAsc(MetricSnapshotEntity::getMetricCode)
                .last("LIMIT " + boundedLimit);
        return snapshotMapper.selectList(query).stream().map(this::toRecord).toList();
    }

    public Map<String, MetricSnapshot> latestDashboardMetrics() {
        if (!available()) {
            return Map.of();
        }
        var rows = snapshotMapper.selectList(new LambdaQueryWrapper<MetricSnapshotEntity>()
                .in(MetricSnapshotEntity::getMetricCode, DASHBOARD_CODES)
                .eq(MetricSnapshotEntity::getDimensionKey, "global")
                .eq(MetricSnapshotEntity::getDimensionValue, "all")
                .orderByDesc(MetricSnapshotEntity::getSnapshotTime)
                .orderByDesc(MetricSnapshotEntity::getCreateTime)
                .last("LIMIT 100"));
        var latest = new LinkedHashMap<String, MetricSnapshot>();
        for (var row : rows) {
            latest.putIfAbsent(row.getMetricCode(), toRecord(row));
        }
        return latest;
    }

    public List<DashboardController.TrendPoint> dailyTrend(String metricCode) {
        if (!available()) {
            return List.of();
        }
        var rows = snapshotMapper.selectList(new LambdaQueryWrapper<MetricSnapshotEntity>()
                .eq(MetricSnapshotEntity::getMetricCode, metricCode)
                .eq(MetricSnapshotEntity::getSnapshotGranularity, "daily")
                .ge(MetricSnapshotEntity::getSnapshotDate, LocalDate.now().minusDays(6))
                .orderByAsc(MetricSnapshotEntity::getSnapshotDate)
                .orderByAsc(MetricSnapshotEntity::getCreateTime));
        var byDate = new LinkedHashMap<LocalDate, BigDecimal>();
        for (var row : rows) {
            byDate.put(row.getSnapshotDate(), row.getMetricValue());
        }
        var points = new ArrayList<DashboardController.TrendPoint>();
        for (var i = 6; i >= 0; i--) {
            var day = LocalDate.now().minusDays(i);
            points.add(new DashboardController.TrendPoint(day.toString().substring(5), byDate.getOrDefault(day, BigDecimal.ZERO)));
        }
        return points;
    }

    public List<DashboardController.CategoryShare> latestCategorySales() {
        if (!available()) {
            return List.of();
        }
        var rows = snapshotMapper.selectList(new LambdaQueryWrapper<MetricSnapshotEntity>()
                .eq(MetricSnapshotEntity::getMetricCode, "category_sales")
                .eq(MetricSnapshotEntity::getDimensionKey, "category")
                .orderByDesc(MetricSnapshotEntity::getSnapshotTime)
                .orderByDesc(MetricSnapshotEntity::getMetricValue)
                .last("LIMIT 20"));
        if (rows.isEmpty()) {
            return List.of();
        }
        var latestTime = rows.get(0).getSnapshotTime();
        return rows.stream()
                .filter(row -> latestTime.equals(row.getSnapshotTime()))
                .map(row -> new DashboardController.CategoryShare(row.getDimensionValue(), row.getMetricValue()))
                .toList();
    }

    private List<MetricSnapshotEntity> currentRows(String granularity, LocalDateTime snapshotTime) {
        var rows = new ArrayList<MetricSnapshotEntity>();
        var aiStats = aiDataStore.stats();
        var activeCodes = metricComputeRegistryService.activeMetricCodes();
        if (mfEpDashboardReadService.enabled()) {
            var facts = mfEpDashboardReadService.readFacts();
            if (activeCodes.contains("user_total")) {
                rows.add(row("user_total", "用户总数", BigDecimal.valueOf(facts.userTotal()), granularity, snapshotTime));
            }
            if (activeCodes.contains("product_total")) {
                rows.add(row("product_total", "商品总数", BigDecimal.valueOf(facts.productTotal()), granularity, snapshotTime));
            }
            if (activeCodes.contains("order_total")) {
                rows.add(row("order_total", "订单数", BigDecimal.valueOf(facts.orderTotal()), granularity, snapshotTime));
            }
            if (activeCodes.contains("gmv_total")) {
                rows.add(row("gmv_total", "GMV", facts.gmvTotal(), granularity, snapshotTime));
            }
            if (activeCodes.contains("merchant_total")) {
                rows.add(row("merchant_total", "商家总数", BigDecimal.valueOf(facts.merchantTotal()), granularity, snapshotTime));
            }
            if (activeCodes.contains("category_sales")) {
                for (var category : facts.categorySales()) {
                    rows.add(row("category_sales", "分类销售额", category.value(), "category", category.name(), granularity, snapshotTime));
                }
            }
        }
        if (activeCodes.contains("ai_conversation_total")) {
            rows.add(row("ai_conversation_total", "AI 咨询次数", BigDecimal.valueOf(aiStats.conversationTotal()), granularity, snapshotTime));
        }
        if (activeCodes.contains("ai_unresolved_total")) {
            rows.add(row("ai_unresolved_total", "AI 未解决问题数", BigDecimal.valueOf(aiStats.unresolvedTotal()), granularity, snapshotTime));
        }
        return rows;
    }

    private MetricSnapshotEntity row(String code, String name, BigDecimal value, String granularity, LocalDateTime snapshotTime) {
        return row(code, name, value, "global", "all", granularity, snapshotTime);
    }

    private MetricSnapshotEntity row(
            String code,
            String name,
            BigDecimal value,
            String dimensionKey,
            String dimensionValue,
            String granularity,
            LocalDateTime snapshotTime
    ) {
        var entity = new MetricSnapshotEntity();
        entity.setMetricCode(code);
        entity.setMetricName(name);
        entity.setMetricValue(value);
        entity.setDimensionKey(dimensionKey);
        entity.setDimensionValue(dimensionValue);
        entity.setSnapshotGranularity(granularity);
        entity.setSnapshotDate(snapshotTime.toLocalDate());
        entity.setSnapshotTime(snapshotTime);
        entity.setCreateTime(LocalDateTime.now());
        return entity;
    }

    private String normalizeGranularity(String granularity) {
        if ("hourly".equals(granularity)) {
            return "hourly";
        }
        return "daily";
    }

    private void requireAvailable() {
        if (!available()) {
            throw new IllegalStateException("metric snapshot requires datacenter MySQL storage");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private List<String> normalizeCodes(List<String> metricCodes, String metricCode) {
        var values = new ArrayList<String>();
        if (metricCodes != null) {
            for (var item : metricCodes) {
                addCodes(values, item);
            }
        }
        addCodes(values, metricCode);
        return values.stream().distinct().toList();
    }

    private void addCodes(List<String> values, String rawValue) {
        if (!hasText(rawValue)) {
            return;
        }
        for (var item : rawValue.split(",")) {
            var code = item.trim();
            if (!code.isEmpty()) {
                values.add(code);
            }
        }
    }

    private MetricSnapshot toRecord(MetricSnapshotEntity entity) {
        return new MetricSnapshot(
                entity.getId(),
                entity.getMetricCode(),
                entity.getMetricName(),
                entity.getMetricValue(),
                entity.getDimensionKey(),
                entity.getDimensionValue(),
                entity.getSnapshotGranularity(),
                entity.getSnapshotDate(),
                entity.getSnapshotTime(),
                entity.getCreateTime()
        );
    }

    public record MetricSnapshot(
            Long id,
            String metricCode,
            String metricName,
            BigDecimal metricValue,
            String dimensionKey,
            String dimensionValue,
            String snapshotGranularity,
            LocalDate snapshotDate,
            LocalDateTime snapshotTime,
            LocalDateTime createTime
    ) {
    }
}
