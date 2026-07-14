package com.mf.datacenter.knowledge;

import com.mf.datacenter.common.ApiResponse;
import com.mf.datacenter.knowledge.entity.ContentCandidateEntity;
import com.mf.datacenter.knowledge.entity.ContentPublishLogEntity;
import com.mf.datacenter.knowledge.entity.ContentSyncLogEntity;
import com.mf.datacenter.knowledge.entity.KnowledgeGapEntity;
import com.mf.datacenter.knowledge.entity.ResearchSourceEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/knowledge")
public class KnowledgeWorkbenchController {
    private final KnowledgeWorkbenchService service;
    public KnowledgeWorkbenchController(KnowledgeWorkbenchService service) { this.service = service; }
    @GetMapping("/gaps") public ApiResponse<List<KnowledgeGapEntity>> gaps() { return ApiResponse.ok(service.gaps()); }
    @PostMapping("/gaps/aggregate") public ApiResponse<Integer> aggregate() { return ApiResponse.ok(service.aggregateFromQuestionPool()); }
    @PostMapping("/gaps") public ApiResponse<KnowledgeGapEntity> create(@RequestBody KnowledgeWorkbenchService.GapRequest r) { return ApiResponse.ok(service.createGap(r)); }
    @PostMapping("/gaps/{id}/research") public ApiResponse<KnowledgeGapEntity> research(@PathVariable("id") Long id) { return ApiResponse.ok(service.research(id)); }
    @PostMapping("/gaps/{id}/ignore") public ApiResponse<KnowledgeGapEntity> ignore(@PathVariable("id") Long id) { return ApiResponse.ok(service.ignore(id)); }
    @GetMapping("/gaps/{id}/sources") public ApiResponse<List<ResearchSourceEntity>> sources(@PathVariable("id") Long id) { return ApiResponse.ok(service.sources(id)); }
    @PostMapping("/gaps/{id}/sources") public ApiResponse<ResearchSourceEntity> source(@PathVariable("id") Long id, @RequestBody KnowledgeWorkbenchService.SourceRequest r) { return ApiResponse.ok(service.addSource(id, r)); }
    @GetMapping("/candidates") public ApiResponse<List<ContentCandidateEntity>> candidates(@RequestParam(name = "gapId", required = false) Long gapId) { return ApiResponse.ok(service.candidates(gapId)); }
    @PostMapping("/gaps/{id}/candidates") public ApiResponse<ContentCandidateEntity> candidate(@PathVariable("id") Long id, @RequestBody KnowledgeWorkbenchService.CandidateRequest r) { return ApiResponse.ok(service.addCandidate(id, r)); }
    @PostMapping("/sample-candidates/{sampleId}/draft") public ApiResponse<KnowledgeWorkbenchService.SampleDraftResult> createFromSample(@PathVariable("sampleId") Long sampleId, @RequestBody KnowledgeWorkbenchService.SampleDraftRequest r) { return ApiResponse.ok(service.createFromSample(sampleId, r)); }
    @PostMapping("/candidates/{id}/mf-ep-draft") public ApiResponse<ContentCandidateEntity> createMfEpDraft(@PathVariable("id") Long id) { return ApiResponse.ok(service.createMfEpDraft(id)); }
    @PostMapping("/candidates/{id}/publish") public ApiResponse<ContentCandidateEntity> publish(@PathVariable("id") Long id, @RequestBody(required = false) Map<String, String> body) { return ApiResponse.ok(service.publish(id, body == null ? null : body.get("operator"))); }
    @PostMapping("/candidates/{id}/offline") public ApiResponse<ContentCandidateEntity> offline(@PathVariable("id") Long id, @RequestBody(required = false) Map<String, String> body) { return ApiResponse.ok(service.offline(id, body == null ? null : body.get("operator"))); }
    @GetMapping("/candidates/{id}/publish-logs") public ApiResponse<List<ContentPublishLogEntity>> publishLogs(@PathVariable("id") Long id) { return ApiResponse.ok(service.publishLogs(id)); }
    @GetMapping("/candidates/{id}/sync-logs") public ApiResponse<List<ContentSyncLogEntity>> syncLogs(@PathVariable("id") Long id) { return ApiResponse.ok(service.syncLogs(id)); }
    @PostMapping("/candidates/{id}/sync") public ApiResponse<ContentSyncLogEntity> retrySync(@PathVariable("id") Long id) { return ApiResponse.ok(service.retrySync(id)); }
    @PostMapping("/candidates/{id}/reject") public ApiResponse<ContentCandidateEntity> reject(@PathVariable("id") Long id) { return ApiResponse.ok(service.reject(id)); }
}
