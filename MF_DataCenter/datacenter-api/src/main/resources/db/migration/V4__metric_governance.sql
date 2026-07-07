CREATE TABLE IF NOT EXISTS dc_metric_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    metric_code VARCHAR(128) NOT NULL,
    metric_name VARCHAR(128) NOT NULL,
    source_table VARCHAR(128) NOT NULL,
    formula VARCHAR(500) NOT NULL,
    period VARCHAR(64) NOT NULL,
    owner VARCHAR(64) NOT NULL,
    description VARCHAR(500),
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dc_metric_definition_code (metric_code),
    KEY idx_dc_metric_definition_enabled (enabled),
    KEY idx_dc_metric_definition_owner (owner)
);

CREATE TABLE IF NOT EXISTS dc_data_quality_check (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    check_code VARCHAR(128) NOT NULL,
    check_name VARCHAR(128) NOT NULL,
    metric_code VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    expected_value VARCHAR(128),
    actual_value VARCHAR(128),
    message VARCHAR(500),
    check_time DATETIME NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_dc_quality_status_time (status, check_time),
    KEY idx_dc_quality_metric_time (metric_code, check_time)
);

INSERT INTO dc_metric_definition
(metric_code, metric_name, source_table, formula, period, owner, description, enabled)
VALUES
('user_total', '用户总数', 'MF_EP.user', 'count(*) where deleted = 0', 'hourly,daily', 'platform-ops', '消费者用户总量', 1),
('product_total', '商品总数', 'MF_EP.product', 'count(*) where deleted = 0', 'hourly,daily', 'product-ops', '商品 SPU 总量', 1),
('order_total', '订单数', 'MF_EP.order', 'count(*) where deleted = 0', 'hourly,daily', 'platform-ops', '订单主表总量', 1),
('gmv_total', 'GMV', 'MF_EP.order', 'sum(pay_amount) for paid order statuses', 'hourly,daily', 'platform-ops', '已支付链路订单金额', 1),
('merchant_total', '商家总数', 'MF_EP.merchant', 'count(*) where deleted = 0', 'hourly,daily', 'merchant-ops', '入驻商家主体数量', 1),
('category_sales', '分类销售额', 'MF_EP.order_item/product/product_category', 'sum(order_item.total_price) group by category', 'hourly,daily', 'product-ops', '分类维度销售额', 1),
('ai_conversation_total', 'AI 咨询次数', 'dc_ai_conversation_log', 'count(*)', 'hourly,daily', 'ai-ops', 'AI 咨询会话累计量', 1),
('ai_unresolved_total', 'AI 未解决问题数', 'dc_unresolved_question', 'count(*) where status in pending, processing', 'hourly,daily', 'ai-ops', 'AI 待处理知识缺口数量', 1)
ON DUPLICATE KEY UPDATE
    metric_name = VALUES(metric_name),
    source_table = VALUES(source_table),
    formula = VALUES(formula),
    period = VALUES(period),
    owner = VALUES(owner),
    description = VALUES(description),
    enabled = VALUES(enabled);
