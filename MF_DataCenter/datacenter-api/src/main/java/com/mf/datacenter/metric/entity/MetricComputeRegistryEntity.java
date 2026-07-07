package com.mf.datacenter.metric.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dc_metric_compute_registry")
public class MetricComputeRegistryEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String metricCode;
    private String computeHandler;
    private String sourceName;
    private String sourceContract;
    private String computeMode;
    private String formulaText;
    private Boolean enabled;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
