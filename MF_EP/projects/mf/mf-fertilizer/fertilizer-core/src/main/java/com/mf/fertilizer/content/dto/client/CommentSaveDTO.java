package com.mf.fertilizer.content.dto.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentSaveDTO {
    @NotBlank private String targetType;
    @NotNull private Long targetId;
    @NotBlank private String content;
    private Long parentId;
}
