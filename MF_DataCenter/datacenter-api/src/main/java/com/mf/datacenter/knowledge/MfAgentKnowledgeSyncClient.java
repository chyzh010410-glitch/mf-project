package com.mf.datacenter.knowledge;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class MfAgentKnowledgeSyncClient {
    private final RestClient client;
    private final String token;

    public MfAgentKnowledgeSyncClient(@Value("${datacenter.agent.knowledge-sync.base-url:http://localhost:8092}") String baseUrl,
                                      @Value("${datacenter.agent.knowledge-sync.internal-token:}") String token) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.token = token;
    }

    @SuppressWarnings("unchecked")
    public SyncResult sync(String requestId, Long contentId) {
        if (token == null || token.isBlank()) throw new IllegalStateException("未配置 AgentService 知识同步凭据");
        Map<String, Object> response = client.post().uri("/api/agent/knowledge/internal-sync")
                .header("X-MF-Internal-Token", token).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("requestId", requestId, "contentIds", contentId == null ? List.of() : List.of(contentId)))
                .retrieve().body(Map.class);
        if (response == null) throw new IllegalStateException("AgentService 未返回同步结果");
        return new SyncResult(String.valueOf(response.get("requestId")), bool(response.get("success")), bool(response.get("reused")), number(response.get("indexedDocuments")), string(response.get("error")));
    }

    private boolean bool(Object value) { return value instanceof Boolean b && b; }
    private int number(Object value) { return value instanceof Number n ? n.intValue() : 0; }
    private String string(Object value) { return value == null ? null : String.valueOf(value); }
    public record SyncResult(String requestId, boolean success, boolean reused, int indexedDocuments, String error) { }
}
