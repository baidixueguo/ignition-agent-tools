package com.codex.ignition.agenttools.common;

import java.util.Collections;
import java.util.List;

public class ApiResponse<T> {
    public boolean success;
    public String requestId;
    public T data;
    public List<ApiError> errors;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, String requestId, T data, List<ApiError> errors) {
        this.success = success;
        this.requestId = requestId;
        this.data = data;
        this.errors = errors;
    }

    public static <T> ApiResponse<T> success(String requestId, T data) {
        return new ApiResponse<>(true, requestId, data, Collections.emptyList());
    }

    public static <T> ApiResponse<T> failure(String requestId, List<ApiError> errors) {
        return new ApiResponse<>(false, requestId, null, errors);
    }
}
