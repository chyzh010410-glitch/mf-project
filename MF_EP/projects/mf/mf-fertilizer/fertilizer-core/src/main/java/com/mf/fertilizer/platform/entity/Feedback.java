package com.mf.fertilizer.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mf.fertilizer.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("feedback")
public class Feedback extends BaseEntity {
    private Long userId;
    private String contact;
    private String content;
    private String images;
    private String type;
    private String status;
    private Long handlerId;
    private String handlerReply;
    private LocalDateTime handleTime;
}
