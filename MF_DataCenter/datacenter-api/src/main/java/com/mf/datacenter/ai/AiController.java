package com.mf.datacenter.ai;

import com.mf.datacenter.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.LocalDateTime;
import com.mf.datacenter.common.PageResult;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiDataStore dataStore;
    private final HistoryAccessService historyAccessService;

    public AiController(AiDataStore dataStore, HistoryAccessService historyAccessService) {
        this.dataStore = dataStore;
        this.historyAccessService = historyAccessService;
    }

    @PostMapping("/conversations")
    public ApiResponse<AiRecords.Conversation> createConversation(@Valid @RequestBody AiRecords.CreateConversationRequest request) {
        return ApiResponse.ok(dataStore.addConversation(request));
    }

    @GetMapping("/conversations")
    public ApiResponse<List<AiRecords.Conversation>> conversations(
            @RequestParam(name = "source", required = false) String source,
            @RequestParam(name = "intent", required = false) String intent,
            @RequestParam(name = "startTime", required = false) LocalDateTime startTime,
            @RequestParam(name = "endTime", required = false) LocalDateTime endTime
    ) {
        return ApiResponse.ok(dataStore.conversations(source, intent, startTime, endTime));
    }

    @GetMapping("/conversations/{id}/trace")
    public ApiResponse<AiRecords.ConversationTrace> conversationTrace(@PathVariable("id") Long id) {
        return ApiResponse.ok(dataStore.conversationTrace(id));
    }

    @GetMapping("/conversations/page")
    public ApiResponse<PageResult<AiRecords.Conversation>> conversationPage(@RequestParam(name = "source", required = false) String source, @RequestParam(name = "intent", required = false) String intent,
                                                                              @RequestParam(name = "pageNo", defaultValue = "1") long pageNo, @RequestParam(name = "pageSize", defaultValue = "10") long pageSize) {
        return ApiResponse.ok(dataStore.conversationPage(source, intent, pageNo, pageSize));
    }

    @GetMapping("/my-conversations")
    public ApiResponse<AiRecords.UserConversationPage> myConversations(
            @RequestHeader(name = "X-MF-Internal-Token", required = false) String internalToken,
            @RequestHeader(name = "X-MF-User-Id", required = false) String userId,
            @RequestHeader(name = "X-MF-User-Type", required = false) String userType,
            @RequestHeader(name = "X-MF-Identity-Signature", required = false) String signature,
            @RequestParam(name = "sessionId") String sessionId,
            @RequestParam(name = "page", defaultValue = "1") long page,
            @RequestParam(name = "pageSize", defaultValue = "50") long pageSize
    ) {
        var identity = historyAccessService.verify(internalToken, userId, userType, signature);
        return ApiResponse.ok(dataStore.userConversations(identity.userId(), sessionId, page, pageSize));
    }

    @DeleteMapping("/my-conversations/{sessionId}")
    public ApiResponse<AiRecords.DeleteConversationResult> deleteMyConversations(
            @RequestHeader(name = "X-MF-Internal-Token", required = false) String internalToken,
            @RequestHeader(name = "X-MF-User-Id", required = false) String userId,
            @RequestHeader(name = "X-MF-User-Type", required = false) String userType,
            @RequestHeader(name = "X-MF-Identity-Signature", required = false) String signature,
            @PathVariable String sessionId
    ) {
        var identity = historyAccessService.verify(internalToken, userId, userType, signature);
        return ApiResponse.ok(dataStore.deleteUserConversations(identity.userId(), sessionId));
    }

    @PatchMapping("/conversations/{id}/satisfaction")
    public ApiResponse<AiRecords.Conversation> updateSatisfaction(@PathVariable Long id, @Valid @RequestBody AiRecords.UpdateSatisfactionRequest request) {
        return ApiResponse.ok(dataStore.updateSatisfaction(id, request.satisfaction()));
    }

    @PostMapping("/tool-calls")
    public ApiResponse<AiRecords.ToolCall> createToolCall(@Valid @RequestBody AiRecords.CreateToolCallRequest request) {
        return ApiResponse.ok(dataStore.addToolCall(request));
    }

    @GetMapping("/tool-calls")
    public ApiResponse<List<AiRecords.ToolCall>> toolCalls() {
        return ApiResponse.ok(dataStore.toolCalls());
    }

    @GetMapping("/tool-calls/page")
    public ApiResponse<PageResult<AiRecords.ToolCall>> toolCallPage(@RequestParam(name = "pageNo", defaultValue = "1") long pageNo, @RequestParam(name = "pageSize", defaultValue = "10") long pageSize) {
        return ApiResponse.ok(dataStore.toolCallPage(pageNo, pageSize));
    }

    @PostMapping("/unresolved-questions")
    public ApiResponse<AiRecords.UnresolvedQuestion> createUnresolved(@Valid @RequestBody AiRecords.CreateUnresolvedQuestionRequest request) {
        return ApiResponse.ok(dataStore.addUnresolved(request));
    }

    @GetMapping("/unresolved-questions")
    public ApiResponse<List<AiRecords.UnresolvedQuestion>> unresolvedQuestions(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        return ApiResponse.ok(dataStore.unresolvedQuestions(status, keyword));
    }

    @GetMapping("/unresolved-questions/page")
    public ApiResponse<PageResult<AiRecords.UnresolvedQuestion>> unresolvedPage(@RequestParam(name = "status", required = false) String status, @RequestParam(name = "keyword", required = false) String keyword,
                                                                                   @RequestParam(name = "pageNo", defaultValue = "1") long pageNo, @RequestParam(name = "pageSize", defaultValue = "10") long pageSize) {
        return ApiResponse.ok(dataStore.unresolvedPage(status, keyword, pageNo, pageSize));
    }

    @PatchMapping("/unresolved-questions/{id}/status")
    public ApiResponse<AiRecords.UnresolvedQuestion> updateUnresolved(
            @PathVariable("id") Long id,
            @Valid @RequestBody AiRecords.UpdateUnresolvedStatusRequest request
    ) {
        return ApiResponse.ok(dataStore.updateUnresolved(id, request));
    }

    @PostMapping("/sample-candidates")
    public ApiResponse<AiRecords.SampleCandidate> createSample(@Valid @RequestBody AiRecords.CreateSampleCandidateRequest request) {
        return ApiResponse.ok(dataStore.addSample(request));
    }

    @GetMapping("/sample-candidates")
    public ApiResponse<List<AiRecords.SampleCandidate>> sampleCandidates(
            @RequestParam(name = "reviewStatus", required = false) String reviewStatus,
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        return ApiResponse.ok(dataStore.sampleCandidates(reviewStatus, keyword));
    }

    @GetMapping("/sample-candidates/page")
    public ApiResponse<PageResult<AiRecords.SampleCandidate>> samplePage(@RequestParam(name = "reviewStatus", required = false) String reviewStatus, @RequestParam(name = "keyword", required = false) String keyword,
                                                                            @RequestParam(name = "pageNo", defaultValue = "1") long pageNo, @RequestParam(name = "pageSize", defaultValue = "10") long pageSize) {
        return ApiResponse.ok(dataStore.samplePage(reviewStatus, keyword, pageNo, pageSize));
    }

    @PatchMapping("/sample-candidates/{id}/review")
    public ApiResponse<AiRecords.SampleCandidate> reviewSample(
            @PathVariable("id") Long id,
            @Valid @RequestBody AiRecords.ReviewSampleRequest request
    ) {
        return ApiResponse.ok(dataStore.reviewSample(id, request));
    }
}
