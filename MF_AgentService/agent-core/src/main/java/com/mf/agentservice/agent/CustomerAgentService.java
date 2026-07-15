package com.mf.agentservice.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.mf.agentservice.api.AgentChatRequest;
import com.mf.agentservice.api.AgentChatResponse;
import com.mf.agentservice.api.AgentSource;
import com.mf.agentservice.api.KnowledgeGap;
import com.mf.agentservice.agent.port.ConversationAuditPort;
import com.mf.agentservice.agent.port.EncyclopediaToolPort;
import com.mf.agentservice.agent.port.KnowledgeRetrievalPort;
import com.mf.agentservice.agent.port.MerchantToolPort;
import com.mf.agentservice.agent.port.OrderToolPort;
import com.mf.agentservice.agent.port.ProductToolPort;
import com.mf.agentservice.rag.RagDocument;
import com.mf.agentservice.tools.ToolExecutionContext;
import com.mf.agentservice.tools.ToolFailureReason;
import com.mf.agentservice.tools.ToolResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class CustomerAgentService {
    private final IntentResolver intentResolver;
    private final ConversationMemoryService conversationMemoryService;
    private final KnowledgeRetrievalPort knowledgeRetrievalPort;
    private final ProductToolPort productTools;
    private final EncyclopediaToolPort encyclopediaTools;
    private final OrderToolPort orderTools;
    private final MerchantToolPort merchantTools;
    private final ConversationAuditPort conversationAuditPort;
    private final KnowledgeGapAdvisor knowledgeGapAdvisor;
    private final PlatformProfileService platformProfileService;
    private final CustomerServiceContractService customerServiceContractService;
    private final CustomerServicePromptCatalog promptCatalog;
    private final Optional<AiCompletionGateway> aiCompletionGateway;

    public CustomerAgentService(
            IntentResolver intentResolver,
            ConversationMemoryService conversationMemoryService,
            KnowledgeRetrievalPort knowledgeRetrievalPort,
            ProductToolPort productTools,
            EncyclopediaToolPort encyclopediaTools,
            OrderToolPort orderTools,
            MerchantToolPort merchantTools,
            ConversationAuditPort conversationAuditPort,
            KnowledgeGapAdvisor knowledgeGapAdvisor,
            PlatformProfileService platformProfileService,
            CustomerServiceContractService customerServiceContractService,
            CustomerServicePromptCatalog promptCatalog,
            ObjectProvider<AiCompletionGateway> aiCompletionGateway
    ) {
        this.intentResolver = intentResolver;
        this.conversationMemoryService = conversationMemoryService;
        this.knowledgeRetrievalPort = knowledgeRetrievalPort;
        this.productTools = productTools;
        this.encyclopediaTools = encyclopediaTools;
        this.orderTools = orderTools;
        this.merchantTools = merchantTools;
        this.conversationAuditPort = conversationAuditPort;
        this.knowledgeGapAdvisor = knowledgeGapAdvisor;
        this.platformProfileService = platformProfileService;
        this.customerServiceContractService = customerServiceContractService;
        this.promptCatalog = promptCatalog;
        this.aiCompletionGateway = Optional.ofNullable(aiCompletionGateway.getIfAvailable());
    }

    public AgentChatResponse chat(AgentChatRequest request) {
        return chat(request, true);
    }

    public AgentChatResponse evaluate(AgentChatRequest request) {
        return chat(request, false);
    }

    private AgentChatResponse chat(AgentChatRequest request, boolean persist) {
        var context = new ToolExecutionContext();
        var contractFlow = customerServiceContractService.match(request.message()).orElse(null);
        var intent = resolveIntent(request, contractFlow);
        var retrievalQuery = retrievalQuery(request, intent);
        var answer = answer(request, intent, context, retrievalQuery, contractFlow);
        String fallbackReason = fallbackReason(intent, answer, context, contractFlow);
        KnowledgeGap knowledgeGap = knowledgeGapAdvisor.assess(intent, fallbackReason, request.message()).orElse(null);
        var resolved = fallbackReason == null;

        Long conversationId = null;
        if (persist) try {
            var logResult = conversationAuditPort.logConversation(context, request, answer, intent.code(), resolved);
            conversationId = logResult.value();
            if (!Boolean.TRUE.equals(logResult.success()) || conversationId == null) {
                fallbackReason = fallbackReason == null ? "datacenter_log_failed" : fallbackReason;
            } else {
                conversationAuditPort.logToolCalls(conversationId, context.records());
                if (!resolved || fallbackReason != null) {
                    conversationAuditPort.reportUnresolved(conversationId, request.message(), fallbackReason == null ? "unresolved" : fallbackReason, knowledgeGap);
                } else if (isSampleCandidate(intent, answer)) {
                    conversationAuditPort.saveSampleCandidate(conversationId, request.message(), answer);
                }
            }
        } catch (RuntimeException ignored) {
            fallbackReason = fallbackReason == null ? "datacenter_log_failed" : fallbackReason;
        }

        resolved = fallbackReason == null;
        if (persist) {
            conversationMemoryService.remember(request.sessionId(), intent, request.message(), answer);
        }
        List<AgentSource> sources = intent == AgentIntent.ENCYCLOPEDIA
                ? knowledgeRetrievalPort.retrieve(retrievalQuery).stream()
                .map(document -> new AgentSource(document.sourceType(), document.sourceId(), document.title(), Instant.now().toString(), 80))
                .toList() : List.of();
        int confidence = confidence(intent, fallbackReason, context, sources);
        boolean reviewRequired = confidence < 70 || "knowledge_not_enough".equals(fallbackReason)
                || "intent_unknown".equals(fallbackReason);
        return new AgentChatResponse(answer, intent.code(), resolved, context.summaries(), conversationId, fallbackReason,
                sources, confidence, reviewRequired, knowledgeGap);
    }

    private int confidence(AgentIntent intent, String fallbackReason, ToolExecutionContext context, List<AgentSource> sources) {
        if (intent == AgentIntent.UNSAFE_ACTION || context.records().stream()
                .anyMatch(record -> record.failureReason() == ToolFailureReason.ORDER_AUTH_REQUIRED)) return 95;
        if (fallbackReason != null) return 30;
        if (intent == AgentIntent.ENCYCLOPEDIA) return sources.isEmpty() ? 55 : 85;
        if (intent == AgentIntent.PRODUCT && context.records().stream().anyMatch(record -> "product.search".equals(record.name()) && record.success())) return 85;
        return intent == AgentIntent.UNKNOWN ? 30 : 80;
    }

    private AgentIntent resolveIntent(AgentChatRequest request, CustomerServiceContractService.ContractFlow contractFlow) {
        AgentIntent contractIntent = intentForContract(contractFlow);
        if (contractIntent != null) {
            return contractIntent;
        }
        AgentIntent intent = intentResolver.resolve(request.message());
        if (intent == AgentIntent.ENCYCLOPEDIA && isShortPlantProductFollowUp(request.message())
                && conversationMemoryService.latestTurn(request.sessionId())
                .map(ConversationMemoryService.ConversationTurn::intent)
                .filter(previous -> previous == AgentIntent.DIRECT || previous == AgentIntent.COMPANY || previous == AgentIntent.PRODUCT)
                .isPresent()) {
            return AgentIntent.PRODUCT;
        }
        return intent == AgentIntent.UNKNOWN && isFollowUp(request.message())
                ? conversationMemoryService.latestTurn(request.sessionId())
                .map(ConversationMemoryService.ConversationTurn::intent)
                .orElse(intent)
                : intent;
    }

    private AgentIntent intentForContract(CustomerServiceContractService.ContractFlow contractFlow) {
        if (contractFlow == null || !contractFlow.hasStatus("confirmed")) {
            return null;
        }
        return switch (contractFlow.id()) {
            case "01", "02", "06", "08", "10" -> AgentIntent.PRODUCT;
            case "11", "14", "15", "17", "19", "20", "21", "22" -> AgentIntent.ENCYCLOPEDIA;
            case "23", "25" -> AgentIntent.ORDER;
            case "31", "32", "33", "34" -> AgentIntent.MERCHANT;
            default -> null;
        };
    }

    private boolean isShortPlantProductFollowUp(String message) {
        String normalized = message == null ? "" : message.replaceAll("[?\\uFF1F\\uFF01\\uFF0C,\\s]", "").trim();
        boolean isPlant = normalized.contains("苹果树") || normalized.contains("果树")
                || normalized.contains("树苗") || normalized.contains("苗木");
        boolean isCareQuestion = normalized.contains("怎么") || normalized.contains("如何")
                || normalized.contains("施肥") || normalized.contains("浇水") || normalized.contains("黄叶")
                || normalized.contains("病") || normalized.contains("防治");
        return normalized.length() <= 12 && isPlant && !isCareQuestion;
    }

    private String answer(AgentChatRequest request, AgentIntent intent, ToolExecutionContext context, String retrievalQuery,
                          CustomerServiceContractService.ContractFlow contractFlow) {
        if (contractFlow != null && (contractFlow.hasStatus("pending_data") || contractFlow.hasStatus("unsupported"))) {
            return contractFlow.response().empty();
        }
        if (contractFlow != null && "37".equals(contractFlow.id())) {
            return clarificationAnswer(request);
        }
        if (contractFlow != null && "39".equals(contractFlow.id())) {
            return contractFlow.response().empty();
        }
        if (contractFlow != null && "40".equals(contractFlow.id())) {
            return contractFlow.response().empty();
        }
        String response = switch (intent) {
            case GREETING -> directAnswer(request.message(), greetingAnswer());
            case HELP -> directAnswer(request.message(), helpAnswer());
            case COMPANY -> companyAnswer();
            case UNSAFE_ACTION -> unsafeAnswer();
            case ORDER -> answerOrder(request, context);
            case MERCHANT -> answerMerchant(context);
            case PRODUCT -> answerProduct(request, context, contractFlow);
            case ENCYCLOPEDIA -> answerKnowledge(retrievalQuery, context);
            case DIRECT -> directAnswer(request.message(), defaultDirectAnswer());
            case OUT_OF_SCOPE -> outOfScopeAnswer();
            case UNKNOWN -> directAnswer(request.message(), unknownAnswer());
        };
        if (contractFlow != null && contractFlow.hasStatus("confirmed")
                && response.contains("无法可靠判断")) {
            return contractFlow.response().empty();
        }
        return response;
    }

    private String clarificationAnswer(AgentChatRequest request) {
        long previousClarifications = conversationMemoryService.recentTurns(request.sessionId()).stream()
                .filter(turn -> turn.intent() == AgentIntent.UNKNOWN)
                .count();
        if (previousClarifications >= 2) {
            return customerServiceContractService.match("还是不知道")
                    .map(flow -> flow.response().empty())
                    .orElse("暂时无法可靠回答，已建议转人工或记录到未解决问题池。");
        }
        return "你想问树苗、肥料、订单还是种植？";
    }

    private String answerProduct(AgentChatRequest request, ToolExecutionContext context,
                                 CustomerServiceContractService.ContractFlow contractFlow) {
        var keyword = productKeyword(request.message());
        var result = productTools.search(context, keyword, null);
        if (!Boolean.TRUE.equals(result.success())) {
            return upstreamAnswer("商品", result.failureReason(), "你可以补充作物、使用场景或肥料名称，我再接着帮你筛选。");
        }
        int productCount = itemCount(result.value());
        if (productCount == 0) {
            if (contractFlow != null && contractFlow.hasStatus("confirmed")) {
                return contractFlow.response().empty();
            }
            return conversationalProductAnswer(request, result.value(), "可以呀。我查了当前商品库，暂时没有找到直接匹配的商品。你可以补充树苗品种、地区和预算，我再帮你换个关键词继续找。");
        }
        return "可以，我先帮你从苗丰商品库里筛了一下，找到 " + productCount
                + " 款比较匹配的商品：" + productSummary(result.value())
                + "。选购时建议重点看适用场景、养分配比和库存；如果你告诉我种什么、树龄和预算，我还能继续帮你挑得更准。";
    }

    private String answerKnowledge(String query, ToolExecutionContext context) {
        List<RagDocument> documents = knowledgeRetrievalPort.retrieve(query);
        ToolResult<JsonNode> encyclopedia = encyclopediaTools.search(context, intentResolver.keyword(query));
        if (!documents.isEmpty()) {
            return "这个情况确实需要留意。根据苗丰知识库，" + documents.get(0).content()
                    + encyclopediaSummary(encyclopedia.value())
                    + " 为了判断得更贴合你的情况，方便的话再告诉我地区、作物年龄、近期天气和症状照片。";
        }
        if (Boolean.TRUE.equals(encyclopedia.success()) && !records(encyclopedia.value()).isEmpty()) {
            return "我先帮你查了苗丰百科。" + encyclopediaSummary(encyclopedia.value())
                    + "如果方便，再补充作物名称、症状出现的时间和图片，我可以继续陪你一起判断。";
        }
        if (encyclopedia.failureReason() != null) {
            return upstreamAnswer("百科", encyclopedia.failureReason(), "请补充作物、地区、症状图片和最近养护记录，我会继续协助。");
        }
        return "这个种植问题我现在无法可靠判断。请补充作物、地区、症状图片和最近养护记录，我会把问题记录到未解决问题池。";
    }

    private String answerOrder(AgentChatRequest request, ToolExecutionContext context) {
        var orderId = intentResolver.extractFirstNumber(request.message());
        if (orderId.isEmpty()) {
            return "我可以帮你看订单进度。麻烦把订单编号发给我；订单属于隐私信息，登录后我才能继续查询。";
        }
        var result = orderTools.status(context, orderId.get(), request.authToken());
        if (!Boolean.TRUE.equals(result.success())) {
            if (result.failureReason() != ToolFailureReason.ORDER_AUTH_REQUIRED) {
                return upstreamAnswer("订单", result.failureReason(), "请稍后再试；我不会在未验证身份时透露订单、物流或售后信息。");
            }
            return "我理解你想尽快确认订单情况，不过订单、物流和售后都属于隐私信息。请先登录后再来问我，我就能在验证身份后帮你查询。";
        }
        return "我已经查到这笔订单的当前状态了。你也可以在订单详情页查看物流和售后进度；如果涉及退款或改订单，需要按平台流程人工确认，我不能替你自动操作。";
    }

    private String answerMerchant(ToolExecutionContext context) {
        var guide = merchantTools.guide(context);
        return Boolean.TRUE.equals(guide.success()) ? "当然可以，下面是苗丰商家入驻的大致流程：\n" + guide.value()
                : "商家入驻说明暂时没加载出来，麻烦稍后再试一次。";
    }

    private String directAnswer(String message, String fallback) {
        return aiCompletionGateway
                .flatMap(gateway -> gateway.complete(promptCatalog.systemPrompt() + "\n\n以下是受控的平台事实卡：\n" + platformProfileService.content(), message))
                .filter(answer -> !answer.isBlank())
                .orElse(fallback);
    }

    private String conversationalProductAnswer(AgentChatRequest request, JsonNode products, String fallback) {
        String history = conversationMemoryService.recentTurns(request.sessionId()).stream()
                .map(turn -> "用户：" + turn.message() + "\n客服：" + turn.answer())
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("无");
        String prompt = promptCatalog.systemPrompt() + "\n\n以下是最近对话：\n" + history
                + "\n\n本轮用户：" + request.message()
                + "\n\n以下是商品查询工具返回的唯一事实：\n" + products
                + "\n\n请自然地承接上文，简短回答。只能引用上述商品事实；没有匹配商品时必须明确说明，不得编造商品、价格、库存或平台政策，也不得承诺后续上架、留意更新、主动联系或通知用户。";
        return aiCompletionGateway
                .flatMap(gateway -> gateway.complete(prompt, request.message()))
                .filter(answer -> !answer.isBlank())
                .orElse(fallback);
    }

    private String defaultDirectAnswer() {
        return "你好，我是苗丰智能客服。你可以直接告诉我想了解的作物养护、商品、订单或商家入驻问题，我会尽力帮你处理。";
    }

    private String greetingAnswer() {
        return "你好呀，我是苗丰智能客服，很高兴帮你。无论是果树养护、病虫害、肥料和苗木商品，还是订单状态、商家入驻，都可以直接问我。比如：‘苹果树叶片发黄怎么办？’";
    }

    private String helpAnswer() {
        return "我能帮你做这几件事：找肥料和苗木商品、分析树木养护和病虫害问题、查询已登录账户的订单状态、说明商家入驻流程。你不用按固定格式提问，直接把你的情况告诉我就行；作物、症状、季节或预算越具体，我给的建议会越有针对性。";
    }

    private String companyAnswer() {
        return "我是苗丰施肥平台的智能客服，主要协助商品咨询、树木养护知识、订单状态和商家入驻流程。你可以像平时聊天一样直接提问，我会在需要时帮你查询平台中的公开信息。";
    }

    private boolean isFollowUp(String message) {
        String normalized = message == null ? "" : message.replaceAll("[?？!！,，。]", "").trim();
        return normalized.startsWith("那") || normalized.startsWith("这个") || normalized.startsWith("这")
                || normalized.startsWith("它") || normalized.startsWith("还有") || normalized.startsWith("然后")
                || normalized.startsWith("刚才") || normalized.startsWith("上面") || normalized.startsWith("继续");
    }

    private String retrievalQuery(AgentChatRequest request, AgentIntent intent) {
        if (intent != AgentIntent.ENCYCLOPEDIA || !isContextSupplement(request.message())) {
            return request.message();
        }
        return conversationMemoryService.latestTurn(request.sessionId())
                .filter(turn -> turn.intent() == AgentIntent.ENCYCLOPEDIA)
                .map(turn -> turn.message() + "\n" + request.message())
                .orElse(request.message());
    }

    private boolean isContextSupplement(String message) {
        String normalized = message == null ? "" : message.replaceAll("[?？!！,，。]", "").trim();
        boolean hasProfile = normalized.matches(".*\\d+\\s*年.*")
                || normalized.matches(".*[省市县区].*");
        boolean hasCrop = normalized.contains("苹果树") || normalized.contains("果树")
                || normalized.contains("树") || normalized.contains("作物");
        boolean hasAction = normalized.contains("怎么") || normalized.contains("如何") || normalized.contains("要点")
                || normalized.contains("修剪") || normalized.contains("施肥") || normalized.contains("浇水")
                || normalized.contains("黄叶") || normalized.contains("病") || normalized.contains("防治");
        return (hasProfile || hasCrop) && !hasAction;
    }

    private String unsafeAnswer() {
        return "我理解你想尽快处理这件事。不过退款、改订单、确认收货和商家审核都需要人工确认，我不能直接替你操作。你把具体诉求告诉我，我可以先帮你说明下一步该走哪个平台流程。";
    }

    private String unknownAnswer() {
        return "我愿意继续帮你一起看，不过现在的信息还不太够。你可以像聊天一样补充一下：是种什么作物、遇到了什么症状、想找什么商品，还是订单/商家方面的问题？我会根据你补充的内容继续判断。";
    }

    private String outOfScopeAnswer() {
        return customerServiceContractService.match("帮我看病")
                .map(flow -> flow.response().empty())
                .orElse("该问题超出平台范围，建议咨询相应专业人士。");
    }

    private String upstreamAnswer(String subject, ToolFailureReason reason, String nextStep) {
        if (reason == ToolFailureReason.UPSTREAM_TIMEOUT) {
            return "我是苗丰智能客服，" + subject + "查询响应超时。" + nextStep;
        }
        if (reason == ToolFailureReason.UPSTREAM_UNAVAILABLE) {
            return "我是苗丰智能客服，" + subject + "服务暂时不可用。" + nextStep;
        }
        return "我是苗丰智能客服，" + subject + "查询暂时失败。" + nextStep;
    }

    private int itemCount(JsonNode data) {
        return records(data).size();
    }

    private String productSummary(JsonNode data) {
        List<String> products = new ArrayList<>();
        for (JsonNode product : records(data).stream().limit(3).toList()) {
            String name = text(product, "name");
            if (name.isBlank()) {
                continue;
            }
            String price = text(product, "price");
            String unit = text(product, "unit");
            products.add(price.isBlank() ? name : name + "（¥" + formatPrice(price) + (unit.isBlank() ? "" : "/" + unit) + "）");
        }
        return products.isEmpty() ? "请在商城查看详情" : String.join("、", products);
    }

    private String encyclopediaSummary(JsonNode data) {
        return records(data).stream().findFirst()
                .map(entry -> {
                    String name = text(entry, "name");
                    String description = text(entry, "description");
                    String careGuide = text(entry, "careGuide");
                    String content = !careGuide.isBlank() ? careGuide : description;
                    if (name.isBlank() && content.isBlank()) {
                        return "";
                    }
                    return " 百科词条“" + name + "”提示：" + content + "。";
                })
                .orElse("");
    }

    private List<JsonNode> records(JsonNode data) {
        if (data == null || data.isNull()) {
            return List.of();
        }
        if (data.has("records") && data.get("records").isArray()) {
            return toList(data.get("records"));
        }
        if (data.has("list") && data.get("list").isArray()) {
            return toList(data.get("list"));
        }
        return data.isArray() ? toList(data) : List.of(data);
    }

    private List<JsonNode> toList(JsonNode data) {
        List<JsonNode> values = new ArrayList<>();
        data.forEach(values::add);
        return values;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? "" : value.asText().trim();
    }

    private String formatPrice(String value) {
        try {
            return new BigDecimal(value).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException ignored) {
            return value;
        }
    }

    private String fallbackReason(AgentIntent intent, String answer, ToolExecutionContext context,
                                  CustomerServiceContractService.ContractFlow contractFlow) {
        if (contractFlow != null && contractFlow.hasStatus("pending_data")) {
            return "contract_pending_data";
        }
        if (contractFlow != null && contractFlow.hasStatus("unsupported")) {
            return "contract_unsupported";
        }
        if (contractFlow != null && "40".equals(contractFlow.id())) {
            return "knowledge_not_enough";
        }
        if (contractFlow != null && "37".equals(contractFlow.id())
                && answer.contains("暂时无法可靠回答")) {
            return "knowledge_not_enough";
        }
        if (contractFlow != null && "37".equals(contractFlow.id())) {
            return null;
        }
        if (intent == AgentIntent.UNSAFE_ACTION) {
            return "unsafe_action_blocked";
        }
        if (intent == AgentIntent.UNKNOWN) {
            return "intent_unknown";
        }
        if (context.records().stream().anyMatch(record -> record.failureReason() == ToolFailureReason.UPSTREAM_TIMEOUT)) {
            return "upstream_timeout";
        }
        if (context.records().stream().anyMatch(record -> record.failureReason() == ToolFailureReason.UPSTREAM_UNAVAILABLE)) {
            return "upstream_unavailable";
        }
        if (context.records().stream().anyMatch(record -> record.failureReason() == ToolFailureReason.UPSTREAM_BUSINESS_ERROR)) {
            return "upstream_business_error";
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

    private boolean genericProductQuestion(String message) {
        String normalized = message == null ? "" : message.replaceAll("[?？!！,，。]", "").trim();
        return normalized.matches(".*(有什么商品|有哪些商品|商品有哪些|看看商品|商品|卖什么|都卖).*" );
    }

    private String productKeyword(String message) {
        if (genericProductQuestion(message)) {
            return "";
        }
        String keyword = intentResolver.keyword(message).replaceFirst("^(搜索|搜|查找|找)", "").trim();
        if (isShortPlantProductFollowUp(message)) {
            return keyword.replaceFirst("^(种|想种|我要种)", "").replaceFirst("(吧|呢|呀)$", "");
        }
        return keyword;
    }
}
