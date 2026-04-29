package com.codex.ignition.agenttools.common;

public final class AgentToolsModule {
    public static final String MODULE_ID = "com.codex.ignition.agenttools.AgentTools";
    public static final String MODULE_VERSION = "0.1.0";
    public static final String MOUNT_ALIAS = "agent-tools";
    public static final String API_VERSION = "v1";
    public static final String API_BASE_PATH = "/system/data/" + MOUNT_ALIAS + "/" + API_VERSION;

    private AgentToolsModule() {
    }
}
