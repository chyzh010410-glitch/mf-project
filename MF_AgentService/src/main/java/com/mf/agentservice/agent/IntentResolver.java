package com.mf.agentservice.agent;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class IntentResolver {
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d{1,18})");
    private static final List<String> UNSAFE_WORDS = List.of(
            "退款", "退钱", "改订单", "取消订单", "确认收货", "支付", "审核商家", "通过审核", "驳回商家",
            "refund", "cancel order", "change order", "approve merchant"
    );
    private static final List<String> PRODUCT_WORDS = List.of(
            "商品", "产品", "肥料", "苗木", "树苗", "推荐", "购买", "价格", "库存", "哪个好", "哪款", "卖什么", "都卖", "product", "fertilizer"
    );
    private static final List<String> ENCYCLOPEDIA_WORDS = List.of(
            "百科", "种植", "养护", "病虫害", "黄叶", "腐烂病", "浇水", "施肥", "苹果树", "果树", "叶片", "根系",
            "怎么处理", "怎么办", "什么原因", "如何养", "plant", "care"
    );
    private static final List<String> ORDER_WORDS = List.of(
            "订单", "发货", "物流", "售后", "order", "shipping"
    );
    private static final List<String> MERCHANT_WORDS = List.of(
            "商家", "入驻", "开店", "资质", "merchant"
    );
    private static final List<String> GREETING_WORDS = List.of("你好", "您好", "嗨", "哈喽", "hello", "hi");
    private static final List<String> HELP_WORDS = List.of("你会什么", "你会做什么", "你能干嘛", "你能做什么", "你能做啥", "有什么功能", "能帮我什么", "帮助", "help");
    private static final List<String> COMPANY_WORDS = List.of("你是哪个公司的客服", "你们是什么公司", "你是谁家的客服", "苗丰是什么");

    public AgentIntent resolve(String message) {
        String normalized = normalize(message);
        if (isShortGreeting(normalized)) {
            return AgentIntent.GREETING;
        }
        if (containsAny(normalized, HELP_WORDS)) {
            return AgentIntent.HELP;
        }
        if (containsAny(normalized, COMPANY_WORDS)) {
            return AgentIntent.COMPANY;
        }
        if (containsAny(normalized, UNSAFE_WORDS)) {
            return AgentIntent.UNSAFE_ACTION;
        }
        if (containsAny(normalized, ORDER_WORDS)) {
            return AgentIntent.ORDER;
        }
        if (containsAny(normalized, MERCHANT_WORDS)) {
            return AgentIntent.MERCHANT;
        }
        if (containsAny(normalized, ENCYCLOPEDIA_WORDS)) {
            if (containsAny(normalized, List.of("推荐", "购买", "价格", "库存", "哪个好", "哪款"))) {
                return AgentIntent.PRODUCT;
            }
            return AgentIntent.ENCYCLOPEDIA;
        }
        if (containsAny(normalized, PRODUCT_WORDS)) {
            return AgentIntent.PRODUCT;
        }
        if (normalized.contains("苗丰") || normalized.contains("mf")) {
            return AgentIntent.DIRECT;
        }
        return AgentIntent.UNKNOWN;
    }

    public Optional<Long> extractFirstNumber(String message) {
        var matcher = NUMBER_PATTERN.matcher(message == null ? "" : message);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(matcher.group(1)));
    }

    public String keyword(String message) {
        if (message == null) {
            return "";
        }
        return message.replaceAll("[?？!！,，。]", " ").trim();
    }

    private boolean containsAny(String value, List<String> words) {
        return words.stream().anyMatch(value::contains);
    }

    private String normalize(String message) {
        return message == null ? "" : message.toLowerCase(Locale.ROOT);
    }

    private boolean isShortGreeting(String message) {
        return message.length() <= 12 && containsAny(message, GREETING_WORDS);
    }
}
