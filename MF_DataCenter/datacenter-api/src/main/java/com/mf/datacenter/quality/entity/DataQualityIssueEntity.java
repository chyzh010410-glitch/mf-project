package com.mf.datacenter.quality.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dc_quality_issue")
public class DataQualityIssueEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String checkCode;
    private String metricCode;
    private String title;
    private String severity;
    private String status;
    private Long latestCheckId;
    private String message;
    private String owner;
    private String resolvedBy;
    private String resolutionNote;
    private LocalDateTime firstSeenTime;
    private LocalDateTime lastSeenTime;
    private LocalDateTime resolvedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
