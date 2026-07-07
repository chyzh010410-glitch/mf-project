package com.mf.fertilizer.product.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductSaveDTO {
    @NotBlank private String name;
    @NotBlank private String productType;
    private Long categoryId;
    private String brand;
    private String coverImage;
    private String images;
    @NotNull private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private String unit;
    private String description;
    private String detailType;
    private String attrsJson;
    private Integer status;
    private Integer isRecommend;
    private Integer isNew;
    private BigDecimal freight;
}
