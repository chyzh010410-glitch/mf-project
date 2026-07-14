package com.mf.fertilizer.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiContentReviewDTO {
    @NotBlank private String decision;
    @NotBlank private String reviewer;
    private String remark;
}
