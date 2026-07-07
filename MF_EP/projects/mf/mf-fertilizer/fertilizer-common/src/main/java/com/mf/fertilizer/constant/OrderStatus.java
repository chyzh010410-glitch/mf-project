package com.mf.fertilizer.constant;

public interface OrderStatus {

    String PENDING_PAY = "pending_pay";
    String PENDING_SHIP = "pending_ship";
    String SHIPPED = "shipped";
    String COMPLETED = "completed";
    String CANCELLED = "cancelled";
    String REFUND_REQUESTED = "refund_requested";
    String REFUNDED = "refunded";
}
