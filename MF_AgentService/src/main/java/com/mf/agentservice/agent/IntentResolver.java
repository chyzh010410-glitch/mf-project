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
            "商品", "产品", "肥料", "苗木", "推荐", "购买", "价格", "库存", "product", "fertilizer"
    );
    private static final List<String> ENCYCLOPEDIA_WORDS = List.of(
            "百科", "种植", "养护", "病虫害", "黄叶", "腐烂病", "浇水", "施肥", "苹果树", "果树", "plant", "care"
    );
    private static final List<String> ORDER_WORDS = List.of(
            "订单", "发货", "物流", "售后", "order", "shipping"
    );
    private static final List<String> MERCHANT_WORDS = List.of(
            "商家", "入驻", "开店", "资质", "merchant"
    );

    public AgentIntent resolve(String message) {
        String normalized = normalize(message);
        if (containsAny(normalized, UNSAFE_WORDS)) {
            return AgentIntent.UNSAFE_ACTION;
        }
        if (containsAny(normalized, ORDER_WORDS)) {
            return AgentIntent.ORDER;
        }
        if (containsAny(normalized, MERCHANT_WORDS)) {
            return AgentIntent.MERCHANT;
        }
        if (containsAny(normalized, PRODUCT_WORDS)) {
            return AgentIntent.PRODUCT;
        }
        if (containsAny(normalized, ENCYCLOPEDIA_WORDS)) {
            return AgentIntent.ENCYCLOPEDIA;
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
}
