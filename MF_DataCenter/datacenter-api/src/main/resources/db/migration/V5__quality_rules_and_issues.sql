CREATE TABLE IF NOT EXISTS dc_quality_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_code VARCHAR(128) NOT NULL,
    rule_name VARCHAR(128) NOT NULL,
    check_type VARCHAR(64) NOT NULL,
    metric_code VARCHAR(128),
    threshold_value VARCHAR(128),
    severity VARCHAR(32) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    description VARCHAR(500),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dc_quality_rule_code (rule_code),
    KEY idx_dc_quality_rule_enabled (enabled),
    KEY idx_dc_quality_rule_metric (metric_code)
);

CREATE TABLE IF NOT EXISTS dc_quality_issue (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    check_code VARCHAR(128) NOT NULL,
    metric_code VARCHAR(128),
    title VARCHAR(200) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    latest_check_id BIGINT,
    message VARCHAR(500),
    owner VARCHAR(64),
    resolved_by VARCHAR(64),
    resolution_note VARCHAR(500),
    first_seen_time DATETIME NOT NULL,
    last_seen_time DATETIME NOT NULL,
    resolved_time DATETIME,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_dc_quality_issue_status (status),
    KEY idx_dc_quality_issue_check_metric (check_code, metric_code),
    KEY idx_dc_quality_issue_last_seen (last_seen_time)
);

INSERT INTO dc_quality_rule
(rule_code, rule_name, check_type, metric_code, threshold_value, severity, enabled, description)
VALUES
('snapshot_missing_default', '快照缺失检查', 'snapshot_missing', NULL, NULL, 'error', 1, '检查启用指标是否存在最新快照'),
('snapshot_freshness_120m', '快照新鲜度检查', 'snapshot_freshness', NULL, '120', 'warning', 1, '最新快照时间不能超过 120 分钟'),
('negative_value_default', '负值检查', 'negative_value', NULL, '0', 'error', 1, '指标值不能为负数'),
('daily_snapshot_presence', '今日日快照存在性', 'daily_snapshot_presence', NULL, NULL, 'warning', 1, '检查当天是否已生成日快照')
ON DUPLICATE KEY UPDATE
    rule_name = VALUES(rule_name),
    check_type = VALUES(check_type),
    metric_code = VALUES(metric_code),
    threshold_value = VALUES(threshold_value),
    severity = VALUES(severity),
    enabled = VALUES(enabled),
    description = VALUES(description);

UPDATE dc_metric_definition SET
    metric_name = '用户总数',
    description = '消费者用户总量'
WHERE metric_code = 'user_total';

UPDATE dc_metric_definition SET
    metric_name = '商品总数',
    description = '商品 SPU 总量'
WHERE metric_code = 'product_total';

UPDATE dc_metric_definition SET
    metric_name = '订单数',
    description = '订单主表总量'
WHERE metric_code = 'order_total';

UPDATE dc_metric_definition SET
    metric_name = 'GMV',
    description = '已支付链路订单金额'
WHERE metric_code = 'gmv_total';

UPDATE dc_metric_definition SET
    metric_name = '商家总数',
    description = '入驻商家主体数量'
WHERE metric_code = 'merchant_total';

UPDATE dc_metric_definition SET
    metric_name = '分类销售额',
    description = '分类维度销售额'
WHERE metric_code = 'category_sales';

UPDATE dc_metric_definition SET
    metric_name = 'AI 咨询次数',
    description = 'AI 咨询会话累计量'
WHERE metric_code = 'ai_conversation_total';

UPDATE dc_metric_definition SET
    metric_name = 'AI 未解决问题数',
    description = 'AI 待处理知识缺口数量'
WHERE metric_code = 'ai_unresolved_total';
