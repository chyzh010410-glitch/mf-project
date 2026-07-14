package com.mf.fertilizer.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AiContentRollbackDTO {
    @NotNull private Integer targetVersion;
    private String operator;
    private String remark;
}
