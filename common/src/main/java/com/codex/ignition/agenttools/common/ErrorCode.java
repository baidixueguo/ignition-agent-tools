package com.codex.ignition.agenttools.common;

public enum ErrorCode {
    UNAUTHORIZED("unauthorized"),
    DISABLED("disabled"),
    INVALID_REQUEST("invalid_request"),
    PROVIDER_NOT_FOUND("provider_not_found"),
    TAG_GROUP_NOT_FOUND("tag_group_not_found"),
    TAG_PATH_INVALID("tag_path_invalid"),
    TAG_OPERATION_FAILED("tag_operation_failed"),
    CONFIGURATION_ERROR("configuration_error"),
    BATCH_LIMIT_EXCEEDED("batch_limit_exceeded"),
    DELETE_LIMIT_EXCEEDED("delete_limit_exceeded"),
    UNSUPPORTED_ATTRIBUTE("unsupported_attribute");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
