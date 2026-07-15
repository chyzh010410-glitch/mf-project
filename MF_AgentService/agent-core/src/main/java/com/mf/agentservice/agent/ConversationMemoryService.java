package com.mf.agentservice.agent;

import com.mf.agentservice.config.MfAgentProperties;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class ConversationMemoryService {
    private final Map<String, SessionMemory> sessions = new ConcurrentHashMap<>();
    private final MfAgentProperties properties;

    public ConversationMemoryService(MfAgentProperties properties) {
        this.properties = properties;
    }

    public Optional<ConversationTurn> latestTurn(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        SessionMemory memory = sessions.get(sessionId);
        if (memory == null || memory.expired(properties.agent().sessionTtl())) {
            sessions.remove(sessionId);
            return Optional.empty();
        }
        return memory.latestTurn();
    }

    public List<ConversationTurn> recentTurns(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return List.of();
        }
        SessionMemory memory = sessions.get(sessionId);
        if (memory == null || memory.expired(properties.agent().sessionTtl())) {
            sessions.remove(sessionId);
            return List.of();
        }
        return memory.turns();
    }

    public void remember(String sessionId, AgentIntent intent, String message, String answer) {
        if (sessionId == null || sessionId.isBlank() || !isLowRisk(intent)) {
            return;
        }
        sessions.compute(sessionId, (key, current) -> {
            SessionMemory memory = current == null || current.expired(properties.agent().sessionTtl())
                    ? new SessionMemory() : current;
            memory.add(intent, message, answer, properties.agent().sessionMaxTurns());
            return memory;
        });
    }

    private boolean isLowRisk(AgentIntent intent) {
        return intent == AgentIntent.DIRECT || intent == AgentIntent.COMPANY || intent == AgentIntent.PRODUCT
                || intent == AgentIntent.ENCYCLOPEDIA || intent == AgentIntent.MERCHANT || intent == AgentIntent.UNKNOWN;
    }

    private static final class SessionMemory {
        private final ArrayDeque<ConversationTurn> turns = new ArrayDeque<>();
        private Instant updatedAt = Instant.now();

        void add(AgentIntent intent, String message, String answer, int maxTurns) {
            turns.addLast(new ConversationTurn(intent, message, answer));
            while (turns.size() > maxTurns) {
                turns.removeFirst();
            }
            updatedAt = Instant.now();
        }

        Optional<ConversationTurn> latestTurn() {
            return Optional.ofNullable(turns.peekLast());
        }

        List<ConversationTurn> turns() {
            return List.copyOf(turns);
        }

        boolean expired(java.time.Duration ttl) {
            return updatedAt.plus(ttl).isBefore(Instant.now());
        }
    }

    public record ConversationTurn(AgentIntent intent, String message, String answer) {
    }
}
