package com.mf.fertilizer.order.dto.client;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PayOrderDTO {

    @Size(max = 20, message = "支付方式长度不能超过20个字符")
    private String paymentMethod;
}
