package com.mf.agentservice.api;

import jakarta.validation.constraints.NotBlank;

public record AgentChatRequest(
        String sessionId,
        @NotBlank String message,
        String userId,
        String userType,
        String authToken
) {
}
