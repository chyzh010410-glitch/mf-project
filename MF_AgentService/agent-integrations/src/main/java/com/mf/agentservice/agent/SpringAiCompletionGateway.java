package com.mf.agentservice.agent;

import com.mf.agentservice.config.MfAgentProperties;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@ConditionalOnProperty(prefix = "mf.agent", name = "llm-enabled", havingValue = "true")
public class SpringAiCompletionGateway implements AiCompletionGateway {
    private static final Logger LOG = LoggerFactory.getLogger(SpringAiCompletionGateway.class);
    private final ChatClient chatClient;
    private final MfAgentProperties properties;
    private final String apiKey;
    private final AtomicInteger requestsInWindow = new AtomicInteger();
    private volatile Instant windowStartedAt = Instant.now();

    public SpringAiCompletionGateway(
            ChatClient.Builder chatClientBuilder,
            MfAgentProperties properties,
            @Value("${spring.ai.openai.api-key:}") String apiKey
    ) {
        this.chatClient = chatClientBuilder.build();
        this.properties = properties;
        this.apiKey = apiKey;
    }

    @Override
    public Optional<String> complete(String systemPrompt, String userMessage) {
        if (!properties.agent().llmEnabled() || apiKey.isBlank() || !tryAcquireRequest()) {
            return Optional.empty();
        }
        try {
            String answer = CompletableFuture.supplyAsync(() -> chatClient.prompt()
                            .system(systemPrompt)
                            .user(userMessage)
                            .call()
                            .content())
                    .get(properties.agent().llmRequestTimeout().toMillis(), TimeUnit.MILLISECONDS);
            return Optional.ofNullable(answer);
        } catch (Exception ex) {
            LOG.warn("DeepSeek chat completion failed; falling back to controlled response: {}", safeMessage(ex));
            return Optional.empty();
        }
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.replaceAll("(?i)(sk-[a-z0-9_-]+|bearer\\s+[^\\s]+)", "***");
    }

    private synchronized boolean tryAcquireRequest() {
        if (windowStartedAt.plusSeconds(60).isBefore(Instant.now())) {
            windowStartedAt = Instant.now();
            requestsInWindow.set(0);
        }
        return requestsInWindow.incrementAndGet() <= properties.agent().llmMaxRequestsPerMinute();
    }
}
