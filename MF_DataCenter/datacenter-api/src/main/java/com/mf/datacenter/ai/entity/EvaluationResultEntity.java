package com.mf.datacenter.ai.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;
@Data @TableName("dc_evaluation_result") public class EvaluationResultEntity { @TableId(type = IdType.AUTO) private Long id; private Long caseId; private String actualIntent; private String actualTools; private String actualFallbackReason; private String answerSnapshot; private Boolean passed; private String failureReason; private LocalDateTime executedAt; }
