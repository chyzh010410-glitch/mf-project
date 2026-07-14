package com.mf.fertilizer.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mf.fertilizer.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_content_draft_version")
public class AiContentDraftVersion extends BaseEntity {
    private Long draftId;
    private Integer version;
    private String action;
    private String operator;
    private String remark;
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
    private Long mfEpContentId;
    private String reviewStatus;
    private String reviewedBy;
    private String reviewRemark;
}
