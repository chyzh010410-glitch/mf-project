package com.mf.fertilizer.content.vo.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EncyclopediaArticleVO implements Serializable {
    private Long id;
    private String title;
    private String summary;
    private String coverImage;
    private String content;
    private String tags;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime createTime;
}
