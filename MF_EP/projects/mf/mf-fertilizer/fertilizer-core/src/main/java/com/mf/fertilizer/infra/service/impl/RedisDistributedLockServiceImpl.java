package com.mf.fertilizer.infra.service.impl;

import com.mf.fertilizer.infra.service.DistributedLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisDistributedLockServiceImpl implements DistributedLockService {

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean tryLock(String key, String value, Duration expire) {
        var locked = redisTemplate.opsForValue().setIfAbsent(key, value, expire);
        return Boolean.TRUE.equals(locked);
    }

    @Override
    public void unlock(String key, String value) {
        redisTemplate.execute(UNLOCK_SCRIPT, List.of(key), value);
    }
}
