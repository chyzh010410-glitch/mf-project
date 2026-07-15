package com.mf.agentservice.agent.port;

import com.mf.agentservice.rag.RagDocument;
import java.util.List;

public interface KnowledgeRetrievalPort {
    List<RagDocument> retrieve(String query);
}
