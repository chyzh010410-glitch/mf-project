package com.mf.fertilizer.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mf.fertilizer.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("encyclopedia_entry")
public class EncyclopediaEntry extends BaseEntity {
    private String name;
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
    private Integer isPublished;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private String tags;
}
