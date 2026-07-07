package com.mf.fertilizer.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mf.fertilizer.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("browsing_history")
public class BrowsingHistory extends BaseEntity {
    private Long userId;
    private String targetType;
    private Long targetId;
    private String targetName;
    private String targetImage;
    private Integer stayDuration;
}
