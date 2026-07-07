package com.mf.fertilizer.task;

import com.mf.fertilizer.constant.RedisKey;
import com.mf.fertilizer.infra.service.DistributedLockService;
import com.mf.fertilizer.order.service.OrderApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final DistributedLockService distributedLockService;
    private final OrderApplicationService orderApplicationService;

    @Scheduled(cron = "0 * * * * ?")
    public void cancelTimeoutOrders() {
        // Redis 分布式锁：SETNX 防多实例重复执行
        String lockValue = UUID.randomUUID().toString();
        var locked = distributedLockService.tryLock(RedisKey.ORDER_TIMEOUT_TASK_LOCK, lockValue, Duration.ofSeconds(50));
        if (!locked) return;

        try {
            var deadline = orderApplicationService.getPaymentTimeoutDeadline(LocalDateTime.now());
            orderApplicationService.closeTimeoutOrders(deadline);
        } catch (Exception e) {
            log.error("订单超时任务异常: {}", e.getMessage());
        } finally {
            distributedLockService.unlock(RedisKey.ORDER_TIMEOUT_TASK_LOCK, lockValue);
        }
    }
}
