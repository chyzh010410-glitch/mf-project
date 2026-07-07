package com.mf.fertilizer.order.service.impl;

import com.mf.fertilizer.constant.OrderStatus;
import com.mf.fertilizer.order.entity.OrderEntity;

import java.time.LocalDateTime;

final class OrderPaymentRules {

    private static final long PAYMENT_TIMEOUT_MINUTES = 1;

    private OrderPaymentRules() {
    }

    static long defaultTimeoutMinutes() {
        return PAYMENT_TIMEOUT_MINUTES;
    }

    static long normalizeTimeoutMinutes(long timeoutMinutes) {
        return timeoutMinutes > 0 ? timeoutMinutes : PAYMENT_TIMEOUT_MINUTES;
    }

    static LocalDateTime timeoutDeadline(LocalDateTime now, long timeoutMinutes) {
        return now.minusMinutes(normalizeTimeoutMinutes(timeoutMinutes));
    }

    static LocalDateTime expireTime(OrderEntity order, long timeoutMinutes) {
        if (order.getCreateTime() == null || !OrderStatus.PENDING_PAY.equals(order.getStatus())) {
            return null;
        }
        return order.getCreateTime().plusMinutes(normalizeTimeoutMinutes(timeoutMinutes));
    }

    static boolean isExpired(OrderEntity order, LocalDateTime now, long timeoutMinutes) {
        var expireTime = expireTime(order, timeoutMinutes);
        return expireTime != null && !now.isBefore(expireTime);
    }
}
