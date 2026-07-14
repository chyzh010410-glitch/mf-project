package com.mf.datacenter.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dc_ai_history_audit_log")
public class AiHistoryAuditLogEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private String action;
    private String outcome;
    private String actorUserId;
    private String sessionId;
    private String detail;
    private LocalDateTime createTime;
}
