package com.mf.fertilizer.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mf.fertilizer.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_upload")
public class UserUpload extends BaseEntity {
    private Long userId;
    private String name;
    private String location;
    private String description;
    private String features;
    private String images;
    private String tags;
    private String status;
    private String reviewComment;
    private Long reviewerId;
    private LocalDateTime reviewTime;
    private Long encyclopediaId;
}
