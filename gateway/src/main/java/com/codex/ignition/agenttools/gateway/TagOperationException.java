package com.codex.ignition.agenttools.gateway;

import com.codex.ignition.agenttools.common.ErrorCode;

public class TagOperationException extends RuntimeException {
    private final ErrorCode code;
    private final String path;

    public TagOperationException(ErrorCode code, String message) {
        this(code, message, null);
    }

    public TagOperationException(ErrorCode code, String message, String path) {
        super(message);
        this.code = code;
        this.path = path;
    }

    public ErrorCode getCode() {
        return code;
    }

    public String getPath() {
        return path;
    }
}
