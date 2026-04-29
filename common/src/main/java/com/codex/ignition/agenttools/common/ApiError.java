package com.codex.ignition.agenttools.common;

public class ApiError {
    public String code;
    public String message;
    public String path;

    public ApiError() {
    }

    public ApiError(String code, String message, String path) {
        this.code = code;
        this.message = message;
        this.path = path;
    }

    public static ApiError of(ErrorCode code, String message) {
        return new ApiError(code.code(), message, null);
    }

    public static ApiError of(ErrorCode code, String message, String path) {
        return new ApiError(code.code(), message, path);
    }
}
