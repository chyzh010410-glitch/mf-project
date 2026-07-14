package com.mf.datacenter.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dc_sample_candidate")
public class SampleCandidateEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private String question;
    private String answer;
    private String source;
    private String qualityStatus;
    private String reviewStatus;
    private String reviewer;
    private String reviewRemark;
    private Boolean recommendedForKnowledge;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
