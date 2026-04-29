package com.codex.ignition.agenttools.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AgentToolsConfigTest {
    @Test
    void hashesValuesDeterministically() {
        assertEquals(
            "2bb80d537b1da3e38bd30361aa855686bde0eacd7162fef6a25fe97bf527a25b",
            AgentToolsConfig.sha256("secret")
        );
    }
}
