package com.mf.fertilizer.platform.service;

public interface NotificationService {
    void sendOrderCreatedNotification(Long userId, String orderNo);
}
