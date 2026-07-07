package com.mf.fertilizer.platform.service.impl;

import com.mf.fertilizer.constant.MessageConstants;
import com.mf.fertilizer.platform.entity.Message;
import com.mf.fertilizer.platform.service.MessageService;
import com.mf.fertilizer.platform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final MessageService messageService;

    @Override
    @Async("taskExecutor")
    public void sendOrderCreatedNotification(Long userId, String orderNo) {
        var message = new Message();
        message.setUserId(userId);
        message.setTitle("订单创建成功");
        message.setContent("您的订单 " + orderNo + " 已创建成功，请及时完成后续操作。");
        message.setType(MessageConstants.TYPE_ORDER);
        message.setPushChannel(MessageConstants.PUSH_CHANNEL_SYSTEM);
        message.setIsRead(0);
        messageService.save(message);
        log.info("发送订单创建通知成功: userId={}, orderNo={}", userId, orderNo);
    }
}
