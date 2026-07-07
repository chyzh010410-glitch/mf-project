-- Merchant V1 incremental schema.
-- Apply after the base schema when enabling platform-audited merchants.

CREATE TABLE IF NOT EXISTS merchant (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username        VARCHAR(50)  NOT NULL COMMENT '用户名',
    password        VARCHAR(200) NOT NULL COMMENT '密码(MD5, follows current client user style)',
    shop_name       VARCHAR(100) NOT NULL COMMENT '店铺名称',
    contact_name    VARCHAR(50)  NOT NULL COMMENT '联系人',
    phone           VARCHAR(20)  NOT NULL COMMENT '手机号',
    status          VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT '状态: pending/approved/rejected/disabled',
    audit_remark    VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
    audit_time      DATETIME     DEFAULT NULL COMMENT '审核时间',
    last_login_time DATETIME     DEFAULT NULL COMMENT '最后登录时间',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_merchant_username (username),
    UNIQUE KEY uk_merchant_phone (phone),
    KEY idx_merchant_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家表';

ALTER TABLE product
    ADD COLUMN merchant_id BIGINT DEFAULT NULL COMMENT '商家ID，NULL表示平台自营' AFTER product_type,
    ADD KEY idx_product_merchant_id (merchant_id);

ALTER TABLE order_item
    ADD COLUMN merchant_id BIGINT DEFAULT NULL COMMENT '商家ID，NULL表示平台自营订单项' AFTER order_no,
    ADD KEY idx_order_item_merchant_id (merchant_id);
