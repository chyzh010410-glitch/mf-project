package com.mf.datacenter.ai;

import com.mf.datacenter.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiDataStore dataStore;

    public AiController(AiDataStore dataStore) {
        this.dataStore = dataStore;
    }

    @PostMapping("/conversations")
    public ApiResponse<AiRecords.Conversation> createConversation(@Valid @RequestBody AiRecords.CreateConversationRequest request) {
        return ApiResponse.ok(dataStore.addConversation(request));
    }

    @GetMapping("/conversations")
    public ApiResponse<List<AiRecords.Conversation>> conversations() {
        return ApiResponse.ok(dataStore.conversations());
    }

    @PostMapping("/tool-calls")
    public ApiResponse<AiRecords.ToolCall> createToolCall(@Valid @RequestBody AiRecords.CreateToolCallRequest request) {
        return ApiResponse.ok(dataStore.addToolCall(request));
    }

    @GetMapping("/tool-calls")
    public ApiResponse<List<AiRecords.ToolCall>> toolCalls() {
        return ApiResponse.ok(dataStore.toolCalls());
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

    @PatchMapping("/sample-candidates/{id}/review")
    public ApiResponse<AiRecords.SampleCandidate> reviewSample(
            @PathVariable("id") Long id,
            @Valid @RequestBody AiRecords.ReviewSampleRequest request
    ) {
        return ApiResponse.ok(dataStore.reviewSample(id, request));
    }
}
