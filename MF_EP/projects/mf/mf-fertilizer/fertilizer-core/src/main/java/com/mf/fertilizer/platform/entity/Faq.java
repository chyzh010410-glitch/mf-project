package com.mf.fertilizer.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mf.fertilizer.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("faq")
public class Faq extends BaseEntity {
    private String question;
    private String answer;
    private String category;
    private Integer sortOrder;
    private Integer isPublished;
    private Integer viewCount;
}
