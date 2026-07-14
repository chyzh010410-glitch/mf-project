-- Governance extensions for controlled AI content publishing.

ALTER TABLE ai_content_draft
    ADD COLUMN review_status VARCHAR(32) DEFAULT NULL COMMENT 'pending/approved/rejected/not_required',
    ADD COLUMN reviewed_by VARCHAR(100) DEFAULT NULL COMMENT '人工审核人',
    ADD COLUMN reviewed_at DATETIME DEFAULT NULL COMMENT '人工审核时间',
    ADD COLUMN review_remark VARCHAR(1000) DEFAULT NULL COMMENT '审核意见';

CREATE TABLE IF NOT EXISTS ai_content_draft_version (
    id BIGINT NOT NULL AUTO_INCREMENT,
    draft_id BIGINT NOT NULL,
    version INT NOT NULL,
    action VARCHAR(32) NOT NULL COMMENT 'created/updated/reviewed/published/offline/rolled_back',
    operator VARCHAR(100) DEFAULT NULL,
    remark VARCHAR(1000) DEFAULT NULL,
    content_type VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(1000) DEFAULT NULL,
    content MEDIUMTEXT NOT NULL,
    tags VARCHAR(500) DEFAULT NULL,
    crop VARCHAR(100) DEFAULT NULL,
    tree_age VARCHAR(100) DEFAULT NULL,
    season VARCHAR(50) DEFAULT NULL,
    region VARCHAR(100) DEFAULT NULL,
    risk_level VARCHAR(32) NOT NULL,
    source_references JSON DEFAULT NULL,
    ai_review_json JSON DEFAULT NULL,
    status VARCHAR(32) NOT NULL,
    mf_ep_content_id BIGINT DEFAULT NULL,
    review_status VARCHAR(32) DEFAULT NULL,
    reviewed_by VARCHAR(100) DEFAULT NULL,
    reviewed_at DATETIME DEFAULT NULL,
    review_remark VARCHAR(1000) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_content_draft_version (draft_id, version),
    KEY idx_ai_content_draft_version_draft (draft_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI内容草稿版本快照';

CREATE TABLE IF NOT EXISTS ai_content_sync_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    draft_id BIGINT NOT NULL,
    mf_ep_content_id BIGINT DEFAULT NULL,
    content_type VARCHAR(32) NOT NULL,
    action VARCHAR(32) NOT NULL COMMENT 'publish/offline/rollback',
    version INT NOT NULL,
    delivery_status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT 'pending/acknowledged',
    consumer VARCHAR(100) DEFAULT NULL,
    acknowledged_at DATETIME DEFAULT NULL,
    remark VARCHAR(500) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_ai_content_sync_pending (delivery_status, id),
    KEY idx_ai_content_sync_draft (draft_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI内容同步事件';
