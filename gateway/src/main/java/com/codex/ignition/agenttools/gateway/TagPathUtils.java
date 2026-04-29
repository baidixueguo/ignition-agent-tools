package com.codex.ignition.agenttools.gateway;

import com.codex.ignition.agenttools.common.ErrorCode;
import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.common.tags.paths.parser.TagPathParser;

public final class TagPathUtils {
    private TagPathUtils() {
    }

    public static String normalizeRelativePath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String normalized = path.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public static String buildQualifiedPath(String provider, String relativePath) {
        String cleanProvider = provider == null ? "" : provider.trim();
        String cleanPath = normalizeRelativePath(relativePath);
        if (cleanProvider.isBlank()) {
            throw new TagOperationException(ErrorCode.INVALID_REQUEST, "provider is required");
        }
        return cleanPath.isEmpty() ? "[" + cleanProvider + "]" : "[" + cleanProvider + "]" + cleanPath;
    }

    public static TagPath parse(String provider, String relativePath) {
        String qualified = buildQualifiedPath(provider, relativePath);
        TagPath path = TagPathParser.parseSafe(qualified);
        if (path == null) {
            throw new TagOperationException(ErrorCode.TAG_PATH_INVALID, "Invalid tag path", relativePath);
        }
        return path;
    }
}
