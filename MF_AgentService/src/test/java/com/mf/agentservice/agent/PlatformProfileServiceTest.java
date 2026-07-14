package com.mf.agentservice.agent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class PlatformProfileServiceTest {

    @Test
    void loadsVersionControlledPlatformProfile() {
        var profile = new PlatformProfileService(new DefaultResourceLoader());

        assertThat(profile.content()).contains("苗丰施肥平台", "不能执行退款");
    }
}
