package com.mf.fertilizer.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mf.fertilizer.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product_category")
public class ProductCategory extends BaseEntity {
    private String name;
    private Long parentId;
    private String type;
    private Integer sortOrder;
    private String icon;
    private String description;
}
