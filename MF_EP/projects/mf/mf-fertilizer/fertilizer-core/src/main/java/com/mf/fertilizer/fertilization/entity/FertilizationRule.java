package com.mf.fertilizer.fertilization.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mf.fertilizer.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fertilization_rule")
public class FertilizationRule extends BaseEntity {

    private String species;

    private Integer ageMin;

    private Integer ageMax;

    /** spring / summer / autumn / winter / all */
    private String season;

    private Long fertilizerId;

    private BigDecimal recommendAmount;

    private String method;

    /** 閻℃帒锕ら妵鍥╂惥婵犱胶鍠橀柛?*/
    private Integer priority;

    private String remark;
}
