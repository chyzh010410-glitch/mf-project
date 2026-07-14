ALTER TABLE dc_ai_conversation_log
    ADD COLUMN user_deleted_at DATETIME NULL AFTER satisfaction,
    ADD INDEX idx_dc_ai_conversation_user_session_time (user_id, session_id, create_time);

CREATE TABLE IF NOT EXISTS dc_ai_history_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    action VARCHAR(32) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    actor_user_id VARCHAR(64) NULL,
    session_id VARCHAR(128) NULL,
    detail VARCHAR(255) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_dc_ai_history_audit_time (create_time),
    KEY idx_dc_ai_history_audit_actor (actor_user_id, create_time)
);
