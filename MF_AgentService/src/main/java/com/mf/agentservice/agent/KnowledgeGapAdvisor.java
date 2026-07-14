package com.mf.agentservice.agent;

import com.mf.agentservice.api.KnowledgeGap;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeGapAdvisor {
    public Optional<KnowledgeGap> assess(AgentIntent intent, String fallbackReason, String question) {
        if (!"knowledge_not_enough".equals(fallbackReason) && intent != AgentIntent.UNKNOWN) {
            return Optional.empty();
        }
        String reason = fallbackReason == null ? "intent_unknown" : fallbackReason;
        return Optional.of(new KnowledgeGap(topic(question), reason, riskLevel(question), suggestedContentTypes(intent)));
    }

    private List<String> suggestedContentTypes(AgentIntent intent) {
        return intent == AgentIntent.ENCYCLOPEDIA
                ? List.of("faq", "article", "encyclopedia")
                : List.of("faq");
    }

    private String riskLevel(String question) {
        String value = question == null ? "" : question;
        if (containsAny(value, "农药", "药剂", "剂量", "中毒", "死亡", "法律", "退款", "支付")) {
            return "high";
        }
        if (containsAny(value, "病", "虫", "施肥", "用药", "修剪")) {
            return "medium";
        }
        return "low";
    }

    private String topic(String question) {
        String value = question == null ? "" : question.replaceAll("\\d{4,}", "*")
                .replaceAll("[?？!！]", " ").trim();
        return value.length() <= 60 ? value : value.substring(0, 60);
    }

    private boolean containsAny(String value, String... words) {
        for (String word : words) {
            if (value.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
