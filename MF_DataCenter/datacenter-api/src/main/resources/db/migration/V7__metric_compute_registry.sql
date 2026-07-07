CREATE TABLE IF NOT EXISTS dc_metric_compute_registry (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    metric_code VARCHAR(128) NOT NULL,
    compute_handler VARCHAR(128) NOT NULL,
    source_name VARCHAR(64) NOT NULL,
    source_contract VARCHAR(256),
    compute_mode VARCHAR(64) NOT NULL,
    formula_text VARCHAR(1000) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    description VARCHAR(500),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dc_metric_compute_code (metric_code),
    KEY idx_dc_metric_compute_enabled (enabled),
    KEY idx_dc_metric_compute_source (source_name)
);

INSERT INTO dc_metric_compute_registry
(metric_code, compute_handler, source_name, source_contract, compute_mode, formula_text, enabled, description)
VALUES
('user_total', 'mf_ep_user_total', 'MF_EP', 'fertilizer.user', 'builtin', 'count(user.id) where deleted = 0', 1, '用户总数受控计算'),
('product_total', 'mf_ep_product_total', 'MF_EP', 'fertilizer.product', 'builtin', 'count(product.id) where deleted = 0', 1, '商品总数受控计算'),
('order_total', 'mf_ep_order_total', 'MF_EP', 'fertilizer.order', 'builtin', 'count(order.id) where deleted = 0', 1, '订单数受控计算'),
('gmv_total', 'mf_ep_gmv_total', 'MF_EP', 'fertilizer.order', 'builtin', 'sum(order.pay_amount) for paid order statuses', 1, 'GMV 受控计算'),
('merchant_total', 'mf_ep_merchant_total', 'MF_EP', 'fertilizer.merchant', 'builtin', 'count(merchant.id) where deleted = 0', 1, '商家总数受控计算'),
('category_sales', 'mf_ep_category_sales', 'MF_EP', 'fertilizer.order_item/product/product_category', 'builtin-dimension', 'sum(order_item.total_price) group by product_category.name', 1, '分类销售额受控计算'),
('ai_conversation_total', 'dc_ai_conversation_total', 'DataCenter', 'dc_ai_conversation_log', 'builtin', 'count(ai_conversation_log.id)', 1, 'AI 咨询次数受控计算'),
('ai_unresolved_total', 'dc_ai_unresolved_total', 'DataCenter', 'dc_unresolved_question', 'builtin', 'count(unresolved_question.id) where status in pending, processing', 1, 'AI 未解决问题数受控计算')
ON DUPLICATE KEY UPDATE
    compute_handler = VALUES(compute_handler),
    source_name = VALUES(source_name),
    source_contract = VALUES(source_contract),
    compute_mode = VALUES(compute_mode),
    formula_text = VALUES(formula_text),
    enabled = VALUES(enabled),
    description = VALUES(description);
