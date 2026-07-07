ALTER TABLE dc_metric_snapshot
    ADD COLUMN snapshot_granularity VARCHAR(16) NOT NULL DEFAULT 'daily' AFTER dimension_value,
    ADD COLUMN snapshot_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER snapshot_date,
    ADD INDEX idx_dc_metric_snapshot_granularity_time (snapshot_granularity, snapshot_time);
