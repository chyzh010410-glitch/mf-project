package com.mf.fertilizer.fertilization.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FertilizationRecordDTO {

    @NotNull(message = "树木ID不能为空")
    private Long treeId;

    @NotNull(message = "肥料ID不能为空")
    private Long fertilizerId;

    @NotNull(message = "施肥用量不能为空")
    private BigDecimal amount;

    @NotNull(message = "施肥日期不能为空")
    private LocalDate fertilizeDate;

    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;

    private String method;

    private String remark;
}
