package com.mf.fertilizer.order.vo.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartVO implements Serializable {
    private List<CartItemVO> items;
    private Integer totalCount;
    private BigDecimal totalAmount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemVO implements Serializable {
        private Long id;
        private Long productId;
        private String productName;
        private String productImage;
        private String productType;
        private Integer stock;
        private BigDecimal price;
        private BigDecimal freight;
        private Integer quantity;
        private Integer selected;
        private BigDecimal subtotal;
    }
}
