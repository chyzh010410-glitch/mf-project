package com.mf.fertilizer.content.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ArticleSaveDTO {
    @NotBlank private String title;
    private String summary;
    private String coverImage;
    private String images;
    @NotBlank private String content;
    private Long categoryId;
    private String tags;
    private Integer isPublished;
    private Integer isTop;
    private Integer isRecommend;
}
