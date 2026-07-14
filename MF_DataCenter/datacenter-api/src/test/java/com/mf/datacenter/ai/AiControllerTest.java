package com.mf.datacenter.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiControllerTest {

    @TempDir
    static Path tempDir;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("datacenter.storage.ai-data-file", () -> tempDir.resolve("ai-store-test.json").toString());
    }

    @Test
    void shouldWriteAiDataAndUpdateAnalysisStats() throws Exception {
        var before = getAiAnalysis();
        var beforeConversations = before.get("conversationTotal").asInt();
        var beforeToolCalls = before.get("toolCallTotal").asInt();

        var conversationId = postAndReadId("/api/ai/conversations", """
                {
                  "source": "MF_AgentService",
                  "sessionId": "test-session",
                  "userId": "test-user",
                  "userType": "consumer",
                  "question": "接口测试咨询问题",
                  "answer": "接口测试回答",
                  "intent": "test",
                  "resolved": true,
                  "satisfaction": 5
                }
                """);

        postAndReadId("/api/ai/tool-calls", """
                {
                  "conversationId": %d,
                  "toolName": "knowledge_search",
                  "requestSummary": "test request",
                  "responseSummary": "test response",
                  "success": true,
                  "durationMs": 77
                }
                """.formatted(conversationId));

        mockMvc.perform(get("/api/ai/conversations/{id}/trace", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conversation.id").value(conversationId))
                .andExpect(jsonPath("$.data.toolCalls[0].toolName").value("knowledge_search"));

        var unresolvedId = postAndReadId("/api/ai/unresolved-questions", """
                {
                  "conversationId": %d,
                  "question": "接口测试未解决问题",
                  "reason": "test reason",
                  "status": "pending",
                  "owner": "ops",
                  "remark": "new"
                }
                """.formatted(conversationId));

        mockMvc.perform(patch("/api/ai/unresolved-questions/{id}/status", unresolvedId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "resolved",
                                  "owner": "ops",
                                  "remark": "done"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("resolved"));

        var sampleId = postAndReadId("/api/ai/sample-candidates", """
                {
                  "conversationId": %d,
                  "question": "接口测试样本问题",
                  "answer": "接口测试样本回答",
                  "source": "test",
                  "qualityStatus": "good",
                  "reviewStatus": "pending"
                }
                """.formatted(conversationId));

        mockMvc.perform(patch("/api/ai/sample-candidates/{id}/review", sampleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewStatus": "approved",
                                  "reviewer": "ops",
                                  "reviewRemark": "ok"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewStatus").value("approved"));

        var after = getAiAnalysis();
        assertThat(after.get("conversationTotal").asInt()).isEqualTo(beforeConversations + 1);
        assertThat(after.get("toolCallTotal").asInt()).isEqualTo(beforeToolCalls + 1);

        mockMvc.perform(get("/api/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cards[?(@.code == 'ai_conversation_total')]").isNotEmpty())
                .andExpect(jsonPath("$.data.latestAgentWrite.question").value("接口测试咨询问题"))
                .andExpect(jsonPath("$.data.governance.riskReasons").isArray());
    }

    @Test
    void shouldRejectInvalidReviewStatus() throws Exception {
        mockMvc.perform(post("/api/ai/sample-candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "状态校验测试",
                                  "answer": "回答",
                                  "source": "test",
                                  "qualityStatus": "good",
                                  "reviewStatus": "unknown"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    private JsonNode getAiAnalysis() throws Exception {
        var content = mockMvc.perform(get("/api/analysis/ai"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(content).get("data");
    }

    private long postAndReadId(String path, String body) throws Exception {
        var content = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(content).get("data").get("id").asLong();
    }
}
