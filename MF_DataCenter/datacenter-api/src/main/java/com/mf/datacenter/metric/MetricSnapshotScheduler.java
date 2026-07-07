package com.mf.datacenter.metric;

import com.mf.datacenter.quality.DataQualityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MetricSnapshotScheduler {

    private static final Logger log = LoggerFactory.getLogger(MetricSnapshotScheduler.class);

    private final boolean enabled;
    private final MetricSnapshotService metricSnapshotService;
    private final DataQualityService dataQualityService;

    public MetricSnapshotScheduler(
            @Value("${datacenter.scheduler.enabled:false}") boolean enabled,
            MetricSnapshotService metricSnapshotService,
            DataQualityService dataQualityService
    ) {
        this.enabled = enabled;
        this.metricSnapshotService = metricSnapshotService;
        this.dataQualityService = dataQualityService;
    }

    @Scheduled(cron = "0 5 * * * *")
    public void refreshHourly() {
        if (!enabled || !metricSnapshotService.available()) {
            return;
        }
        var rows = metricSnapshotService.refreshHourly();
        log.info("refreshed hourly metric snapshots: {}", rows.size());
        dataQualityService.runChecks();
    }

    @Scheduled(cron = "0 10 0 * * *")
    public void refreshDaily() {
        if (!enabled || !metricSnapshotService.available()) {
            return;
        }
        var rows = metricSnapshotService.refreshDaily();
        log.info("refreshed daily metric snapshots: {}", rows.size());
        dataQualityService.runChecks();
    }
}
