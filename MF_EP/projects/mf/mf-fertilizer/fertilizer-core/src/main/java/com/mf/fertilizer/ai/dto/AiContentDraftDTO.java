package com.mf.fertilizer.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiContentDraftDTO {
    @NotBlank private String contentType;
    @NotBlank private String title;
    private String summary;
    @NotBlank private String content;
    private String tags;
    private String crop;
    private String treeAge;
    private String season;
    private String region;
    private String riskLevel;
    private String sourceReferences;
    private String aiReviewJson;
    private String status;
    private String createdBy;
    private String remark;
}
