package com.mf.datacenter.metric.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("dc_metric_snapshot")
public class MetricSnapshotEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String metricCode;
    private String metricName;
    private BigDecimal metricValue;
    private String dimensionKey;
    private String dimensionValue;
    private String snapshotGranularity;
    private LocalDate snapshotDate;
    private LocalDateTime snapshotTime;
    private LocalDateTime createTime;
}
