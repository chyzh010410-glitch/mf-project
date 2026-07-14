package com.mf.agentservice.api;
import com.mf.agentservice.rag.HybridRagService;
import com.mf.agentservice.rag.KnowledgeRetrievalService;
import com.mf.agentservice.rag.RagProperties;
import com.mf.agentservice.rag.KnowledgeSyncService;
import com.mf.agentservice.rag.KnowledgeSyncEventConsumer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/agent/knowledge")
public class KnowledgeController {
  private final HybridRagService hybrid; private final KnowledgeRetrievalService keyword; private final RagProperties properties; private final KnowledgeSyncService syncService; private final KnowledgeSyncEventConsumer eventConsumer; private final String key; private final String syncKey;
  public KnowledgeController(HybridRagService hybrid, KnowledgeRetrievalService keyword, RagProperties properties, KnowledgeSyncService syncService, KnowledgeSyncEventConsumer eventConsumer, @Value("${mf.agent.evaluation-key:}") String key, @Value("${mf.agent.knowledge-sync-key:}") String syncKey) { this.hybrid=hybrid; this.keyword=keyword; this.properties=properties; this.syncService=syncService; this.eventConsumer=eventConsumer; this.key=key; this.syncKey=syncKey; }
  @GetMapping("/status") public Map<String,Object> status() { return Map.of("enabled",properties.enabled(),"collection",properties.collection(),"keywordDocuments",keyword.documents().size()); }
  @PostMapping({"/reindex","/sync"}) public Map<String,Object> reindex(@RequestHeader(name="X-MF-Evaluation-Key") String requestKey) { if(key.isBlank()||!key.equals(requestKey)) throw new ResponseStatusException(HttpStatus.FORBIDDEN); return Map.of("indexedDocuments",hybrid.reindex()); }
  @PostMapping("/internal-sync") public KnowledgeSyncService.SyncResult internalSync(@RequestHeader(name="X-MF-Internal-Token") String requestKey, @Valid @RequestBody SyncRequest request) { if(syncKey.isBlank()||!syncKey.equals(requestKey)) throw new ResponseStatusException(HttpStatus.FORBIDDEN); return syncService.sync(request.requestId(), request.contentIds()); }
  @GetMapping("/sync-events/status") public List<KnowledgeSyncEventConsumer.SyncEventStatus> syncEventStatus(@RequestHeader(name="X-MF-Internal-Token") String requestKey) { verifySyncKey(requestKey); return eventConsumer.statuses(); }
  @PostMapping("/sync-events/retry-failed") public List<KnowledgeSyncEventConsumer.EventSyncResult> retryFailedEvents(@RequestHeader(name="X-MF-Internal-Token") String requestKey) { verifySyncKey(requestKey); return eventConsumer.retryFailedEvents(); }
  private void verifySyncKey(String requestKey) { if(syncKey.isBlank()||!syncKey.equals(requestKey)) throw new ResponseStatusException(HttpStatus.FORBIDDEN); }
  public record SyncRequest(@NotBlank String requestId, List<Long> contentIds) { }
}
