package com.mf.fertilizer.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mf.fertilizer.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product")
public class Product extends BaseEntity {
    private String name;
    private String productType;
    private Long merchantId;
    private Long categoryId;
    private String brand;
    private String coverImage;
    private String images;
    private String videoUrl;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private String unit;
    private Integer salesCount;
    private Integer status;
    private Integer isRecommend;
    private Integer isNew;
    private Integer sortOrder;
    private String description;
    private Integer minPurchase;
    private BigDecimal freight;
}
