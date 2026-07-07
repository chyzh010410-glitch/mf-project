package com.mf.fertilizer.fertilization.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mf.fertilizer.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tree")
public class Tree extends BaseEntity {

    private String species;

    private String variety;

    private Integer age;

    private LocalDate plantDate;

    private String location;

    private BigDecimal area;

    private Integer quantity;

    /** healthy / sick / dead */
    private String status;

    private String remark;
}
