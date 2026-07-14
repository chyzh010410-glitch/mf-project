package com.mf.agentservice.agent;

public enum AgentIntent {
    GREETING("greeting"),
    HELP("help"),
    COMPANY("company"),
    DIRECT("direct"),
    PRODUCT("product"),
    ENCYCLOPEDIA("encyclopedia"),
    ORDER("order"),
    MERCHANT("merchant"),
    UNSAFE_ACTION("unsafe_action"),
    UNKNOWN("unknown");

    private final String code;

    AgentIntent(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
