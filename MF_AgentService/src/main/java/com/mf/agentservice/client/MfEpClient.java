package com.mf.agentservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.mf.agentservice.config.MfAgentProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MfEpClient {
    private static final ParameterizedTypeReference<ApiEnvelope> ENVELOPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final String internalToken;

    public MfEpClient(@Qualifier("mfEpRestClient") RestClient restClient, MfAgentProperties properties) {
        this.restClient = restClient;
        this.internalToken = properties.ep().internalToken();
    }

    public JsonNode searchProducts(String keyword, String productType, Integer page, Integer size) {
        return getEnvelopeData(restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/client/products")
                        .queryParam("keyword", nullToEmpty(keyword))
                        .queryParam("productType", nullToEmpty(productType))
                        .queryParam("page", page == null ? 1 : page)
                        .queryParam("size", size == null ? 5 : size)
                        .build())
                .retrieve()
                .body(ENVELOPE));
    }

    public JsonNode productDetail(Long productId) {
        return getEnvelopeData(restClient.get()
                .uri("/client/products/{id}", productId)
                .retrieve()
                .body(ENVELOPE));
    }

    public JsonNode searchEncyclopedia(String keyword, Integer page, Integer size) {
        return getEnvelopeData(restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/client/encyclopedia")
                        .queryParam("keyword", nullToEmpty(keyword))
                        .queryParam("page", page == null ? 1 : page)
                        .queryParam("size", size == null ? 5 : size)
                        .build())
                .retrieve()
                .body(ENVELOPE));
    }

    public JsonNode listArticles(Integer page, Integer size) {
        return getEnvelopeData(restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/client/articles")
                        .queryParam("page", page == null ? 1 : page)
                        .queryParam("size", size == null ? 100 : size)
                        .build())
                .retrieve()
                .body(ENVELOPE));
    }

    public JsonNode listFaq() {
        return getEnvelopeData(restClient.get()
                .uri("/client/faq")
                .retrieve()
                .body(ENVELOPE));
    }

    public JsonNode orderStatus(Long orderId, String authToken) {
        return getEnvelopeData(restClient.get()
                .uri("/client/orders/{id}", orderId)
                .header("Authorization", normalizeBearer(authToken))
                .retrieve()
                .body(ENVELOPE));
    }

    public List<KnowledgeSyncEvent> pendingKnowledgeSyncEvents(int limit) {
        var data = getEnvelopeData(restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/internal/ai-content/sync-events")
                        .queryParam("limit", Math.max(1, limit))
                        .build())
                .header("X-MF-Internal-Token", internalToken)
                .retrieve()
                .body(ENVELOPE));
        List<KnowledgeSyncEvent> events = new ArrayList<>();
        for (JsonNode event : data) {
            events.add(new KnowledgeSyncEvent(
                    event.path("id").asLong(),
                    event.path("draftId").asLong(),
                    event.path("mfEpContentId").asLong(),
                    event.path("contentType").asText(),
                    event.path("action").asText(),
                    event.path("version").asInt()));
        }
        return events;
    }

    public void acknowledgeKnowledgeSyncEvent(long eventId) {
        getEnvelopeData(restClient.post()
                .uri("/internal/ai-content/sync-events/{id}/ack", eventId)
                .header("X-MF-Internal-Token", internalToken)
                .body(Map.of("consumer", "mf-agent-service"))
                .retrieve()
                .body(ENVELOPE));
    }

    public void reportKnowledgeSyncFailure(long eventId, String state, int failureAttempts, String reason) {
        getEnvelopeData(restClient.post()
                .uri("/internal/ai-content/sync-events/{id}/failure", eventId)
                .header("X-MF-Internal-Token", internalToken)
                .body(Map.of(
                        "consumer", "mf-agent-service",
                        "state", state,
                        "failureAttempts", String.valueOf(failureAttempts),
                        "reason", reason == null || reason.isBlank() ? "knowledge sync failed" : reason))
                .retrieve()
                .body(ENVELOPE));
    }

    public record KnowledgeSyncEvent(long id, long draftId, long mfEpContentId, String contentType, String action, int version) {
    }

    private JsonNode getEnvelopeData(ApiEnvelope envelope) {
        if (envelope == null) {
            throw new IllegalStateException("Empty response from MF_EP");
        }
        if (!envelope.ok()) {
            throw new IllegalStateException(envelope.message());
        }
        return envelope.data();
    }

    private String normalizeBearer(String authToken) {
        if (authToken == null || authToken.isBlank()) {
            return "";
        }
        return authToken.startsWith("Bearer ") ? authToken : "Bearer " + authToken;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
