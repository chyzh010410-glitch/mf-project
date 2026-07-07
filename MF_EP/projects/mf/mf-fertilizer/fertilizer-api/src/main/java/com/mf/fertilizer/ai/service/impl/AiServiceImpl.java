package com.mf.fertilizer.ai.service.impl;

import com.mf.fertilizer.ai.client.AiClient;
import com.mf.fertilizer.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiClient aiClient;

    @Override
    public Map<?, ?> chat(String question) {
        return aiClient.chat(question);
    }

    @Override
    public Map<?, ?> draftArticle(String topic, String category) {
        return aiClient.draftArticle(topic, category);
    }

    @Override
    public Map<?, ?> draftEncyclopedia(String name) {
        return aiClient.draftEncyclopedia(name);
    }

    @Override
    public Map<?, ?> rebuildKnowledge() {
        return aiClient.rebuildKnowledge();
    }
}
