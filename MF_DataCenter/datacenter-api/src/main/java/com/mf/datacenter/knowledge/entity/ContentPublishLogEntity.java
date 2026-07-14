package com.mf.datacenter.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dc_ai_content_publish_log")
public class ContentPublishLogEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long candidateId;
    private Long mfEpContentId;
    private String action;
    private String operator;
    private String remark;
    private LocalDateTime createTime;
}
