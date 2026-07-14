package com.mf.agentservice.rag;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix = "mf.rag")
public record RagProperties(boolean enabled, String qdrantUrl, String embeddingBaseUrl, String collection, double scoreThreshold) {}
