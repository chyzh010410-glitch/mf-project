ALTER TABLE dc_ai_content_sync_log
    ADD COLUMN mf_ep_event_id BIGINT NULL,
    ADD COLUMN delivery_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    ADD KEY idx_dc_ai_content_sync_delivery (delivery_status, update_time);
