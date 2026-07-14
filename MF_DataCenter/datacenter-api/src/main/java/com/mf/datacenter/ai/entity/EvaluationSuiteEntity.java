package com.mf.datacenter.ai.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;
@Data @TableName("dc_evaluation_suite") public class EvaluationSuiteEntity { @TableId(type = IdType.AUTO) private Long id; private String name; private String description; private LocalDateTime createTime; }
