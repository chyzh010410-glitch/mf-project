package com.mf.agentservice.rag;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
@Component
public class EmbeddingClient {
  private final RagProperties properties;
  public EmbeddingClient(RagProperties properties) { this.properties = properties; }
  public List<Double> embed(String text) {
    JsonNode node = RestClient.create(properties.embeddingBaseUrl()).post().uri("/embed").body(java.util.Map.of("texts", List.of(text))).retrieve().body(JsonNode.class);
    if (node == null || !node.path("embeddings").isArray() || node.path("embeddings").isEmpty()) throw new IllegalStateException("Empty embedding response");
    List<Double> vector = new ArrayList<>();
    node.path("embeddings").get(0).forEach(value -> vector.add(value.asDouble()));
    return vector;
  }
}
