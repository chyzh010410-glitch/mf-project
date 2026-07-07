package com.mf.fertilizer.ai.client;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;

@Component
public class AiClient {

    private final RestTemplate rest;
    private static final String AI_BASE = "http://localhost:5000/api";

    public AiClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(120000);
        this.rest = new RestTemplate(factory);
    }

    public Map<?, ?> chat(String question) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var body = Map.of("question", question);
        return rest.postForObject(AI_BASE + "/chat", new HttpEntity<>(body, headers), Map.class);
    }

    public Map<?, ?> draftArticle(String topic, String category) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var body = Map.of("topic", topic, "category", category != null ? category : "");
        return rest.postForObject(AI_BASE + "/article/draft", new HttpEntity<>(body, headers), Map.class);
    }

    public Map<?, ?> draftEncyclopedia(String name) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var body = Map.of("name", name);
        return rest.postForObject(AI_BASE + "/encyclopedia/draft", new HttpEntity<>(body, headers), Map.class);
    }

    public Map<?, ?> rebuildKnowledge() {
        return rest.postForObject(AI_BASE + "/knowledge/rebuild", null, Map.class);
    }
}
