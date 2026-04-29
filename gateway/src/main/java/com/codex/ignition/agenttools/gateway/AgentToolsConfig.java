package com.codex.ignition.agenttools.gateway;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class AgentToolsConfig {
    private static final String PROP_PREFIX = "agenttools.";
    private static final String ENV_PREFIX = "IGNITION_AGENT_TOOLS_";

    private final boolean enabled;
    private final String apiKeyHash;
    private final int maxBatchSize;
    private final int maxDeleteCount;
    private final String sourceDescription;

    private AgentToolsConfig(
        boolean enabled,
        String apiKeyHash,
        int maxBatchSize,
        int maxDeleteCount,
        String sourceDescription
    ) {
        this.enabled = enabled;
        this.apiKeyHash = apiKeyHash;
        this.maxBatchSize = maxBatchSize;
        this.maxDeleteCount = maxDeleteCount;
        this.sourceDescription = sourceDescription;
    }

    public static AgentToolsConfig load() {
        String rawApiKey = readSetting("apiKey", "API_KEY");
        String rawHash = readSetting("apiKeyHash", "API_KEY_HASH");
        String hash = blankToNull(rawHash);
        String source = hash != null ? "hash" : "none";
        if (hash == null && rawApiKey != null && !rawApiKey.isBlank()) {
            hash = sha256(rawApiKey);
            source = "bootstrap-api-key";
        }

        boolean configured = hash != null;
        boolean enabled = configured && parseBoolean(readSetting("enabled", "ENABLED"), true);
        int maxBatchSize = parseInteger(readSetting("maxBatchSize", "MAX_BATCH_SIZE"), 100);
        int maxDeleteCount = parseInteger(readSetting("maxDeleteCount", "MAX_DELETE_COUNT"), 100);

        return new AgentToolsConfig(enabled, hash, maxBatchSize, maxDeleteCount, source);
    }

    private static String readSetting(String propertyName, String envName) {
        String propertyValue = System.getProperty(PROP_PREFIX + propertyName);
        if (propertyValue != null) {
            return propertyValue;
        }
        return System.getenv(ENV_PREFIX + envName);
    }

    private static boolean parseBoolean(String raw, boolean defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(raw);
    }

    private static int parseInteger(String raw, int defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isConfigured() {
        return apiKeyHash != null && !apiKeyHash.isBlank();
    }

    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    public int getMaxDeleteCount() {
        return maxDeleteCount;
    }

    public boolean matchesApiKey(String apiKey) {
        if (!isConfigured() || apiKey == null || apiKey.isBlank()) {
            return false;
        }
        return sha256(apiKey).equals(apiKeyHash);
    }

    public Map<String, Object> describe() {
        Map<String, Object> description = new LinkedHashMap<>();
        description.put("enabled", enabled);
        description.put("configured", isConfigured());
        description.put("maxBatchSize", maxBatchSize);
        description.put("maxDeleteCount", maxDeleteCount);
        description.put("credentialSource", sourceDescription);
        return description;
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte current : bytes) {
                builder.append(String.format(Locale.ROOT, "%02x", current));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
