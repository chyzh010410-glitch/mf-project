package com.mf.agentservice.rag;

import java.util.List;

public record RagDocument(
        String sourceType,
        String sourceId,
        String title,
        String content,
        List<String> tags
) {
}
