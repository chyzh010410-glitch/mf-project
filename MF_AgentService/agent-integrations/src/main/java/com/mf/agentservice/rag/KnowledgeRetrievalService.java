package com.mf.agentservice.rag;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeRetrievalService {
    private final List<RagDocument> baselineDocuments = List.of(
            new RagDocument("faq", "care-yellow-leaf", "叶片发黄排查",
                    "叶片发黄通常先排查浇水、光照、缺氮缺铁、根系问题和病虫害。回答时应建议用户补充地区、作物、叶片照片和最近施肥记录。",
                    List.of("黄叶", "养护", "施肥")),
            new RagDocument("faq", "fruit-tree-fertilizer", "果树施肥原则",
                    "果树施肥应按基肥、萌芽肥、膨果肥、采后肥分阶段处理，避免一次性高浓度施肥。推荐商品前应结合树龄、土壤和季节。",
                    List.of("果树", "肥料", "施肥")),
            new RagDocument("encyclopedia", "apple-rot", "苹果树腐烂病",
                    "苹果树腐烂病需要刮除病斑、伤口消毒、加强树势管理，并避免在雨后立即处理伤口。严重时应联系当地植保人员确认。",
                    List.of("苹果树", "腐烂病", "病虫害")),
            new RagDocument("faq", "safe-order-service", "订单售后边界",
                    "客服 Agent 只能解释订单、退款和售后流程，不能自动退款、改订单、确认收货或替代人工审核。",
                    List.of("订单", "售后", "安全"))
    );
    private volatile List<RagDocument> documents = baselineDocuments;

    public List<RagDocument> retrieve(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String normalized = query.toLowerCase(Locale.ROOT);
        return documents.stream()
                .map(document -> new ScoredDocument(document, score(document, normalized)))
                .filter(scored -> scored.score() >= 5)
                .sorted((left, right) -> Integer.compare(right.score(), left.score()))
                .limit(3)
                .map(ScoredDocument::document)
                .toList();
    }

    public List<RagDocument> documents() {
        return documents;
    }

    public void refresh(JsonNode encyclopedia, JsonNode faq, JsonNode articles) {
        List<RagDocument> refreshed = new ArrayList<>(baselineDocuments);
        appendEncyclopedia(refreshed, encyclopedia);
        appendFaq(refreshed, faq);
        appendArticles(refreshed, articles);
        documents = List.copyOf(refreshed);
    }

    private void appendEncyclopedia(List<RagDocument> refreshed, JsonNode data) {
        for (JsonNode entry : records(data)) {
            String name = text(entry, "name");
            String content = firstNonBlank(text(entry, "careGuide"), text(entry, "description"));
            if (name.isBlank() || content.isBlank()) {
                continue;
            }
            refreshed.add(new RagDocument("encyclopedia", "entry-" + text(entry, "id"), name, content,
                    tags(name, text(entry, "alias"), text(entry, "tags"))));
        }
    }

    private void appendArticles(List<RagDocument> refreshed, JsonNode data) {
        for (JsonNode article : records(data)) {
            String title = text(article, "title");
            String content = firstNonBlank(text(article, "summary"), text(article, "content"));
            if (title.isBlank() || content.isBlank()) {
                continue;
            }
            refreshed.add(new RagDocument("article", "article-" + text(article, "id"), title, content,
                    tags(title, text(article, "tags"))));
        }
    }

    private void appendFaq(List<RagDocument> refreshed, JsonNode data) {
        for (JsonNode entry : records(data)) {
            String question = text(entry, "question");
            String answer = text(entry, "answer");
            if (question.isBlank() || answer.isBlank()) {
                continue;
            }
            refreshed.add(new RagDocument("faq", "faq-" + text(entry, "id"), question, answer,
                    tags(question, text(entry, "category"))));
        }
    }

    private List<JsonNode> records(JsonNode data) {
        if (data == null || data.isNull()) {
            return List.of();
        }
        JsonNode records = data.isArray() ? data : data.path("records");
        if (!records.isArray()) {
            return List.of();
        }
        List<JsonNode> values = new ArrayList<>();
        records.forEach(values::add);
        return values;
    }

    private List<String> tags(String... values) {
        List<String> tags = new ArrayList<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            for (String tag : value.split("[,，|/\\s]+")) {
                if (!tag.isBlank()) {
                    tags.add(tag);
                }
            }
        }
        return tags;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText().trim();
    }

    private String firstNonBlank(String first, String second) {
        return first.isBlank() ? second : first;
    }

    private int score(RagDocument document, String query) {
        int score = 0;
        if (containsEither(query, document.title())) {
            score += 8;
        }
        for (String tag : document.tags()) {
            if (query.contains(tag.toLowerCase(Locale.ROOT))) {
                score += isGenericTag(tag) ? 1 : 5;
            }
        }
        return score;
    }

    private boolean isGenericTag(String tag) {
        return List.of("苹果树", "果树", "树", "养护", "种植", "病虫害").contains(tag);
    }

    private boolean containsEither(String query, String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains(query) || query.contains(normalized);
    }

    private record ScoredDocument(RagDocument document, int score) {
    }
}
