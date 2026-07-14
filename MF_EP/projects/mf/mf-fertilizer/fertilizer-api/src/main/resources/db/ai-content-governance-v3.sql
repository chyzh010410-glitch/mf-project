-- Follow-up governance fields for existing V2 installations.

ALTER TABLE ai_content_draft_version
    ADD COLUMN reviewed_at DATETIME DEFAULT NULL COMMENT '人工审核时间' AFTER reviewed_by;
