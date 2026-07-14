package com.mf.datacenter.ai.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;
@Data @TableName("dc_evaluation_case") public class EvaluationCaseEntity { @TableId(type = IdType.AUTO) private Long id; private Long suiteId; private String question; private String expectedIntent; private String expectedTool; private String expectedSafetyResult; private String tags; private Boolean enabled; private LocalDateTime createTime; }
