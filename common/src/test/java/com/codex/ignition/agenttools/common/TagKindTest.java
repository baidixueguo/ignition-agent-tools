package com.codex.ignition.agenttools.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TagKindTest {
    @Test
    void parsesWireValuesCaseInsensitively() {
        assertEquals(TagKind.FOLDER, TagKind.from("folder"));
        assertEquals(TagKind.ATOMIC, TagKind.from("Atomic"));
        assertEquals(TagKind.UDT_INSTANCE, TagKind.from("udtInstance"));
    }

    @Test
    void returnsNullForUnknownValue() {
        assertNull(TagKind.from("unknown"));
    }
}
