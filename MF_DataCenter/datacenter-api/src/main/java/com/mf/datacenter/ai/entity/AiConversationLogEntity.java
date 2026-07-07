package com.mf.datacenter.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dc_ai_conversation_log")
public class AiConversationLogEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String source;
    private String sessionId;
    private String userId;
    private String userType;
    private String question;
    private String answer;
    private String intent;
    private Boolean resolved;
    private Integer satisfaction;
    private LocalDateTime createTime;
}
