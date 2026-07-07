package com.mf.agentservice.client;

import com.fasterxml.jackson.databind.JsonNode;

public record ApiEnvelope(
        Integer code,
        String message,
        JsonNode data
) {
    public boolean ok() {
        return code == null || code == 0 || code == 200;
    }
}
