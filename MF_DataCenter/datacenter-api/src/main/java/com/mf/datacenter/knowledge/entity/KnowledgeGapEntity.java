package com.mf.datacenter.knowledge.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.time.LocalDateTime;
@Data @TableName("dc_ai_knowledge_gap") public class KnowledgeGapEntity { @TableId(type=IdType.AUTO) private Long id; private String normalizedTopic; private String sampleQuestion; private Integer occurrenceCount; private Integer lowScoreCount; private String riskLevel; private String status; private LocalDateTime lastSeenAt; private LocalDateTime createTime; private LocalDateTime updateTime; }
