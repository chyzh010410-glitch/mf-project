package com.mf.fertilizer.platform.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ActivitySaveDTO {
    @NotBlank private String title;
    private String description;
    private String coverImage;
    @NotBlank private String type;
    private String ruleJson;
    @NotNull private LocalDateTime startTime;
    @NotNull private LocalDateTime endTime;
    private Integer isBanner;
    private Integer sortOrder;
}
