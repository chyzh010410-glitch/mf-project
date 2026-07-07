package com.mf.fertilizer.order.vo.client;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderCreateResultVO {

    private Long orderId;
    private String orderNo;
    private BigDecimal payAmount;
    private LocalDateTime paymentExpireTime;
}
