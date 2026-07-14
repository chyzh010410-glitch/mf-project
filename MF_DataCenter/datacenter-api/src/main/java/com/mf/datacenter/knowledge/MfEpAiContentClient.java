package com.mf.datacenter.knowledge;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MfEpAiContentClient {
    private final RestClient client;
    private final String token;

    public MfEpAiContentClient(@Value("${datacenter.mf-ep.ai-content.base-url:http://localhost:8080}") String baseUrl,
                               @Value("${datacenter.mf-ep.ai-content.internal-token:change-me}") String token) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.token = token;
    }

    public MfEpDraft createDraft(Map<String, Object> request) { return call("/internal/ai-content/drafts", request); }
    public MfEpDraft publish(long draftId, String operator) { return call("/internal/ai-content/drafts/" + draftId + "/publish", Map.of("operator", operator)); }
    public MfEpDraft offline(long draftId, String operator) { return call("/internal/ai-content/" + draftId + "/offline", Map.of("operator", operator)); }

    @SuppressWarnings("unchecked")
    public List<SyncEvent> pendingSyncEvents() {
        Map<String, Object> response = client.get().uri("/internal/ai-content/sync-events?limit=100").header("X-MF-Internal-Token", token).retrieve().body(Map.class);
        if (response == null || !Integer.valueOf(200).equals(response.get("code")) || !(response.get("data") instanceof List<?> rows)) throw new IllegalStateException(response == null ? "MF_EP 未返回同步事件" : String.valueOf(response.get("msg")));
        return rows.stream().filter(Map.class::isInstance).map(Map.class::cast).map(row -> new SyncEvent(number(row.get("id")), number(row.get("draftId")), String.valueOf(row.get("action")))).toList();
    }

    @SuppressWarnings("unchecked")
    public SyncEventStatus syncEvent(long eventId) {
        Map<String, Object> response = client.get().uri("/internal/ai-content/sync-events/" + eventId).header("X-MF-Internal-Token", token).retrieve().body(Map.class);
        if (response == null || !Integer.valueOf(200).equals(response.get("code")) || !(response.get("data") instanceof Map<?, ?> data)) throw new IllegalStateException(response == null ? "MF_EP 未返回同步事件" : String.valueOf(response.get("msg")));
        return new SyncEventStatus(number(data.get("id")), text(data.get("deliveryStatus")), text(data.get("consumer")), text(data.get("acknowledgedAt")), text(data.get("lastFailureReason")));
    }

    @SuppressWarnings("unchecked")
    private MfEpDraft call(String path, Map<String, Object> body) {
        Map<String, Object> response = client.post().uri(path).header("X-MF-Internal-Token", token)
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(Map.class);
        if (response == null || !Integer.valueOf(200).equals(response.get("code")) || !(response.get("data") instanceof Map<?, ?> data)) {
            throw new IllegalStateException(response == null ? "MF_EP 未返回草稿结果" : String.valueOf(response.get("msg")));
        }
        return new MfEpDraft(number(data.get("id")), number(data.get("mfEpContentId")), String.valueOf(data.get("status")));
    }

    private Long number(Object value) { return value instanceof Number n ? n.longValue() : value == null ? null : Long.valueOf(String.valueOf(value)); }
    private String text(Object value) { return value == null ? null : String.valueOf(value); }
    public record MfEpDraft(Long id, Long contentId, String status) { }
    public record SyncEvent(Long id, Long draftId, String action) { }
    public record SyncEventStatus(Long id, String deliveryStatus, String consumer, String acknowledgedAt, String lastFailureReason) { }
}
