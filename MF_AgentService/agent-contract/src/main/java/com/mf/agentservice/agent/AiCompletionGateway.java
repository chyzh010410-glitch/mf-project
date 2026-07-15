package com.mf.agentservice.agent;

import java.util.Optional;

public interface AiCompletionGateway {
    Optional<String> complete(String systemPrompt, String userMessage);
}
