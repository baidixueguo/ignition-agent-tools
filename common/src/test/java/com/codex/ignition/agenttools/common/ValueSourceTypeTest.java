package com.codex.ignition.agenttools.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ValueSourceTypeTest {
    @Test
    void parsesWireValuesCaseInsensitively() {
        assertEquals(ValueSourceType.MEMORY, ValueSourceType.from("memory"));
        assertEquals(ValueSourceType.OPC, ValueSourceType.from("OPC"));
        assertEquals(ValueSourceType.EXPRESSION, ValueSourceType.from("Expression"));
    }

    @Test
    void returnsNullForUnknownValue() {
        assertNull(ValueSourceType.from("unknown"));
    }
}
