package com.mf.fertilizer.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mf.fertilizer.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("encyclopedia_article")
public class EncyclopediaArticle extends BaseEntity {
    private String title;
    private String summary;
    private String coverImage;
    private String images;
    private String content;
    private Long authorId;
    private Long categoryId;
    private String tags;
    private Integer isPublished;
    private Integer isTop;
    private Integer isRecommend;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
}
