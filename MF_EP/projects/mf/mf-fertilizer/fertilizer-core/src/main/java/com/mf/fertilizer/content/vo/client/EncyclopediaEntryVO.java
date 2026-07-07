package com.mf.fertilizer.content.vo.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EncyclopediaEntryVO implements Serializable {
    private Long id;
    private String name;
    private String scientificName;
    private String alias;
    private String family;
    private String genus;
    private String coverImage;
    private String images;
    private String description;
    private String morphology;
    private String distribution;
    private String habitat;
    private String careGuide;
    private String tags;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
}
