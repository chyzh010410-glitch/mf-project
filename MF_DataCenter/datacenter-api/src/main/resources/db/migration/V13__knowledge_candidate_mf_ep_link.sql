ALTER TABLE dc_ai_content_candidate
    ADD COLUMN mf_ep_draft_id BIGINT NULL,
    ADD COLUMN mf_ep_content_id BIGINT NULL,
    ADD COLUMN last_error VARCHAR(1000) NULL,
    ADD KEY idx_dc_ai_candidate_ep_draft (mf_ep_draft_id);
