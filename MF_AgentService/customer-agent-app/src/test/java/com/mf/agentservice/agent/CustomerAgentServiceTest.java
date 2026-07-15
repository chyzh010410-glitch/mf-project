package com.mf.agentservice.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mf.agentservice.api.AgentChatRequest;
import com.mf.agentservice.client.DataCenterClient;
import com.mf.agentservice.config.MfAgentProperties;
import com.mf.agentservice.client.MfEpClient;
import com.mf.agentservice.rag.KnowledgeRetrievalService;
import com.mf.agentservice.rag.HybridRagService;
import com.mf.agentservice.rag.EmbeddingClient;
import com.mf.agentservice.rag.RagProperties;
import com.mf.agentservice.tools.DataCenterTools;
import com.mf.agentservice.tools.EncyclopediaTools;
import com.mf.agentservice.tools.McpToolNames;
import com.mf.agentservice.tools.MerchantTools;
import com.mf.agentservice.tools.OrderTools;
import com.mf.agentservice.tools.ProductTools;
import com.mf.agentservice.tools.ToolExecutor;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.web.client.ResourceAccessException;

class CustomerAgentServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void platformWhatDoesItSellIsAProductQuestion() {
        assertThat(new IntentResolver().resolve("这个平台都卖什么东西")).isEqualTo(AgentIntent.PRODUCT);
    }

    @Test
    void productQuestionCallsProductToolAndWritesDataCenterLogs() throws Exception {
        var mfEpClient = mock(MfEpClient.class);
        when(mfEpClient.searchProducts(eq("推荐几款适合果树的肥料"), eq(null), eq(1), eq(5)))
                .thenReturn(objectMapper.readTree("{\"records\":[{\"id\":1,\"name\":\"果树营养肥\",\"price\":88.00,\"unit\":\"袋\"},{\"id\":2,\"name\":\"膨果肥\",\"price\":99,\"unit\":\"袋\"}]}"));

        var dataCenterClient = mock(DataCenterClient.class);
        when(dataCenterClient.logConversation(any())).thenReturn(42L);
        doNothing().when(dataCenterClient).logToolCall(any());
        doNothing().when(dataCenterClient).saveSampleCandidate(any());

        var service = service(mfEpClient, dataCenterClient);
        var response = service.chat(new AgentChatRequest("s1", "推荐几款适合果树的肥料", "u1", "client", null));

        assertThat(response.intent()).isEqualTo("product");
        assertThat(response.answer()).contains("2 款", "果树营养肥");
        assertThat(response.answer()).contains("果树营养肥（¥88/袋）", "膨果肥（¥99/袋）");
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

    @Test
    void knowledgeQuestionIncludesPublicEncyclopediaGuidance() throws Exception {
        var mfEpClient = mock(MfEpClient.class);
        when(mfEpClient.searchEncyclopedia(eq("苹果树腐烂病怎么处理"), eq(1), eq(5)))
                .thenReturn(objectMapper.readTree("{\"records\":[{\"name\":\"苹果树\",\"careGuide\":\"及时清理病枝并保持果园通风\"}]}"));
        var dataCenterClient = mock(DataCenterClient.class);
        when(dataCenterClient.logConversation(any())).thenReturn(44L);
        doNothing().when(dataCenterClient).logToolCall(any());
        doNothing().when(dataCenterClient).saveSampleCandidate(any());

        var response = service(mfEpClient, dataCenterClient)
                .chat(new AgentChatRequest("s3", "苹果树腐烂病怎么处理", "u1", "client", null));

        assertThat(response.intent()).isEqualTo("encyclopedia");
        assertThat(response.answer()).contains("苹果树腐烂病", "及时清理病枝并保持果园通风");
        assertThat(response.usedTools()).extracting("name").contains(McpToolNames.ENCYCLOPEDIA_SEARCH);
    }

    @Test
    void upstreamFailureIsObservableAndDoesNotProduceProductData() {
        var mfEpClient = mock(MfEpClient.class);
        when(mfEpClient.searchProducts(eq("推荐果树肥"), eq(null), eq(1), eq(5)))
                .thenThrow(new ResourceAccessException("read timed out", new SocketTimeoutException("read timed out")));
        var dataCenterClient = mock(DataCenterClient.class);
        when(dataCenterClient.logConversation(any())).thenReturn(45L);
        doNothing().when(dataCenterClient).logToolCall(any());
        doNothing().when(dataCenterClient).reportUnresolved(any());

        var response = service(mfEpClient, dataCenterClient)
                .chat(new AgentChatRequest("s4", "推荐果树肥", "u1", "client", null));

        assertThat(response.resolved()).isFalse();
        assertThat(response.fallbackReason()).isEqualTo("upstream_timeout");
        assertThat(response.answer()).contains("查询响应超时").doesNotContain("¥");
        assertThat(response.usedTools()).filteredOn(tool -> "product.search".equals(tool.name()))
                .allMatch(tool -> "upstream_timeout".equals(tool.failureReason()));
    }

    @Test
    void dataCenterFailureDoesNotDiscardAnswerAndIsObservable() throws Exception {
        var mfEpClient = mock(MfEpClient.class);
        when(mfEpClient.searchProducts(eq("推荐果树肥"), eq(null), eq(1), eq(5)))
                .thenReturn(objectMapper.readTree("{\"records\":[{\"name\":\"果树营养肥\",\"price\":88,\"unit\":\"袋\"}]}"));
        var dataCenterClient = mock(DataCenterClient.class);
        when(dataCenterClient.logConversation(any())).thenThrow(new ResourceAccessException("connection refused"));

        var response = service(mfEpClient, dataCenterClient)
                .chat(new AgentChatRequest("s5", "推荐果树肥", "u1", "client", null));

        assertThat(response.answer()).contains("果树营养肥");
        assertThat(response.conversationId()).isNull();
        assertThat(response.resolved()).isFalse();
        assertThat(response.fallbackReason()).isEqualTo("datacenter_log_failed");
        assertThat(response.usedTools()).filteredOn(tool -> "datacenter.logConversation".equals(tool.name()))
                .allMatch(tool -> "upstream_unavailable".equals(tool.failureReason()));
    }

    @Test
    void completeNewQuestionDoesNotInheritPreviousKnowledgeIntent() throws Exception {
        var mfEpClient = mock(MfEpClient.class);
        when(mfEpClient.searchEncyclopedia(any(), eq(1), eq(5)))
                .thenReturn(objectMapper.readTree("{\"records\":[]}"));
        var dataCenterClient = mock(DataCenterClient.class);
        when(dataCenterClient.logConversation(any())).thenReturn(46L);
        doNothing().when(dataCenterClient).logToolCall(any());
        doNothing().when(dataCenterClient).saveSampleCandidate(any());

        var service = service(mfEpClient, dataCenterClient);
        service.chat(new AgentChatRequest("s6", "苹果树黄叶怎么办", "u1", "client", null));
        var response = service.chat(new AgentChatRequest("s6", "你是哪个公司的客服", "u1", "client", null));

        assertThat(response.intent()).isEqualTo("company");
        assertThat(response.answer()).contains("苗丰施肥平台");
    }

    @Test
    void knowledgeProfileSupplementKeepsPreviousKnowledgeTopic() throws Exception {
        var mfEpClient = mock(MfEpClient.class);
        when(mfEpClient.searchEncyclopedia(any(), eq(1), eq(5)))
                .thenReturn(objectMapper.readTree("{\"records\":[]}"));
        var dataCenterClient = mock(DataCenterClient.class);
        when(dataCenterClient.logConversation(any())).thenReturn(47L);
        doNothing().when(dataCenterClient).logToolCall(any());
        doNothing().when(dataCenterClient).saveSampleCandidate(any());

        var service = service(mfEpClient, dataCenterClient);
        service.chat(new AgentChatRequest("s7", "果树冬剪五要点", "u1", "client", null));
        service.chat(new AgentChatRequest("s7", "陕西、五年的苹果树", "u1", "client", null));

        verify(mfEpClient).searchEncyclopedia(argThat(query -> query.contains("果树冬剪五要点")
                && query.contains("陕西") && query.contains("苹果树")), eq(1), eq(5));
    }

    @Test
    void shortPlantReplyAfterPlatformIntroductionSearchesProducts() throws Exception {
        var mfEpClient = mock(MfEpClient.class);
        when(mfEpClient.searchProducts(eq(""), eq(null), eq(1), eq(5)))
                .thenReturn(objectMapper.readTree("{\"records\":[{\"id\":1,\"name\":\"果树苗\",\"price\":58,\"unit\":\"株\"}]}"));
        when(mfEpClient.searchProducts(eq("苹果树"), eq(null), eq(1), eq(5)))
                .thenReturn(objectMapper.readTree("{\"records\":[{\"id\":1,\"name\":\"苹果树苗\",\"price\":68,\"unit\":\"株\"}]}"));
        var dataCenterClient = mock(DataCenterClient.class);
        when(dataCenterClient.logConversation(any())).thenReturn(49L);
        doNothing().when(dataCenterClient).logToolCall(any());
        doNothing().when(dataCenterClient).saveSampleCandidate(any());

        var service = service(mfEpClient, dataCenterClient);
        service.chat(new AgentChatRequest("s9", "这个平台都卖什么东西", "u1", "client", null));
        var response = service.chat(new AgentChatRequest("s9", "种苹果树", "u1", "client", null));

        assertThat(response.intent()).isEqualTo("product");
        assertThat(response.answer()).contains("苹果树苗");
        verify(mfEpClient).searchProducts(eq("苹果树"), eq(null), eq(1), eq(5));
    }

    @Test
    void productAnswerUsesModelToNaturallyPresentVerifiedToolResults() throws Exception {
        var mfEpClient = mock(MfEpClient.class);
        when(mfEpClient.searchProducts(eq(""), eq(null), eq(1), eq(5)))
                .thenReturn(objectMapper.readTree("{\"records\":[{\"id\":1,\"name\":\"果树苗\",\"price\":58,\"unit\":\"株\"}]}"));
        when(mfEpClient.searchProducts(eq("苹果树"), eq(null), eq(1), eq(5)))
                .thenReturn(objectMapper.readTree("{\"records\":[{\"id\":1,\"name\":\"苹果树苗\",\"price\":68,\"unit\":\"株\"}]}"));
        var dataCenterClient = mock(DataCenterClient.class);
        when(dataCenterClient.logConversation(any())).thenReturn(50L);
        doNothing().when(dataCenterClient).logToolCall(any());
        doNothing().when(dataCenterClient).saveSampleCandidate(any());
        AiCompletionGateway model = (prompt, message) -> Optional.of("如果你准备种苹果树，可以先看看当前在售的苹果树苗，再告诉我地区和预算。");

        var service = service(mfEpClient, dataCenterClient, model);
        service.chat(new AgentChatRequest("s10", "这个平台都卖什么东西", "u1", "client", null));
        var response = service.chat(new AgentChatRequest("s10", "种苹果树", "u1", "client", null));

        assertThat(response.answer()).contains("苹果树苗");
    }

    @Test
    void knowledgeGapIsReturnedAndAddedToExistingProblemPoolRemark() throws Exception {
        var mfEpClient = mock(MfEpClient.class);
        when(mfEpClient.searchEncyclopedia(any(), eq(1), eq(5)))
                .thenReturn(objectMapper.readTree("{\"records\":[]}"));
        var dataCenterClient = mock(DataCenterClient.class);
        when(dataCenterClient.logConversation(any())).thenReturn(48L);
        doNothing().when(dataCenterClient).logToolCall(any());
        doNothing().when(dataCenterClient).reportUnresolved(any());

        var response = service(mfEpClient, dataCenterClient)
                .chat(new AgentChatRequest("s8", "苹果树炭疽病怎么处理", "u1", "client", null));

        assertThat(response.fallbackReason()).isEqualTo("knowledge_not_enough");
        assertThat(response.knowledgeGap()).isNotNull();
        assertThat(response.knowledgeGap().riskLevel()).isEqualTo("medium");
        verify(dataCenterClient).reportUnresolved(argThat(body -> body.get("remark").toString().contains("topic=苹果树炭疽病怎么处理")
                && body.get("remark").toString().contains("suggested=faq,article,encyclopedia")));
    }

    @Test
    void pendingLogisticsContractDoesNotCallOrderApi() {
        var mfEpClient = mock(MfEpClient.class);
        var dataCenterClient = mock(DataCenterClient.class);
        when(dataCenterClient.logConversation(any())).thenReturn(51L);
        doNothing().when(dataCenterClient).logToolCall(any());
        doNothing().when(dataCenterClient).reportUnresolved(any());

        var response = service(mfEpClient, dataCenterClient)
                .chat(new AgentChatRequest("contract-1", "物流到哪了", "u1", "client", "token"));

        assertThat(response.answer()).isEqualTo("订单尚未发货或暂无物流信息。");
        assertThat(response.fallbackReason()).isEqualTo("contract_pending_data");
        verify(mfEpClient, org.mockito.Mockito.never()).orderStatus(any(), any());
    }

    @Test
    void unsupportedAddressChangeUsesContractFallback() {
        var mfEpClient = mock(MfEpClient.class);
        var dataCenterClient = mock(DataCenterClient.class);
        when(dataCenterClient.logConversation(any())).thenReturn(52L);
        doNothing().when(dataCenterClient).logToolCall(any());
        doNothing().when(dataCenterClient).reportUnresolved(any());

        var response = service(mfEpClient, dataCenterClient)
                .chat(new AgentChatRequest("contract-2", "改收货地址", "u1", "client", "token"));

        assertThat(response.answer()).isEqualTo("暂不支持通过客服修改，请在订单页面或联系人工客服。");
        assertThat(response.fallbackReason()).isEqualTo("contract_unsupported");
    }

    @Test
    void clarificationStopsAfterTwoRoundsAndReportsUnresolved() {
        var mfEpClient = mock(MfEpClient.class);
        var dataCenterClient = mock(DataCenterClient.class);
        when(dataCenterClient.logConversation(any())).thenReturn(53L, 54L, 55L);
        doNothing().when(dataCenterClient).logToolCall(any());
        doNothing().when(dataCenterClient).reportUnresolved(any());

        var service = service(mfEpClient, dataCenterClient);
        var first = service.chat(new AgentChatRequest("contract-3", "这个怎么样", "u1", "client", null));
        var second = service.chat(new AgentChatRequest("contract-3", "这个怎么样", "u1", "client", null));
        var third = service.chat(new AgentChatRequest("contract-3", "这个怎么样", "u1", "client", null));

        assertThat(first.answer()).isEqualTo("你想问树苗、肥料、订单还是种植？");
        assertThat(second.answer()).isEqualTo("你想问树苗、肥料、订单还是种植？");
        assertThat(third.answer()).isEqualTo("暂时无法可靠回答，已建议转人工或记录到未解决问题池。");
        assertThat(third.fallbackReason()).isEqualTo("knowledge_not_enough");
        verify(dataCenterClient).reportUnresolved(any());
    }

    @Test
    void outOfScopeQuestionUsesPlatformBoundary() {
        var mfEpClient = mock(MfEpClient.class);
        var dataCenterClient = mock(DataCenterClient.class);
        when(dataCenterClient.logConversation(any())).thenReturn(56L);
        doNothing().when(dataCenterClient).logToolCall(any());

        var response = service(mfEpClient, dataCenterClient)
                .chat(new AgentChatRequest("contract-4", "帮我看病", "u1", "client", null));

        assertThat(response.intent()).isEqualTo("out_of_scope");
        assertThat(response.answer()).isEqualTo("该问题超出平台范围，建议咨询相应专业人士。");
        assertThat(response.resolved()).isTrue();
    }

    @Test
    void confirmedProductSearchUsesContractRouteAndDoesNotLetModelInventStock() throws Exception {
        var mfEpClient = mock(MfEpClient.class);
        when(mfEpClient.searchProducts(eq("苹果苗"), eq(null), eq(1), eq(5)))
                .thenReturn(objectMapper.readTree("{\"records\":[]}"));
        var dataCenterClient = mock(DataCenterClient.class);
        when(dataCenterClient.logConversation(any())).thenReturn(57L);
        doNothing().when(dataCenterClient).logToolCall(any());
        doNothing().when(dataCenterClient).reportUnresolved(any());
        AiCompletionGateway model = (prompt, message) -> Optional.of("平台没有上架任何商品");

        var response = service(mfEpClient, dataCenterClient, model)
                .chat(new AgentChatRequest("contract-5", "搜索苹果苗", "u1", "client", null));

        assertThat(response.intent()).isEqualTo("product");
        assertThat(response.answer()).isEqualTo("没有找到在售且有库存的相关商品。");
        assertThat(response.answer()).doesNotContain("平台没有上架");
        verify(mfEpClient).searchProducts(eq("苹果苗"), eq(null), eq(1), eq(5));
    }

    @Test
    void confirmedWateringQuestionUsesContractEmptyResponseWhenKnowledgeIsUnavailable() throws Exception {
        var mfEpClient = mock(MfEpClient.class);
        when(mfEpClient.searchEncyclopedia(eq("果树夏天浇多少水"), eq(1), eq(5)))
                .thenReturn(objectMapper.readTree("{\"records\":[]}"));
        var dataCenterClient = mock(DataCenterClient.class);
        when(dataCenterClient.logConversation(any())).thenReturn(58L);
        doNothing().when(dataCenterClient).logToolCall(any());
        doNothing().when(dataCenterClient).reportUnresolved(any());

        var response = service(mfEpClient, dataCenterClient)
                .chat(new AgentChatRequest("contract-6", "果树夏天浇多少水？", "u1", "client", null));

        assertThat(response.intent()).isEqualTo("encyclopedia");
        assertThat(response.answer()).isEqualTo("当前没有足够依据，请先观察土壤并咨询人工。");
    }

    @SuppressWarnings("unchecked")
    private CustomerAgentService service(MfEpClient mfEpClient, DataCenterClient dataCenterClient) {
        return service(mfEpClient, dataCenterClient, null);
    }

    @SuppressWarnings("unchecked")
    private CustomerAgentService service(MfEpClient mfEpClient, DataCenterClient dataCenterClient, AiCompletionGateway model) {
        var executor = new ToolExecutor();
        var provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(model);
        return new CustomerAgentService(
                new IntentResolver(),
                new ConversationMemoryService(new MfAgentProperties(null, null, null)),
                new HybridRagService(new KnowledgeRetrievalService(), new EmbeddingClient(new RagProperties(false, "", "", "", 0.62)), new RagProperties(false, "", "", "", 0.62)),
                new ProductTools(mfEpClient, executor),
                new EncyclopediaTools(mfEpClient, executor),
                new OrderTools(mfEpClient, executor),
                new MerchantTools(executor),
                new DataCenterTools(dataCenterClient, executor),
                new KnowledgeGapAdvisor(),
                new PlatformProfileService(new DefaultResourceLoader()),
                new CustomerServiceContractService(new DefaultResourceLoader()),
                new CustomerServicePromptCatalog(new DefaultResourceLoader()),
                provider
        );
    }
}
