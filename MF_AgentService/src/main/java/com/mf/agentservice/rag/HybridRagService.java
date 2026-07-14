package com.mf.agentservice.rag;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
@Service
public class HybridRagService {
  private final KnowledgeRetrievalService keyword; private final EmbeddingClient embedding; private final RagProperties properties;
  public HybridRagService(KnowledgeRetrievalService keyword, EmbeddingClient embedding, RagProperties properties) { this.keyword=keyword; this.embedding=embedding; this.properties=properties; }
  public List<RagDocument> retrieve(String query) {
    List<RagDocument> fallback = keyword.retrieve(query);
    if (!properties.enabled()) return fallback;
    try { List<Double> vector=embedding.embed(query); JsonNode response=RestClient.create(properties.qdrantUrl()).post().uri("/collections/{collection}/points/query",properties.collection()).body(Map.of("query",vector,"limit",6,"with_payload",true)).retrieve().body(JsonNode.class); Map<String,RagDocument> merged=new LinkedHashMap<>(); for(RagDocument d:fallback) merged.put(d.sourceId(),d); for(JsonNode hit: response.path("result").path("points")) { if(hit.path("score").asDouble()<properties.scoreThreshold()) continue; JsonNode p=hit.path("payload"); RagDocument d=new RagDocument(p.path("type").asText(),p.path("id").asText(),p.path("title").asText(),p.path("content").asText(),List.of()); merged.putIfAbsent(d.sourceId(),d); } return merged.values().stream().limit(3).toList(); } catch(RuntimeException ex) { return fallback; }
  }
  public int reindex() {
    if(!properties.enabled()) throw new IllegalStateException("MF_RAG_ENABLED is false");
    List<RagDocument> docs=keyword.documents(); if(docs.isEmpty()) return 0; int size=embedding.embed(docs.get(0).title()+"\n"+docs.get(0).content()).size(); RestClient client=RestClient.create(properties.qdrantUrl()); client.put().uri("/collections/{collection}",properties.collection()).body(Map.of("vectors",Map.of("size",size,"distance","Cosine"))).retrieve().toBodilessEntity(); List<Map<String,Object>> points=new ArrayList<>(); for(RagDocument d:docs){ points.add(Map.of("id",UUID.nameUUIDFromBytes(d.sourceId().getBytes(StandardCharsets.UTF_8)).toString(),"vector",embedding.embed(d.title()+"\n"+d.content()),"payload",Map.of("type",d.sourceType(),"id",d.sourceId(),"title",d.title(),"content",d.content()))); } client.put().uri("/collections/{collection}/points",properties.collection()).body(Map.of("points",points)).retrieve().toBodilessEntity(); return docs.size();
  }
}
