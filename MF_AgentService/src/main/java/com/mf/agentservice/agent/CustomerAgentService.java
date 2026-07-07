package com.mf.agentservice.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.mf.agentservice.api.AgentChatRequest;
import com.mf.agentservice.api.AgentChatResponse;
import com.mf.agentservice.rag.KnowledgeRetrievalService;
import com.mf.agentservice.rag.RagDocument;
import com.mf.agentservice.tools.DataCenterTools;
import com.mf.agentservice.tools.EncyclopediaTools;
import com.mf.agentservice.tools.MerchantTools;
import com.mf.agentservice.tools.OrderTools;
import com.mf.agentservice.tools.ProductTools;
import com.mf.agentservice.tools.ToolExecutionContext;
import com.mf.agentservice.tools.ToolResult;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class CustomerAgentService {
    private static final String SYSTEM_PROMPT = """
            You are MF_AgentService, the controlled customer service Agent for the MF platform.
            Answer concisely, call tools for business data, and never perform refunds, order edits,
            merchant audits, direct database writes, or other high-risk actions.
            """;

    private final IntentResolver intentResolver;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final ProductTools productTools;
    private final EncyclopediaTools encyclopediaTools;
    private final OrderTools orderTools;
    private final MerchantTools merchantTools;
    private final DataCenterTools dataCenterTools;
    private final Optional<AiCompletionGateway> aiCompletionGateway;

    public CustomerAgentService(
            IntentResolver intentResolver,
            KnowledgeRetrievalService knowledgeRetrievalService,
            ProductTools productTools,
            EncyclopediaTools encyclopediaTools,
            OrderTools orderTools,
            MerchantTools merchantTools,
            DataCenterTools dataCenterTools,
            ObjectProvider<AiCompletionGateway> aiCompletionGateway
    ) {
        this.intentResolver = intentResolver;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
        this.productTools = productTools;
        this.encyclopediaTools = encyclopediaTools;
        this.orderTools = orderTools;
        this.merchantTools = merchantTools;
        this.dataCenterTools = dataCenterTools;
        this.aiCompletionGateway = Optional.ofNullable(aiCompletionGateway.getIfAvailable());
    }

    public AgentChatResponse chat(AgentChatRequest request) {
        var context = new ToolExecutionContext();
        var intent = intentResolver.resolve(request.message());
        var answer = answer(request, intent, context);
        var resolved = intent != AgentIntent.UNKNOWN && intent != AgentIntent.UNSAFE_ACTION;
        String fallbackReason = fallbackReason(intent, answer);

        Long conversationId = null;
        try {
            var logResult = dataCenterTools.logConversation(context, request, answer, intent.code(), resolved);
            conversationId = logResult.value();
            dataCenterTools.logToolCalls(conversationId, context.records());
            if (!resolved || fallbackReason != null) {
                dataCenterTools.reportUnresolved(conversationId, request.message(), fallbackReason == null ? "unresolved" : fallbackReason);
            } else if (isSampleCandidate(intent, answer)) {
                dataCenterTools.saveSampleCandidate(conversationId, request.message(), answer);
            }
        } catch (RuntimeException ignored) {
            fallbackReason = fallbackReason == null ? "datacenter_log_failed" : fallbackReason;
        }

        return new AgentChatResponse(answer, intent.code(), resolved, context.summaries(), conversationId, fallbackReason);
    }

    private String answer(AgentChatRequest request, AgentIntent intent, ToolExecutionContext context) {
        return switch (intent) {
            case UNSAFE_ACTION -> unsafeAnswer();
            case ORDER -> answerOrder(request, context);
            case MERCHANT -> answerMerchant(context);
            case PRODUCT -> answerProduct(request, context);
            case ENCYCLOPEDIA -> answerKnowledge(request, context);
            case DIRECT -> directAnswer(request.message());
            case UNKNOWN -> unknownAnswer();
        };
    }

    private String answerProduct(AgentChatRequest request, ToolExecutionContext context) {
        var keyword = intentResolver.keyword(request.message());
        var result = productTools.search(context, keyword, null);
        if (!Boolean.TRUE.equals(result.success())) {
            return "我暂时没有查到商品信息，可以换一个更具体的苗木、肥料名称，或稍后再试。";
        }
        return "我已按你的问题查询了苗丰商品库。你可以优先查看匹配度靠前、库存正常、适用场景明确的商品；如果告诉我作物、季节和预算，我可以继续帮你缩小范围。";
    }

    private String answerKnowledge(AgentChatRequest request, ToolExecutionContext context) {
        List<RagDocument> documents = knowledgeRetrievalService.retrieve(request.message());
        ToolResult<JsonNode> encyclopedia = encyclopediaTools.search(context, intentResolver.keyword(request.message()));
        if (!documents.isEmpty()) {
            return "根据苗丰知识库，" + documents.get(0).content()
                    + " 我也会结合百科检索结果一起判断；如果你能补充地区、作物年龄、近期天气和照片，建议会更准确。";
        }
        if (Boolean.TRUE.equals(encyclopedia.success())) {
            return "我已查询苗丰百科，但当前信息还不够完整。请补充作物名称、症状、发生时间和图片，我再帮你判断。";
        }
        return "这个种植问题我现在无法可靠判断。请补充作物、地区、症状图片和最近养护记录，我会把问题记录到未解决问题池。";
    }

    private String answerOrder(AgentChatRequest request, ToolExecutionContext context) {
        var orderId = intentResolver.extractFirstNumber(request.message());
        if (orderId.isEmpty()) {
            return "请提供订单编号或订单 ID。涉及订单隐私时，我必须确认登录身份后才能查询。";
        }
        var result = orderTools.status(context, orderId.get(), request.authToken());
        if (!Boolean.TRUE.equals(result.success())) {
            return "订单状态属于敏感信息，请先登录后再查询。我不会在未验证身份时透露订单、物流或售后信息。";
        }
        return "我已查询到该订单的当前状态。你可以在订单详情页继续查看物流、售后进度；如需退款或改订单，需要按平台流程人工确认，我不能自动执行。";
    }

    private String answerMerchant(ToolExecutionContext context) {
        var guide = merchantTools.guide(context);
        return Boolean.TRUE.equals(guide.success()) ? guide.value() : "商家入驻说明暂时不可用，请稍后再试。";
    }

    private String directAnswer(String message) {
        return aiCompletionGateway
                .flatMap(gateway -> gateway.complete(SYSTEM_PROMPT, message))
                .filter(answer -> !answer.isBlank())
                .orElse("你好，我是苗丰精灵背后的客服 Agent。可以帮你查商品、百科、订单状态、商家入驻流程，也能把未解决问题沉淀到数据中台。");
    }

    private String unsafeAnswer() {
        return "这类操作需要人工确认，我不能自动退款、改订单、确认收货或审核商家。你可以告诉我具体诉求，我会说明平台流程并引导你去对应入口处理。";
    }

    private String unknownAnswer() {
        return "这个问题我还不能可靠判断。请补充作物、商品、订单或商家入驻相关背景，我会继续帮你查；必要时我会把问题记录到未解决问题池。";
    }

    private String fallbackReason(AgentIntent intent, String answer) {
        if (intent == AgentIntent.UNSAFE_ACTION) {
            return "unsafe_action_blocked";
        }
        if (intent == AgentIntent.UNKNOWN) {
            return "intent_unknown";
        }
        if (answer.contains("无法可靠判断")) {
            return "knowledge_not_enough";
        }
        return null;
    }

    private boolean isSampleCandidate(AgentIntent intent, String answer) {
        return (intent == AgentIntent.ENCYCLOPEDIA || intent == AgentIntent.MERCHANT)
                && answer != null
                && answer.length() >= 40;
    }
}
