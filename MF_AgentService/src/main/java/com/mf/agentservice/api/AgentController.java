package com.mf.agentservice.api;

import com.mf.agentservice.agent.CustomerAgentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final CustomerAgentService customerAgentService;

    public AgentController(CustomerAgentService customerAgentService) {
        this.customerAgentService = customerAgentService;
    }

    @PostMapping("/chat")
    public AgentChatResponse chat(@Valid @RequestBody AgentChatRequest request) {
        return customerAgentService.chat(request);
    }
}
