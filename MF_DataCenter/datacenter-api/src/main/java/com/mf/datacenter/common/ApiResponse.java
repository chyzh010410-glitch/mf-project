package com.mf.datacenter.common;

public record ApiResponse<T>(Integer code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data);
    }
}
