package com.mf.datacenter.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dc_ai_tool_call_log")
public class AiToolCallLogEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private String toolName;
    private String requestSummary;
    private String responseSummary;
    private Boolean success;
    private String errorMessage;
    private Long durationMs;
    private LocalDateTime createTime;
}
