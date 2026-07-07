package com.mf.agentservice.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mf.agentservice.api.AgentChatRequest;
import com.mf.agentservice.client.DataCenterClient;
import com.mf.agentservice.client.MfEpClient;
import com.mf.agentservice.rag.KnowledgeRetrievalService;
import com.mf.agentservice.tools.DataCenterTools;
import com.mf.agentservice.tools.EncyclopediaTools;
import com.mf.agentservice.tools.McpToolNames;
import com.mf.agentservice.tools.MerchantTools;
import com.mf.agentservice.tools.OrderTools;
import com.mf.agentservice.tools.ProductTools;
import com.mf.agentservice.tools.ToolExecutor;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class CustomerAgentServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void productQuestionCallsProductToolAndWritesDataCenterLogs() throws Exception {
        var mfEpClient = mock(MfEpClient.class);
        when(mfEpClient.searchProducts(eq("推荐几款适合果树的肥料"), eq(null), eq(1), eq(5)))
                .thenReturn(objectMapper.readTree("{\"records\":[]}"));

        var dataCenterClient = mock(DataCenterClient.class);
        when(dataCenterClient.logConversation(any())).thenReturn(42L);
        doNothing().when(dataCenterClient).logToolCall(any());
        doNothing().when(dataCenterClient).saveSampleCandidate(any());

        var service = service(mfEpClient, dataCenterClient);
        var response = service.chat(new AgentChatRequest("s1", "推荐几款适合果树的肥料", "u1", "client", null));

        assertThat(response.intent()).isEqualTo("product");
        assertThat(response.conversationId()).isEqualTo(42L);
        assertThat(response.usedTools()).extracting("name")
                .contains(McpToolNames.PRODUCT_SEARCH, McpToolNames.DATACENTER_LOG_CONVERSATION);
        verify(dataCenterClient).logConversation(any());
        verify(dataCenterClient, atLeastOnce()).logToolCall(any());
    }

    @Test
    void orderQuestionWithoutTokenDoesNotCallMfEpOrderApi() throws Exception {
        var mfEpClient = mock(MfEpClient.class);
        var dataCenterClient = mock(DataCenterClient.class);
        when(dataCenterClient.logConversation(any())).thenReturn(43L);
        doNothing().when(dataCenterClient).logToolCall(any());
        doNothing().when(dataCenterClient).reportUnresolved(any());

        var service = service(mfEpClient, dataCenterClient);
        var response = service.chat(new AgentChatRequest("s2", "我的订单 123 发货了吗", "u1", "client", null));

        assertThat(response.intent()).isEqualTo("order");
        assertThat(response.answer()).contains("请先登录");
        assertThat(response.usedTools()).extracting("name").contains(McpToolNames.ORDER_STATUS);
        verify(mfEpClient, org.mockito.Mockito.never()).orderStatus(eq(123L), any());
    }

    @SuppressWarnings("unchecked")
    private CustomerAgentService service(MfEpClient mfEpClient, DataCenterClient dataCenterClient) {
        var executor = new ToolExecutor();
        var provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return new CustomerAgentService(
                new IntentResolver(),
                new KnowledgeRetrievalService(),
                new ProductTools(mfEpClient, executor),
                new EncyclopediaTools(mfEpClient, executor),
                new OrderTools(mfEpClient, executor),
                new MerchantTools(executor),
                new DataCenterTools(dataCenterClient, executor),
                provider
        );
    }
}
