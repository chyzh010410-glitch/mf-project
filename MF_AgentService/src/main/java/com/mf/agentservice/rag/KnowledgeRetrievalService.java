package com.mf.agentservice.rag;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeRetrievalService {
    private final List<RagDocument> documents = List.of(
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

    public List<RagDocument> retrieve(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String normalized = query.toLowerCase(Locale.ROOT);
        return documents.stream()
                .filter(document -> matches(document, normalized))
                .limit(3)
                .toList();
    }

    private boolean matches(RagDocument document, String query) {
        if (contains(query, document.title()) || contains(query, document.content())) {
            return true;
        }
        return document.tags().stream().anyMatch(tag -> query.contains(tag.toLowerCase(Locale.ROOT)));
    }

    private boolean contains(String query, String value) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }
}
