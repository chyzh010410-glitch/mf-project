package com.mf.datacenter.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dc_notification")
public class NotificationEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String notificationKey;
    private String title;
    private String content;
    private String severity;
    private String targetPath;
    private Boolean readFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
