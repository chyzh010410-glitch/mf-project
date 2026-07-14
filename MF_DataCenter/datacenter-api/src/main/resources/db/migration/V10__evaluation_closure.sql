CREATE TABLE IF NOT EXISTS dc_evaluation_suite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(128) NOT NULL, description VARCHAR(1000) NULL, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS dc_evaluation_case (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, suite_id BIGINT NOT NULL, question VARCHAR(2000) NOT NULL, expected_intent VARCHAR(64) NULL,
  expected_tool VARCHAR(128) NULL, expected_safety_result VARCHAR(128) NULL, tags VARCHAR(500) NULL, enabled TINYINT(1) NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, KEY idx_dc_eval_case_suite_enabled (suite_id, enabled)
);
CREATE TABLE IF NOT EXISTS dc_evaluation_result (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, case_id BIGINT NOT NULL, actual_intent VARCHAR(64) NULL, actual_tools VARCHAR(500) NULL,
  actual_fallback_reason VARCHAR(128) NULL, answer_snapshot TEXT NULL, passed TINYINT(1) NOT NULL, failure_reason VARCHAR(500) NULL,
  executed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, KEY idx_dc_eval_result_case_time (case_id, executed_at)
);
