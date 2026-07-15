package com.mf.agentservice.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mf.agentservice.agent.CustomerAgentService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AgentController.class)
class AgentControllerWebTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerAgentService customerAgentService;

    @Test
    void keepsTheChatEndpointAndResponseShape() throws Exception {
        when(customerAgentService.chat(any())).thenReturn(new AgentChatResponse(
                "受控回答", "product", true, List.of(), 1L, null, List.of(), 85, false, null));

        mockMvc.perform(post("/api/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"test\",\"message\":\"搜索苹果苗\",\"userType\":\"client\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("受控回答"))
                .andExpect(jsonPath("$.intent").value("product"))
                .andExpect(jsonPath("$.conversationId").value(1));
    }
}
