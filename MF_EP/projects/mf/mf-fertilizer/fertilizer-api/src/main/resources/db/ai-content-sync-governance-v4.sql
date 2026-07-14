-- Persist AgentService failure state and allow controlled manual retry.

ALTER TABLE ai_content_sync_event
    ADD COLUMN failure_attempts INT NOT NULL DEFAULT 0 COMMENT 'AgentService累计失败次数' AFTER acknowledged_at,
    ADD COLUMN last_failure_reason VARCHAR(1000) DEFAULT NULL COMMENT '最近失败原因' AFTER failure_attempts,
    ADD COLUMN last_failed_at DATETIME DEFAULT NULL COMMENT '最近失败时间' AFTER last_failure_reason,
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0 COMMENT '人工重试次数' AFTER last_failed_at,
    ADD COLUMN last_retry_at DATETIME DEFAULT NULL COMMENT '最近人工重试时间' AFTER retry_count;
