package com.mf.datacenter.knowledge.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.time.LocalDateTime;
@Data @TableName("dc_ai_research_source") public class ResearchSourceEntity { @TableId(type=IdType.AUTO) private Long id; private Long gapId; private String title; private String url; private String publisher; private LocalDateTime retrievedAt; private String summary; private Integer authorityScore; private LocalDateTime createTime; }
