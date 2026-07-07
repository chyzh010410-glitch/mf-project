package com.mf.fertilizer.fertilization.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsVO implements Serializable {

    /** Total fertilization records. */
    private Long totalRecords;

    /** Total fertilized area in square meters. */
    private BigDecimal totalArea;

    /** Total fertilizer amount in kg. */
    private BigDecimal totalAmount;

    /** Number of trees involved. */
    private Long treeCount;

    /** Number of fertilizer types involved. */
    private Long fertilizerTypeCount;
}
