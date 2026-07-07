package com.mf.fertilizer.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.platform.entity.PlatformConfig;
import com.mf.fertilizer.platform.mapper.PlatformConfigMapper;
import com.mf.fertilizer.platform.service.PlatformConfigService;
import org.springframework.stereotype.Service;

@Service
public class PlatformConfigServiceImpl extends ServiceImpl<PlatformConfigMapper, PlatformConfig> implements PlatformConfigService {
    @Override
    public String getValue(String key, String defaultValue) {
        PlatformConfig config = lambdaQuery()
                .eq(PlatformConfig::getConfigKey, key)
                .last("limit 1")
                .one();
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            return defaultValue;
        }
        return config.getConfigValue();
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getValue(key, null);
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value)
                || "1".equals(value)
                || "yes".equalsIgnoreCase(value)
                || "on".equalsIgnoreCase(value);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String value = getValue(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
