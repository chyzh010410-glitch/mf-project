package com.mf.datacenter.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mf.datacenter.ai.AiDataStore;
import com.mf.datacenter.metric.MetricSnapshotService;
import com.mf.datacenter.notification.entity.NotificationEntity;
import com.mf.datacenter.notification.mapper.NotificationMapper;
import com.mf.datacenter.quality.DataQualityService;
import com.mf.datacenter.source.SourceContractService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class NotificationService {
    private final NotificationMapper mapper;
    private final SourceContractService sourceContractService;
    private final DataQualityService dataQualityService;
    private final MetricSnapshotService metricSnapshotService;
    private final AiDataStore aiDataStore;

    public NotificationService(ObjectProvider<NotificationMapper> mapper, SourceContractService sourceContractService,
                               DataQualityService dataQualityService, MetricSnapshotService metricSnapshotService, AiDataStore aiDataStore) {
        this.mapper = mapper.getIfAvailable();
        this.sourceContractService = sourceContractService;
        this.dataQualityService = dataQualityService;
        this.metricSnapshotService = metricSnapshotService;
        this.aiDataStore = aiDataStore;
    }

    public synchronized List<Notification> notifications() {
        sync();
        if (mapper == null) return List.of();
        return mapper.selectList(new LambdaQueryWrapper<NotificationEntity>().orderByAsc(NotificationEntity::getReadFlag).orderByDesc(NotificationEntity::getUpdateTime))
                .stream().map(this::toRecord).toList();
    }

    public long unreadCount() {
        sync();
        return mapper == null ? 0 : mapper.selectCount(new LambdaQueryWrapper<NotificationEntity>().eq(NotificationEntity::getReadFlag, false));
    }

    public void markRead(Long id) {
        if (mapper != null) mapper.update(null, new LambdaUpdateWrapper<NotificationEntity>().eq(NotificationEntity::getId, id).set(NotificationEntity::getReadFlag, true).set(NotificationEntity::getUpdateTime, LocalDateTime.now()));
    }

    public synchronized void sync() {
        if (mapper == null) return;
        Set<String> activeKeys = new HashSet<>();
        var source = sourceContractService.check();
        if (!Boolean.TRUE.equals(source.connected()) || source.failedTables() > 0) {
            activeKeys.add("source-contract");
            upsert("source-contract", "源表契约异常", source.message(), "error", "/source-governance");
        }
        for (var issue : dataQualityService.issues("open")) {
            var key = "quality-" + issue.id();
            activeKeys.add(key);
            upsert(key, "数据质量待处理：" + issue.title(), issue.message(), issue.severity(), "/data-quality");
        }
        var latest = metricSnapshotService.latestDashboardMetrics().values().stream().map(MetricSnapshotService.MetricSnapshot::snapshotTime).max(LocalDateTime::compareTo).orElse(null);
        if (latest == null || Duration.between(latest, LocalDateTime.now()).toMinutes() > 120) {
            activeKeys.add("snapshot-freshness");
            upsert("snapshot-freshness", "指标快照需要刷新", latest == null ? "尚未生成指标快照" : "最新快照已超过 120 分钟", "warning", "/dashboard");
        }
        var recentToolCalls = aiDataStore.toolCalls();
        if (recentToolCalls.size() >= 2 && Boolean.FALSE.equals(recentToolCalls.get(0).success()) && Boolean.FALSE.equals(recentToolCalls.get(1).success())) {
            var call = recentToolCalls.get(0);
            var key = "tool-failure-" + call.toolName();
            activeKeys.add(key);
            upsert(key, "Agent 工具连续调用失败：" + call.toolName(), call.errorMessage(), "warning", "/ai-analysis");
        }
        closeResolved(activeKeys);
    }

    private void upsert(String key, String title, String content, String severity, String path) {
        var row = mapper.selectOne(new LambdaQueryWrapper<NotificationEntity>().eq(NotificationEntity::getNotificationKey, key));
        var now = LocalDateTime.now();
        if (row == null) {
            row = new NotificationEntity();
            row.setNotificationKey(key); row.setCreateTime(now); row.setReadFlag(false);
        }
        row.setTitle(title); row.setContent(content); row.setSeverity(severity); row.setTargetPath(path); row.setReadFlag(false); row.setUpdateTime(now);
        if (row.getId() == null) mapper.insert(row); else mapper.updateById(row);
    }

    private void closeResolved(Set<String> activeKeys) {
        mapper.selectList(new LambdaQueryWrapper<NotificationEntity>()).stream()
                .filter(row -> managedKey(row.getNotificationKey()) && !activeKeys.contains(row.getNotificationKey()) && !Boolean.TRUE.equals(row.getReadFlag()))
                .forEach(row -> { row.setReadFlag(true); row.setUpdateTime(LocalDateTime.now()); mapper.updateById(row); });
    }

    private boolean managedKey(String key) { return "source-contract".equals(key) || "snapshot-freshness".equals(key) || key.startsWith("quality-") || key.startsWith("tool-failure-"); }

    private Notification toRecord(NotificationEntity row) {
        return new Notification(row.getId(), row.getTitle(), row.getContent(), row.getSeverity(), row.getTargetPath(), Boolean.TRUE.equals(row.getReadFlag()), row.getUpdateTime());
    }

    public record Notification(Long id, String title, String content, String severity, String targetPath, boolean read, LocalDateTime updateTime) {}
}
