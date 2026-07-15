package com.mf.agentservice.tools;

public enum ToolFailureReason {
    ORDER_AUTH_REQUIRED,
    UPSTREAM_TIMEOUT,
    UPSTREAM_UNAVAILABLE,
    UPSTREAM_BUSINESS_ERROR,
    UNKNOWN
}
