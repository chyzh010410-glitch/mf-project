package com.mf.fertilizer.order.service;

import com.mf.fertilizer.order.entity.OrderEntity;

public interface OrderStatusService {

    void checkCanCancel(OrderEntity order);

    void checkCanPay(OrderEntity order);

    void checkCanShip(OrderEntity order);

    void checkCanComplete(OrderEntity order);

    void checkCanChangeTo(OrderEntity order, String targetStatus);
}
