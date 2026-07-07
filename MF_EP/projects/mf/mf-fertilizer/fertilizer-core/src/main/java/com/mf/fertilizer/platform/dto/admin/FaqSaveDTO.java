package com.mf.fertilizer.platform.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FaqSaveDTO {
    @NotBlank private String question;
    @NotBlank private String answer;
    @NotBlank private String category;
    private Integer sortOrder;
    private Integer isPublished;
}
