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
        ep = ep == null ? new Endpoint("http://localhost:8080", "") : ep;
        datacenter = datacenter == null ? new Endpoint("http://localhost:8091", "") : datacenter;
        agent = agent == null ? new Agent(false, Duration.ofSeconds(5), Duration.ofSeconds(10), 20,
                Duration.ofMinutes(20), 4, Duration.ofMinutes(10), "", Duration.ofSeconds(30), 5, Duration.ofMinutes(5)) : agent;
    }

    public record Endpoint(String baseUrl, String internalToken) {
        public Endpoint {
            internalToken = internalToken == null ? "" : internalToken;
        }
    }

    public record Agent(
            Boolean llmEnabled,
            Duration requestTimeout,
            Duration llmRequestTimeout,
            Integer llmMaxRequestsPerMinute,
            Duration sessionTtl,
            Integer sessionMaxTurns,
            Duration knowledgeRefreshInterval,
            String knowledgeSyncKey,
            Duration knowledgeSyncPollInterval,
            Integer knowledgeSyncMaxRetries,
            Duration knowledgeSyncPendingTimeout
    ) {
        public Agent {
            llmEnabled = Boolean.TRUE.equals(llmEnabled);
            requestTimeout = requestTimeout == null ? Duration.ofSeconds(5) : requestTimeout;
            llmRequestTimeout = llmRequestTimeout == null ? Duration.ofSeconds(10) : llmRequestTimeout;
            llmMaxRequestsPerMinute = llmMaxRequestsPerMinute == null ? 20 : Math.max(1, llmMaxRequestsPerMinute);
            sessionTtl = sessionTtl == null ? Duration.ofMinutes(20) : sessionTtl;
            sessionMaxTurns = sessionMaxTurns == null ? 4 : Math.max(1, sessionMaxTurns);
            knowledgeRefreshInterval = knowledgeRefreshInterval == null ? Duration.ofMinutes(10) : knowledgeRefreshInterval;
            knowledgeSyncKey = knowledgeSyncKey == null ? "" : knowledgeSyncKey;
            knowledgeSyncPollInterval = knowledgeSyncPollInterval == null ? Duration.ofSeconds(30) : knowledgeSyncPollInterval;
            knowledgeSyncMaxRetries = knowledgeSyncMaxRetries == null ? 5 : Math.max(1, knowledgeSyncMaxRetries);
            knowledgeSyncPendingTimeout = knowledgeSyncPendingTimeout == null ? Duration.ofMinutes(5) : knowledgeSyncPendingTimeout;
        }
    }
}
