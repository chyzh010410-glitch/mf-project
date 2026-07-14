CREATE TABLE dc_ai_content_sync_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    candidate_id BIGINT NOT NULL,
    mf_ep_draft_id BIGINT NULL,
    mf_ep_content_id BIGINT NULL,
    action VARCHAR(32) NOT NULL,
    request_id VARCHAR(160) NOT NULL,
    success TINYINT(1) NOT NULL DEFAULT 0,
    reused TINYINT(1) NOT NULL DEFAULT 0,
    indexed_documents INT NOT NULL DEFAULT 0,
    error VARCHAR(1000) NULL,
    completed_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_dc_ai_content_sync_request (request_id),
    KEY idx_dc_ai_content_sync_candidate (candidate_id, create_time)
);
