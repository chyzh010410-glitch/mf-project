package com.mf.datacenter.quality.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dc_data_quality_check")
public class DataQualityCheckEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String checkCode;
    private String checkName;
    private String metricCode;
    private String status;
    private String severity;
    private String expectedValue;
    private String actualValue;
    private String message;
    private LocalDateTime checkTime;
    private LocalDateTime createTime;
}
