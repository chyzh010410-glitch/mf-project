package com.mf.fertilizer.product.service.impl;

import com.mf.fertilizer.constant.RedisKey;
import com.mf.fertilizer.product.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean deductProductStock(Long productId, Integer currentStock, Integer quantity) {
        String stockKey = RedisKey.productStock(productId);
        redisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(currentStock));
        Long remaining = redisTemplate.opsForValue().decrement(stockKey, quantity);
        if (remaining != null && remaining < 0) {
            rollbackProductStock(productId, quantity);
            return false;
        }
        return true;
    }

    @Override
    public void rollbackProductStock(Long productId, Integer quantity) {
        redisTemplate.opsForValue().increment(RedisKey.productStock(productId), quantity);
    }
}
