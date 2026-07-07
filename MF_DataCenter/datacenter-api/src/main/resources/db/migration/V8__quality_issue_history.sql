CREATE TABLE IF NOT EXISTS dc_quality_issue_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    issue_id BIGINT NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    operator VARCHAR(64),
    note VARCHAR(500),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_dc_quality_issue_history_issue (issue_id, create_time),
    KEY idx_dc_quality_issue_history_status (to_status, create_time)
);
