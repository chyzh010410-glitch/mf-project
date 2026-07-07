package com.mf.fertilizer.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mf.fertilizer.platform.entity.PlatformConfig;

public interface PlatformConfigService extends IService<PlatformConfig> {
    String getValue(String key, String defaultValue);

    boolean getBoolean(String key, boolean defaultValue);

    int getInt(String key, int defaultValue);
}
