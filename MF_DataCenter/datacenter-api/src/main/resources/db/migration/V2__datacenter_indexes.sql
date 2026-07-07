ALTER TABLE dc_ai_conversation_log ADD INDEX idx_dc_ai_conversation_create_time (create_time);
ALTER TABLE dc_ai_conversation_log ADD INDEX idx_dc_ai_conversation_user_id (user_id);
ALTER TABLE dc_ai_conversation_log ADD INDEX idx_dc_ai_conversation_intent (intent);

ALTER TABLE dc_ai_tool_call_log ADD INDEX idx_dc_ai_tool_call_conversation_id (conversation_id);
ALTER TABLE dc_ai_tool_call_log ADD INDEX idx_dc_ai_tool_call_create_time (create_time);

ALTER TABLE dc_unresolved_question ADD INDEX idx_dc_unresolved_status_update_time (status, update_time);
ALTER TABLE dc_unresolved_question ADD INDEX idx_dc_unresolved_conversation_id (conversation_id);

ALTER TABLE dc_sample_candidate ADD INDEX idx_dc_sample_review_update_time (review_status, update_time);
ALTER TABLE dc_sample_candidate ADD INDEX idx_dc_sample_conversation_id (conversation_id);

ALTER TABLE dc_metric_snapshot ADD INDEX idx_dc_metric_snapshot_code_date (metric_code, snapshot_date);
ALTER TABLE dc_metric_snapshot ADD INDEX idx_dc_metric_snapshot_dimension (dimension_key, dimension_value);
