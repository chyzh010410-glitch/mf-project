package com.mf.agentservice.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mf")
public record MfAgentProperties(
        Endpoint ep,
        Endpoint datacenter,
        Agent agent
) {
    public MfAgentProperties {
        ep = ep == null ? new Endpoint("http://localhost:8080") : ep;
        datacenter = datacenter == null ? new Endpoint("http://localhost:8091") : datacenter;
        agent = agent == null ? new Agent(false, Duration.ofSeconds(5)) : agent;
    }

    public record Endpoint(String baseUrl) {
    }

    public record Agent(Boolean llmEnabled, Duration requestTimeout) {
        public Agent {
            llmEnabled = Boolean.TRUE.equals(llmEnabled);
            requestTimeout = requestTimeout == null ? Duration.ofSeconds(5) : requestTimeout;
        }
    }
}
