package com.mf.datacenter.quality.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dc_quality_issue_history")
public class DataQualityIssueHistoryEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long issueId;
    private String fromStatus;
    private String toStatus;
    private String operator;
    private String note;
    private LocalDateTime createTime;
}
