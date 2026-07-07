package com.mf.agentservice.agent;

import com.mf.agentservice.config.MfAgentProperties;
import java.util.Optional;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(ChatClient.Builder.class)
@ConditionalOnProperty(prefix = "mf.agent", name = "llm-enabled", havingValue = "true")
public class SpringAiCompletionGateway implements AiCompletionGateway {
    private final ChatClient chatClient;
    private final MfAgentProperties properties;

    public SpringAiCompletionGateway(ChatClient.Builder chatClientBuilder, MfAgentProperties properties) {
        this.chatClient = chatClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public Optional<String> complete(String systemPrompt, String userMessage) {
        if (!properties.agent().llmEnabled()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .content());
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }
}
