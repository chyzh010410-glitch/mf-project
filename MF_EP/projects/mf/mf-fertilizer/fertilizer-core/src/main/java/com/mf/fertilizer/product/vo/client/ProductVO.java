package com.mf.fertilizer.product.vo.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVO implements Serializable {
    private Long id;
    private String name;
    private String productType;
    private String categoryName;
    private String brand;
    private String coverImage;
    private String images;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private String unit;
    private Integer salesCount;
    private Integer status;
    private Integer isRecommend;
    private Integer isNew;
    private String description;
    private BigDecimal freight;
    private String attrsJson;
    private String detailType;
}
