package com.mf.fertilizer.content.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EncyclopediaSaveDTO {
    @NotBlank private String name;
    private String scientificName;
    private String alias;
    private String pinyin;
    private String family;
    private String genus;
    private Long categoryId;
    private String coverImage;
    private String images;
    private String description;
    private String morphology;
    private String distribution;
    private String habitat;
    private String careGuide;
    private String valueDescription;
    private String tags;
    private Integer isPublished;
}
