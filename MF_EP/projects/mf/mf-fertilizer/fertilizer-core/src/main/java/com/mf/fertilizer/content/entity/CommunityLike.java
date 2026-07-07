package com.mf.fertilizer.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mf.fertilizer.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("community_like")
public class CommunityLike extends BaseEntity {
    private Long userId;
    private String targetType;
    private Long targetId;
}
