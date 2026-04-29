package com.codex.ignition.agenttools.gateway;

import com.codex.ignition.agenttools.common.ApiError;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public final class AuditLogger {
    private static final Logger LOGGER = LogManager.getLogger("IgnitionAgentToolsAudit");

    public void log(
        String requestId,
        String operation,
        String provider,
        int itemCount,
        boolean dryRun,
        boolean success,
        String remoteAddress,
        List<ApiError> errors
    ) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("requestId", requestId);
        event.put("operation", operation);
        event.put("provider", provider);
        event.put("itemCount", itemCount);
        event.put("dryRun", dryRun);
        event.put("success", success);
        event.put("remoteAddress", remoteAddress);
        event.put("errors", errors);
        LOGGER.info(JsonSupport.GSON.toJson(event));
    }
}
