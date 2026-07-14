package com.mf.fertilizer.ai.controller.internal;

import com.mf.fertilizer.ai.dto.AiContentDraftDTO;
import com.mf.fertilizer.ai.dto.AiContentReviewDTO;
import com.mf.fertilizer.ai.dto.AiContentRollbackDTO;
import com.mf.fertilizer.ai.entity.AiContentDraft;
import com.mf.fertilizer.ai.entity.AiContentDraftVersion;
import com.mf.fertilizer.ai.entity.AiContentSyncEvent;
import com.mf.fertilizer.ai.service.InternalAiContentApplicationService;
import com.mf.fertilizer.vo.ResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/ai-content")
public class InternalAiContentController {

    private final InternalAiContentApplicationService internalAiContentApplicationService;

    @Value("${mf.internal-token:change-me}")
    private String internalToken;

    @PostMapping("/drafts")
    public ResultVO<AiContentDraft> createDraft(@RequestHeader("X-MF-Internal-Token") String token,
                                                @Valid @RequestBody AiContentDraftDTO dto) {
        verifyToken(token);
        return ResultVO.success(internalAiContentApplicationService.createDraft(dto));
    }

    @PutMapping("/drafts/{id}")
    public ResultVO<AiContentDraft> updateDraft(@RequestHeader("X-MF-Internal-Token") String token,
                                                @PathVariable Long id,
                                                @Valid @RequestBody AiContentDraftDTO dto) {
        verifyToken(token);
        return ResultVO.success(internalAiContentApplicationService.updateDraft(id, dto));
    }

    @PostMapping("/drafts/{id}/publish")
    public ResultVO<AiContentDraft> publishDraft(@RequestHeader("X-MF-Internal-Token") String token,
                                                 @PathVariable Long id,
                                                 @RequestBody(required = false) Map<String, String> body) {
        verifyToken(token);
        return ResultVO.success(internalAiContentApplicationService.publishDraft(id, operator(body)));
    }

    @PostMapping("/{id}/offline")
    public ResultVO<AiContentDraft> offline(@RequestHeader("X-MF-Internal-Token") String token,
                                            @PathVariable Long id,
                                            @RequestBody(required = false) Map<String, String> body) {
        verifyToken(token);
        return ResultVO.success(internalAiContentApplicationService.offlineContent(id, operator(body)));
    }

    @PostMapping("/drafts/{id}/review")
    public ResultVO<AiContentDraft> review(@RequestHeader("X-MF-Internal-Token") String token,
                                           @PathVariable Long id,
                                           @Valid @RequestBody AiContentReviewDTO dto) {
        verifyToken(token);
        return ResultVO.success(internalAiContentApplicationService.reviewDraft(id, dto));
    }

    @PostMapping("/drafts/{id}/rollback")
    public ResultVO<AiContentDraft> rollback(@RequestHeader("X-MF-Internal-Token") String token,
                                             @PathVariable Long id,
                                             @Valid @RequestBody AiContentRollbackDTO dto) {
        verifyToken(token);
        return ResultVO.success(internalAiContentApplicationService.rollbackDraft(id, dto));
    }

    @GetMapping("/{id}")
    public ResultVO<AiContentDraft> get(@RequestHeader("X-MF-Internal-Token") String token,
                                        @PathVariable Long id) {
        verifyToken(token);
        return ResultVO.success(internalAiContentApplicationService.getContent(id));
    }

    @GetMapping("/drafts/{id}/versions")
    public ResultVO<List<AiContentDraftVersion>> versions(@RequestHeader("X-MF-Internal-Token") String token,
                                                           @PathVariable Long id) {
        verifyToken(token);
        return ResultVO.success(internalAiContentApplicationService.listVersions(id));
    }

    @GetMapping("/sync-events")
    public ResultVO<List<AiContentSyncEvent>> pendingSyncEvents(@RequestHeader("X-MF-Internal-Token") String token,
                                                                 @org.springframework.web.bind.annotation.RequestParam(required = false) Integer limit) {
        verifyToken(token);
        return ResultVO.success(internalAiContentApplicationService.listPendingSyncEvents(limit));
    }

    @GetMapping("/sync-events/{id}")
    public ResultVO<AiContentSyncEvent> syncEvent(@RequestHeader("X-MF-Internal-Token") String token,
                                                   @PathVariable Long id) {
        verifyToken(token);
        return ResultVO.success(internalAiContentApplicationService.getSyncEvent(id));
    }

    @PostMapping("/sync-events/{id}/ack")
    public ResultVO<Void> acknowledgeSyncEvent(@RequestHeader("X-MF-Internal-Token") String token,
                                                @PathVariable Long id,
                                                @RequestBody(required = false) Map<String, String> body) {
        verifyToken(token);
        internalAiContentApplicationService.acknowledgeSyncEvent(id, body == null ? null : body.get("consumer"));
        return ResultVO.success();
    }

    @PostMapping("/sync-events/{id}/failure")
    public ResultVO<Void> recordSyncEventFailure(@RequestHeader("X-MF-Internal-Token") String token,
                                                  @PathVariable Long id,
                                                  @RequestBody Map<String, String> body) {
        verifyToken(token);
        internalAiContentApplicationService.recordSyncEventFailure(
                id,
                body.get("consumer"),
                body.get("state"),
                parseFailureAttempts(body.get("failureAttempts")),
                body.get("reason")
        );
        return ResultVO.success();
    }

    @PostMapping("/sync-events/{id}/retry")
    public ResultVO<Void> retrySyncEvent(@RequestHeader("X-MF-Internal-Token") String token,
                                          @PathVariable Long id,
                                          @RequestBody(required = false) Map<String, String> body) {
        verifyToken(token);
        internalAiContentApplicationService.retrySyncEvent(id, operator(body));
        return ResultVO.success();
    }

    private void verifyToken(String token) {
        if (!internalToken.equals(token)) {
            throw new com.mf.fertilizer.exception.BusinessException(403, "内部服务凭据无效");
        }
    }

    private String operator(Map<String, String> body) {
        if (body == null) {
            return "datacenter";
        }
        return body.getOrDefault("operator", "datacenter");
    }

    private Integer parseFailureAttempts(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new com.mf.fertilizer.exception.BusinessException(400, "failureAttempts必须是整数");
        }
    }
}
