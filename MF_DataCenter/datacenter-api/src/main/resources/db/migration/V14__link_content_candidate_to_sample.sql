ALTER TABLE dc_ai_content_candidate
    ADD COLUMN source_sample_id BIGINT NULL,
    ADD UNIQUE KEY uk_dc_ai_content_candidate_source_sample (source_sample_id);
