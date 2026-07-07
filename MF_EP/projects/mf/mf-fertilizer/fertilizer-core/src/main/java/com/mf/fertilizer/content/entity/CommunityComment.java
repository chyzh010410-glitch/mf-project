package com.mf.fertilizer.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mf.fertilizer.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("community_comment")
public class CommunityComment extends BaseEntity {
    private Long userId;
    private String targetType;
    private Long targetId;
    private Long parentId;
    private Long replyToUserId;
    private String content;
    private Integer isDeletedByAdmin;
    private String ip;
    private Integer likeCount;
}
