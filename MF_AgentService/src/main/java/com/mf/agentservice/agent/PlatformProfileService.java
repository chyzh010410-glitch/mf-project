package com.mf.agentservice.agent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
public class PlatformProfileService {
    private static final Logger LOG = LoggerFactory.getLogger(PlatformProfileService.class);
    private static final String RESOURCE_PATH = "classpath:agent/platform-profile.md";
    private static final String FALLBACK_PROFILE = "平台介绍暂未加载；不要编造平台事实。";

    private final String content;

    public PlatformProfileService(ResourceLoader resourceLoader) {
        this.content = load(resourceLoader.getResource(RESOURCE_PATH));
    }

    public String content() {
        return content;
    }

    private String load(Resource resource) {
        try (var input = resource.getInputStream()) {
            String value = new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
            return value.isBlank() ? FALLBACK_PROFILE : value;
        } catch (IOException exception) {
            LOG.warn("Platform profile could not be loaded; using a safe fallback");
            return FALLBACK_PROFILE;
        }
    }
}
