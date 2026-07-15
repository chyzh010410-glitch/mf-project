package com.mf.agentservice.agent;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class CustomerServicePromptCatalog {
    private static final String PROMPT_RESOURCE = "classpath:prompts/customer-service-agent.md";

    private final String systemPrompt;

    public CustomerServicePromptCatalog(ResourceLoader resourceLoader) {
        try (InputStream input = resourceLoader.getResource(PROMPT_RESOURCE).getInputStream()) {
            this.systemPrompt = new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load customer service prompt", exception);
        }
    }

    public String systemPrompt() {
        return systemPrompt;
    }
}
