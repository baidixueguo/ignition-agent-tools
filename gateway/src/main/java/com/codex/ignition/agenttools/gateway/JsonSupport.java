package com.codex.ignition.agenttools.gateway;

import com.inductiveautomation.ignition.common.gson.Gson;
import com.inductiveautomation.ignition.common.gson.GsonBuilder;

public final class JsonSupport {
    public static final Gson GSON = new GsonBuilder()
        .serializeNulls()
        .disableHtmlEscaping()
        .create();

    private JsonSupport() {
    }
}
