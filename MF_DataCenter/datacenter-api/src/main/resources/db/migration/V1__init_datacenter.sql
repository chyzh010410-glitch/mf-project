CREATE TABLE IF NOT EXISTS dc_ai_conversation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source VARCHAR(64) NOT NULL,
    session_id VARCHAR(128),
    user_id VARCHAR(64),
    user_type VARCHAR(32),
    question TEXT NOT NULL,
    answer TEXT,
    intent VARCHAR(128),
    resolved TINYINT(1),
    satisfaction INT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS dc_ai_tool_call_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT,
    tool_name VARCHAR(128) NOT NULL,
    request_summary TEXT,
    response_summary TEXT,
    success TINYINT(1),
    error_message TEXT,
    duration_ms BIGINT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS dc_unresolved_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT,
    question TEXT NOT NULL,
    reason VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    owner VARCHAR(64),
    remark TEXT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS dc_sample_candidate (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    source VARCHAR(64),
    quality_status VARCHAR(32),
    review_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    reviewer VARCHAR(64),
    review_remark TEXT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS dc_metric_snapshot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    metric_code VARCHAR(128) NOT NULL,
    metric_name VARCHAR(128) NOT NULL,
    metric_value DECIMAL(18, 2) NOT NULL,
    dimension_key VARCHAR(128),
    dimension_value VARCHAR(128),
    snapshot_date DATE NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
