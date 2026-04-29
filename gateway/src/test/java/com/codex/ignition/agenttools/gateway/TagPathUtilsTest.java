package com.codex.ignition.agenttools.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TagPathUtilsTest {
    @Test
    void normalizesRelativePath() {
        assertEquals("Line1/Temp", TagPathUtils.normalizeRelativePath("\\Line1\\Temp/"));
        assertEquals("", TagPathUtils.normalizeRelativePath("/"));
    }

    @Test
    void buildsQualifiedPath() {
        assertEquals("[default]Line1/Temp", TagPathUtils.buildQualifiedPath("default", "/Line1/Temp"));
        assertEquals("[default]", TagPathUtils.buildQualifiedPath("default", ""));
    }
}
