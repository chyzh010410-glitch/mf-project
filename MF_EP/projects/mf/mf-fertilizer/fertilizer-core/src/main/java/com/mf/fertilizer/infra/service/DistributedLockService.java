package com.mf.fertilizer.infra.service;

import java.time.Duration;

public interface DistributedLockService {

    boolean tryLock(String key, String value, Duration expire);

    void unlock(String key, String value);
}
