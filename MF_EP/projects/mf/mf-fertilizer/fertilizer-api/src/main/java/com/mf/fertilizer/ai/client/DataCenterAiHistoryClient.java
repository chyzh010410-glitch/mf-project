package com.mf.fertilizer.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;

@Component
public class DataCenterAiHistoryClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String internalToken;
    private final String identitySecret;

    public DataCenterAiHistoryClient(
            @Value("${mf.datacenter.base-url:http://localhost:8091}") String baseUrl,
            @Value("${mf.datacenter.internal-token:change-me}") String internalToken,
            @Value("${mf.datacenter.internal-identity-secret:change-me-identity}") String identitySecret
    ) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
        this.baseUrl = baseUrl;
        this.internalToken = internalToken;
        this.identitySecret = identitySecret;
    }

    public JsonNode getConversations(Long userId, String sessionId, long page, long pageSize) {
        var url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/api/ai/my-conversations")
                .queryParam("sessionId", sessionId)
                .queryParam("page", page)
                .queryParam("pageSize", pageSize)
                .toUriString();
        return responseData(restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers(userId)), JsonNode.class).getBody());
    }

    public JsonNode deleteConversations(Long userId, String sessionId) {
        var url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/api/ai/my-conversations/{sessionId}")
                .buildAndExpand(sessionId)
                .toUriString();
        return responseData(restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers(userId)), JsonNode.class).getBody());
    }

    private HttpHeaders headers(Long userId) {
        var userIdText = String.valueOf(userId);
        var userType = "client";
        var headers = new HttpHeaders();
        headers.set("X-MF-Internal-Token", internalToken);
        headers.set("X-MF-User-Id", userIdText);
        headers.set("X-MF-User-Type", userType);
        headers.set("X-MF-Identity-Signature", sign(userIdText + ":" + userType));
        return headers;
    }

    private JsonNode responseData(JsonNode body) {
        if (body == null || body.path("code").asInt(-1) != 0) {
            throw new IllegalStateException("DataCenter history request failed");
        }
        return body.path("data");
    }

    private String sign(String content) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(identitySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            var bytes = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            var out = new StringBuilder();
            for (byte b : bytes) {
                out.append(String.format("%02x", b));
            }
            return out.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("identity signature unavailable", ex);
        }
    }
}
