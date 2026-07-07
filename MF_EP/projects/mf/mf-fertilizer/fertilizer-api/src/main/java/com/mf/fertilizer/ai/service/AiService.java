package com.mf.fertilizer.ai.service;

import java.util.Map;

public interface AiService {

    Map<?, ?> chat(String question);

    Map<?, ?> draftArticle(String topic, String category);

    Map<?, ?> draftEncyclopedia(String name);

    Map<?, ?> rebuildKnowledge();
}
