package com.mf.fertilizer.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mf.fertilizer.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment")
public class Payment extends BaseEntity {
    private Long orderId;
    private String orderNo;
    private Long userId;
    private String payMethod;
    private BigDecimal amount;
    private String tradeNo;
    private String status;
    private LocalDateTime payTime;
    private BigDecimal refundAmount;
    private LocalDateTime refundTime;
    private String rawResponse;
}
