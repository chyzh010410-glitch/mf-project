CREATE TABLE IF NOT EXISTS dc_source_table_contract (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_name VARCHAR(64) NOT NULL,
    schema_name VARCHAR(128) NOT NULL,
    table_name VARCHAR(128) NOT NULL,
    business_name VARCHAR(128) NOT NULL,
    description VARCHAR(500),
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dc_source_table_contract (source_name, schema_name, table_name),
    KEY idx_dc_source_table_enabled (enabled)
);

CREATE TABLE IF NOT EXISTS dc_source_field_contract (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    table_contract_id BIGINT NOT NULL,
    field_name VARCHAR(128) NOT NULL,
    field_type VARCHAR(128),
    required TINYINT(1) NOT NULL DEFAULT 1,
    description VARCHAR(500),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dc_source_field_contract (table_contract_id, field_name),
    KEY idx_dc_source_field_table (table_contract_id)
);

INSERT INTO dc_source_table_contract
(source_name, schema_name, table_name, business_name, description, enabled)
VALUES
('MF_EP', 'fertilizer', 'user', '用户表', '运营总览用户总数来源', 1),
('MF_EP', 'fertilizer', 'product', '商品表', '运营总览商品指标和分类销售额来源', 1),
('MF_EP', 'fertilizer', 'product_category', '商品分类表', '分类销售额维度来源', 1),
('MF_EP', 'fertilizer', 'merchant', '商家表', '运营总览商家指标来源', 1),
('MF_EP', 'fertilizer', 'order', '订单表', '订单数和 GMV 来源', 1),
('MF_EP', 'fertilizer', 'order_item', '订单明细表', '分类销售额事实来源', 1)
ON DUPLICATE KEY UPDATE
    business_name = VALUES(business_name),
    description = VALUES(description),
    enabled = VALUES(enabled);

INSERT INTO dc_source_field_contract
(table_contract_id, field_name, field_type, required, description)
SELECT t.id, f.field_name, f.field_type, f.required, f.description
FROM dc_source_table_contract t
JOIN (
    SELECT 'user' AS table_name, 'id' AS field_name, 'BIGINT' AS field_type, 1 AS required, '用户主键' AS description
    UNION ALL SELECT 'user', 'deleted', 'TINYINT', 1, '逻辑删除标记'
    UNION ALL SELECT 'product', 'id', 'BIGINT', 1, '商品主键'
    UNION ALL SELECT 'product', 'category_id', 'BIGINT', 1, '商品分类'
    UNION ALL SELECT 'product', 'status', 'INT', 1, '商品状态'
    UNION ALL SELECT 'product', 'deleted', 'TINYINT', 1, '逻辑删除标记'
    UNION ALL SELECT 'product_category', 'id', 'BIGINT', 1, '分类主键'
    UNION ALL SELECT 'product_category', 'name', 'VARCHAR', 1, '分类名称'
    UNION ALL SELECT 'product_category', 'deleted', 'TINYINT', 1, '逻辑删除标记'
    UNION ALL SELECT 'merchant', 'id', 'BIGINT', 1, '商家主键'
    UNION ALL SELECT 'merchant', 'status', 'VARCHAR', 1, '商家状态'
    UNION ALL SELECT 'merchant', 'deleted', 'TINYINT', 1, '逻辑删除标记'
    UNION ALL SELECT 'order', 'id', 'BIGINT', 1, '订单主键'
    UNION ALL SELECT 'order', 'pay_amount', 'DECIMAL', 1, '支付金额'
    UNION ALL SELECT 'order', 'status', 'VARCHAR', 1, '订单状态'
    UNION ALL SELECT 'order', 'deleted', 'TINYINT', 1, '逻辑删除标记'
    UNION ALL SELECT 'order', 'create_time', 'DATETIME', 1, '创建时间'
    UNION ALL SELECT 'order_item', 'id', 'BIGINT', 1, '订单明细主键'
    UNION ALL SELECT 'order_item', 'order_id', 'BIGINT', 1, '订单主键'
    UNION ALL SELECT 'order_item', 'product_id', 'BIGINT', 1, '商品主键'
    UNION ALL SELECT 'order_item', 'total_price', 'DECIMAL', 1, '明细金额'
    UNION ALL SELECT 'order_item', 'deleted', 'TINYINT', 1, '逻辑删除标记'
) f ON f.table_name = t.table_name
WHERE t.source_name = 'MF_EP' AND t.schema_name = 'fertilizer'
ON DUPLICATE KEY UPDATE
    field_type = VALUES(field_type),
    required = VALUES(required),
    description = VALUES(description);
