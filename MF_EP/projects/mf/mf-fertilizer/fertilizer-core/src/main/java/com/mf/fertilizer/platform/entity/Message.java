package com.mf.fertilizer.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mf.fertilizer.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("message")
public class Message extends BaseEntity {
    private Long userId;
    private String title;
    private String content;
    private String type;
    private String targetType;
    private Long targetId;
    private Integer isRead;
    private LocalDateTime readTime;
    private String pushChannel;
}
