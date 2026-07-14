package com.mf.datacenter.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dc_ai_content_sync_log")
public class ContentSyncLogEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long candidateId;
    private Long mfEpDraftId;
    private Long mfEpContentId;
    private Long mfEpEventId;
    private String action;
    private String requestId;
    private Boolean success;
    private Boolean reused;
    private Integer indexedDocuments;
    private String error;
    private String deliveryStatus;
    private LocalDateTime completedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
