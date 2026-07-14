ALTER TABLE dc_unresolved_question
    ADD COLUMN priority VARCHAR(16) NOT NULL DEFAULT 'normal' AFTER status,
    ADD COLUMN due_time DATETIME NULL AFTER priority,
    ADD COLUMN knowledge_action VARCHAR(500) NULL AFTER remark;

ALTER TABLE dc_sample_candidate
    ADD COLUMN recommended_for_knowledge TINYINT(1) NOT NULL DEFAULT 0 AFTER review_remark;

CREATE INDEX idx_dc_unresolved_priority_due ON dc_unresolved_question (status, priority, due_time);
CREATE INDEX idx_dc_sample_recommended_review ON dc_sample_candidate (recommended_for_knowledge, review_status);

CREATE TABLE IF NOT EXISTS dc_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    notification_key VARCHAR(128) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content VARCHAR(1000) NULL,
    severity VARCHAR(16) NOT NULL DEFAULT 'warning',
    target_path VARCHAR(255) NULL,
    read_flag TINYINT(1) NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dc_notification_key (notification_key),
    KEY idx_dc_notification_read_time (read_flag, update_time)
);
