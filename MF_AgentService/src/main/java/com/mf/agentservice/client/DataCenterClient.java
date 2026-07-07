package com.mf.agentservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class DataCenterClient {
    private static final ParameterizedTypeReference<ApiEnvelope> ENVELOPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    public DataCenterClient(@Qualifier("mfDataCenterRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public Long logConversation(Map<String, Object> body) {
        var data = post("/api/ai/conversations", body);
        return data == null || data.get("id") == null ? null : data.get("id").asLong();
    }

    public void logToolCall(Map<String, Object> body) {
        post("/api/ai/tool-calls", body);
    }

    public void reportUnresolved(Map<String, Object> body) {
        post("/api/ai/unresolved-questions", body);
    }

    public void saveSampleCandidate(Map<String, Object> body) {
        post("/api/ai/sample-candidates", body);
    }

    private JsonNode post(String path, Map<String, Object> body) {
        var envelope = restClient.post()
                .uri(path)
                .body(body)
                .retrieve()
                .body(ENVELOPE);
        if (envelope == null) {
            throw new IllegalStateException("Empty response from MF_DataCenter");
        }
        if (!envelope.ok()) {
            throw new IllegalStateException(envelope.message());
        }
        return envelope.data();
    }
}
