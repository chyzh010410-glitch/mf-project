package com.mf.fertilizer.order.service.impl;

import com.mf.fertilizer.constant.OrderStatus;
import com.mf.fertilizer.constant.ResultCode;
import com.mf.fertilizer.exception.BusinessException;
import com.mf.fertilizer.order.entity.OrderEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderStatusServiceImplTest {

    private final OrderStatusServiceImpl service = new OrderStatusServiceImpl();

    @Test
    void checkCanCancelAllowsPendingPayOrder() {
        assertDoesNotThrow(() -> service.checkCanCancel(order(OrderStatus.PENDING_PAY)));
    }

    @Test
    void checkCanCancelRejectsNonPendingPayOrder() {
        var exception = assertThrows(
                BusinessException.class,
                () -> service.checkCanCancel(order(OrderStatus.SHIPPED))
        );

        assertEquals(ResultCode.BAD_REQUEST, exception.getCode());
    }

    @Test
    void checkCanChangeToAllowsConfiguredTransitions() {
        assertDoesNotThrow(() -> service.checkCanChangeTo(order(OrderStatus.PENDING_PAY), OrderStatus.PENDING_SHIP));
        assertDoesNotThrow(() -> service.checkCanChangeTo(order(OrderStatus.PENDING_PAY), OrderStatus.CANCELLED));
        assertDoesNotThrow(() -> service.checkCanChangeTo(order(OrderStatus.PENDING_SHIP), OrderStatus.SHIPPED));
        assertDoesNotThrow(() -> service.checkCanChangeTo(order(OrderStatus.PENDING_SHIP), OrderStatus.REFUND_REQUESTED));
        assertDoesNotThrow(() -> service.checkCanChangeTo(order(OrderStatus.PENDING_SHIP), OrderStatus.REFUNDED));
        assertDoesNotThrow(() -> service.checkCanChangeTo(order(OrderStatus.SHIPPED), OrderStatus.COMPLETED));
        assertDoesNotThrow(() -> service.checkCanChangeTo(order(OrderStatus.SHIPPED), OrderStatus.REFUND_REQUESTED));
        assertDoesNotThrow(() -> service.checkCanChangeTo(order(OrderStatus.SHIPPED), OrderStatus.REFUNDED));
        assertDoesNotThrow(() -> service.checkCanChangeTo(order(OrderStatus.COMPLETED), OrderStatus.REFUND_REQUESTED));
        assertDoesNotThrow(() -> service.checkCanChangeTo(order(OrderStatus.COMPLETED), OrderStatus.REFUNDED));
        assertDoesNotThrow(() -> service.checkCanChangeTo(order(OrderStatus.REFUND_REQUESTED), OrderStatus.REFUNDED));
    }

    @Test
    void checkCanChangeToAllowsNoopTransition() {
        assertDoesNotThrow(() -> service.checkCanChangeTo(order(OrderStatus.COMPLETED), OrderStatus.COMPLETED));
    }

    @Test
    void checkCanChangeToRejectsIllegalTransition() {
        var exception = assertThrows(
                BusinessException.class,
                () -> service.checkCanChangeTo(order(OrderStatus.PENDING_SHIP), OrderStatus.COMPLETED)
        );

        assertEquals(ResultCode.BAD_REQUEST, exception.getCode());
    }

    @Test
    void checkCanChangeToRejectsEmptyTargetStatus() {
        var exception = assertThrows(
                BusinessException.class,
                () -> service.checkCanChangeTo(order(OrderStatus.PENDING_PAY), null)
        );

        assertEquals(ResultCode.BAD_REQUEST, exception.getCode());
    }

    private OrderEntity order(String status) {
        var order = new OrderEntity();
        order.setStatus(status);
        return order;
    }
}
