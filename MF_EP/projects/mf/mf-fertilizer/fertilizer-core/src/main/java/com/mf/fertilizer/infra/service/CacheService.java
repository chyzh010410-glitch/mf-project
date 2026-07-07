package com.mf.fertilizer.infra.service;

import java.time.Duration;

public interface CacheService {

    void set(String key, String value, Duration ttl);

    String get(String key);

    boolean hasKey(String key);

    void delete(String key);
}
