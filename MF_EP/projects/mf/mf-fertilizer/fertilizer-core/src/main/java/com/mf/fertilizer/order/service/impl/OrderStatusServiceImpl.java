package com.mf.fertilizer.order.service.impl;

import com.mf.fertilizer.constant.OrderStatus;
import com.mf.fertilizer.constant.ResultCode;
import com.mf.fertilizer.exception.BusinessException;
import com.mf.fertilizer.order.entity.OrderEntity;
import com.mf.fertilizer.order.service.OrderStatusService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class OrderStatusServiceImpl implements OrderStatusService {

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING_PAY, Set.of(OrderStatus.PENDING_SHIP, OrderStatus.CANCELLED),
            OrderStatus.PENDING_SHIP, Set.of(OrderStatus.SHIPPED, OrderStatus.REFUND_REQUESTED, OrderStatus.REFUNDED),
            OrderStatus.SHIPPED, Set.of(OrderStatus.COMPLETED, OrderStatus.REFUND_REQUESTED, OrderStatus.REFUNDED),
            OrderStatus.COMPLETED, Set.of(OrderStatus.REFUND_REQUESTED, OrderStatus.REFUNDED),
            OrderStatus.REFUND_REQUESTED, Set.of(OrderStatus.REFUNDED)
    );

    @Override
    public void checkCanCancel(OrderEntity order) {
        checkCurrentStatus(order, OrderStatus.PENDING_PAY, "Only pending payment orders can be cancelled");
    }

    @Override
    public void checkCanPay(OrderEntity order) {
        checkCurrentStatus(order, OrderStatus.PENDING_PAY, "Only pending payment orders can be paid");
    }

    @Override
    public void checkCanShip(OrderEntity order) {
        checkCurrentStatus(order, OrderStatus.PENDING_SHIP, "Current order status cannot be shipped");
    }

    @Override
    public void checkCanComplete(OrderEntity order) {
        checkCurrentStatus(order, OrderStatus.SHIPPED, "Only shipped orders can be completed");
    }

    @Override
    public void checkCanChangeTo(OrderEntity order, String targetStatus) {
        if (targetStatus == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Target order status cannot be empty");
        }
        if (targetStatus.equals(order.getStatus())) {
            return;
        }
        var nextStatuses = ALLOWED_TRANSITIONS.getOrDefault(order.getStatus(), Set.of());
        if (!nextStatuses.contains(targetStatus)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Illegal order status transition");
        }
    }

    private void checkCurrentStatus(OrderEntity order, String requiredStatus, String message) {
        if (!requiredStatus.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, message);
        }
    }
}
