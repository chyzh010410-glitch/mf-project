package com.mf.fertilizer.order.service;

import com.mf.fertilizer.order.dto.client.OrderCreateDTO;
import com.mf.fertilizer.order.entity.OrderEntity;
import com.mf.fertilizer.order.vo.client.OrderCreateResultVO;
import com.mf.fertilizer.order.vo.client.OrderVO;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.vo.PageVO;

import java.time.LocalDateTime;
import java.util.Map;

public interface OrderApplicationService {

    OrderCreateResultVO createOrder(Long userId, OrderCreateDTO dto);

    PageVO<OrderVO> listUserOrders(Long userId, PageDTO page, String status);

    OrderVO getUserOrderDetail(Long userId, Long orderId);

    PageVO<OrderEntity> listAdminOrders(PageDTO page, String status, String orderNo);

    Map<String, Object> getAdminOrderDetail(Long orderId);

    Map<String, Long> getAdminOrderStatistics();

    PageVO<OrderVO> listMerchantOrders(Long merchantId, PageDTO page, String status);

    OrderVO getMerchantOrderDetail(Long merchantId, Long orderId);

    void shipMerchantOrder(Long merchantId, Long orderId, String logisticsCompany, String logisticsNo);

    void cancelOrder(Long orderId, String reason);

    void cancelUserOrder(Long userId, Long orderId, String reason);

    void requestRefund(Long userId, Long orderId, String reason);

    void payOrder(Long orderId);

    void shipOrder(Long orderId, String logisticsCompany, String logisticsNo);

    void completeOrder(Long orderId);

    void completeUserOrder(Long userId, Long orderId);

    void changeStatus(Long orderId, String targetStatus);

    LocalDateTime getPaymentTimeoutDeadline(LocalDateTime now);

    void closeTimeoutOrders(LocalDateTime deadline);
}
