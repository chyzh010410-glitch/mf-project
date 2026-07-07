package com.mf.fertilizer.order.vo.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO implements Serializable {
    private Long id;
    private String orderNo;
    private LocalDateTime createTime;
    private LocalDateTime paymentExpireTime;
    private BigDecimal totalAmount;
    private BigDecimal freightAmount;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private String status;
    private String paymentMethod;
    private LocalDateTime payTime;
    private LocalDateTime shipTime;
    private LocalDateTime completeTime;
    private LocalDateTime cancelTime;
    private String cancelReason;
    private String adminRemark;
    private String logisticsCompany;
    private String logisticsNo;
    private String addressSnapshot;
    private List<OrderItemVO> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemVO implements Serializable {
        private Long merchantId;
        private Long productId;
        private String productName;
        private String productImage;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal totalPrice;
    }
}
