package com.mf.fertilizer.order.dto.client;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartAddDTO {
    @NotNull private Long productId;
    @NotNull private Integer quantity;
}
