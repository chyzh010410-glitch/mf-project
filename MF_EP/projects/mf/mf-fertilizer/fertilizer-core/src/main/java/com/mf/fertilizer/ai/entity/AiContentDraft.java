package com.mf.fertilizer.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mf.fertilizer.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_content_draft")
public class AiContentDraft extends BaseEntity {
    private String contentType;
    private String title;
    private String summary;
    private String content;
    private String tags;
    private String crop;
    private String treeAge;
    private String season;
    private String region;
    private String riskLevel;
    private String sourceReferences;
    private String aiReviewJson;
    private String status;
    private Integer version;
    private Long mfEpContentId;
    private String createdBy;
    private String publishedBy;
    private LocalDateTime publishedAt;
    private String reviewStatus;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewRemark;
    private String remark;
}
