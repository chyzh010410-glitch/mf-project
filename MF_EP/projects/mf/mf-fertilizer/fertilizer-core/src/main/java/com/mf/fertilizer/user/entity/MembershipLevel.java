package com.mf.fertilizer.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mf.fertilizer.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("membership_level")
public class MembershipLevel extends BaseEntity {
    private String name;
    private Integer level;
    private Integer minPoints;
    private BigDecimal discountRate;
    private String icon;
    private String description;
}
