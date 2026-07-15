package com.mf.agentservice.api;

public record AgentSource(String type, String id, String title, String updatedAt, Integer score) {
}
