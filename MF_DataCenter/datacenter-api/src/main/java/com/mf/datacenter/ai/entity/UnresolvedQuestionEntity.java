package com.mf.datacenter.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dc_unresolved_question")
public class UnresolvedQuestionEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private String question;
    private String reason;
    private String status;
    private String priority;
    private LocalDateTime dueTime;
    private String owner;
    private String remark;
    private String knowledgeAction;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
