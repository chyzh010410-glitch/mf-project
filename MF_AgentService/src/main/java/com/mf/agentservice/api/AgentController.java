package com.mf.agentservice.api;

import com.mf.agentservice.agent.CustomerAgentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final CustomerAgentService customerAgentService;
    private final String evaluationKey;

    public AgentController(CustomerAgentService customerAgentService, @Value("${mf.agent.evaluation-key:}") String evaluationKey) {
        this.customerAgentService = customerAgentService;
        this.evaluationKey = evaluationKey;
    }

    @PostMapping("/chat")
    public AgentChatResponse chat(@Valid @RequestBody AgentChatRequest request) {
        return customerAgentService.chat(request);
    }

    @PostMapping("/evaluate")
    public AgentChatResponse evaluate(
            @RequestHeader(name = "X-MF-Evaluation-Key") String evaluationKey,
            @Valid @RequestBody AgentChatRequest request
    ) {
        if (this.evaluationKey.isBlank() || !this.evaluationKey.equals(evaluationKey)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        }
        return customerAgentService.evaluate(request);
    }

    @PostMapping("/chat/stream")
    public SseEmitter streamChat(@Valid @RequestBody AgentChatRequest request) {
        SseEmitter emitter = new SseEmitter(30_000L);
        CompletableFuture.runAsync(() -> {
            try {
                emitter.send(SseEmitter.event().name("status").data("thinking"));
                emitter.send(SseEmitter.event().name("status").data("working"));
                AgentChatResponse response = customerAgentService.chat(request);
                sendAnswerChunks(emitter, response.answer());
                emitter.send(SseEmitter.event().name("result").data(response));
                emitter.send(SseEmitter.event().name("status").data(response.resolved() ? "success" : "doubt"));
                emitter.complete();
            } catch (IOException exception) {
                emitter.completeWithError(exception);
            } catch (RuntimeException exception) {
                try {
                    emitter.send(SseEmitter.event().name("status").data("error"));
                } catch (IOException ignored) {
                    // The caller has disconnected.
                }
                emitter.completeWithError(exception);
            }
        });
        return emitter;
    }

    private void sendAnswerChunks(SseEmitter emitter, String answer) throws IOException {
        if (answer == null || answer.isBlank()) {
            return;
        }
        int chunkSize = 12;
        for (int start = 0; start < answer.length(); start += chunkSize) {
            int end = Math.min(answer.length(), start + chunkSize);
            emitter.send(SseEmitter.event().name("token").data(answer.substring(start, end)));
        }
    }
}
