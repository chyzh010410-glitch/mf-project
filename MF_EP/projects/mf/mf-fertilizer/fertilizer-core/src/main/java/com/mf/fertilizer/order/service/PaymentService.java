package com.mf.fertilizer.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.order.entity.Payment;
import com.mf.fertilizer.vo.PageVO;

public interface PaymentService extends IService<Payment> {

    void payOrder(Long userId, Long orderId, String paymentMethod);

    void refundOrder(Long orderId, String reason);

    PageVO<Payment> listAdminPayments(PageDTO page, String status, String orderNo, String tradeNo);
}
