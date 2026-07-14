package com.mf.agentservice.api;

import com.mf.agentservice.agent.AiCompletionGateway;
import com.mf.agentservice.config.MfAgentProperties;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/runtime")
public class AgentRuntimeController {
    private final MfAgentProperties properties;
    private final ObjectProvider<AiCompletionGateway> completionGateway;
    private final String apiKey;

    public AgentRuntimeController(
            MfAgentProperties properties,
            ObjectProvider<AiCompletionGateway> completionGateway,
            @Value("${spring.ai.openai.api-key:}") String apiKey
    ) {
        this.properties = properties;
        this.completionGateway = completionGateway;
        this.apiKey = apiKey;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "llmEnabled", properties.agent().llmEnabled(),
                "apiKeyConfigured", !apiKey.isBlank(),
                "chatGatewayAvailable", completionGateway.getIfAvailable() != null
        );
    }
}
