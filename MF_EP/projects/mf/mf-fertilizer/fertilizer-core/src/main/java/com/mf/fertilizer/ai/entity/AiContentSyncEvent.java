package com.mf.fertilizer.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mf.fertilizer.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_content_sync_event")
public class AiContentSyncEvent extends BaseEntity {
    private Long draftId;
    private Long mfEpContentId;
    private String contentType;
    private String action;
    private Integer version;
    private String deliveryStatus;
    private String consumer;
    private java.time.LocalDateTime acknowledgedAt;
    private Integer failureAttempts;
    private String lastFailureReason;
    private java.time.LocalDateTime lastFailedAt;
    private Integer retryCount;
    private java.time.LocalDateTime lastRetryAt;
    private String remark;
}
