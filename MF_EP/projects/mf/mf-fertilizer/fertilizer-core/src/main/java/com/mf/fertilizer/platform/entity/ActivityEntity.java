package com.mf.fertilizer.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mf.fertilizer.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("activity")
public class ActivityEntity extends BaseEntity {
    private String title;
    private String description;
    private String coverImage;
    private String type;
    private String ruleJson;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Integer isBanner;
    private Integer sortOrder;
}
