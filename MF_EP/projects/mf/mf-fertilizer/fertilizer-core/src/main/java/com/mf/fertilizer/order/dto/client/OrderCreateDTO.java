package com.mf.fertilizer.order.dto.client;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class OrderCreateDTO {
    @NotNull private Long addressId;
    @NotNull private List<OrderItemDTO> items;
    private String remark;
    
    @Data
    public static class OrderItemDTO {
        @NotNull private Long productId;
        @NotNull private Integer quantity;
    }
}
