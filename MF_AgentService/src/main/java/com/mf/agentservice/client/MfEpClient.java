package com.mf.agentservice.client;

import com.fasterxml.jackson.databind.JsonNode;
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

    public MfEpClient(@Qualifier("mfEpRestClient") RestClient restClient) {
        this.restClient = restClient;
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

    public JsonNode orderStatus(Long orderId, String authToken) {
        return getEnvelopeData(restClient.get()
                .uri("/client/orders/{id}", orderId)
                .header("Authorization", normalizeBearer(authToken))
                .retrieve()
                .body(ENVELOPE));
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
