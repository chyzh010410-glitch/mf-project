package com.mf.fertilizer.fertilization.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendResultVO implements Serializable {

    private Long fertilizerId;
    private String fertilizerName;
    private String fertilizerType;
    private String nutrientContent;
    private BigDecimal recommendAmount;
    private BigDecimal unitPrice;
    private String method;
    private String ruleRemark;
    private Integer priority;
}
