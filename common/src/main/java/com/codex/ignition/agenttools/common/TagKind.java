package com.codex.ignition.agenttools.common;

public enum TagKind {
    FOLDER("folder"),
    ATOMIC("atomic"),
    UDT_INSTANCE("udtInstance");

    private final String wireValue;

    TagKind(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static TagKind from(String raw) {
        if (raw == null) {
            return null;
        }
        for (TagKind value : values()) {
            if (value.wireValue.equalsIgnoreCase(raw)) {
                return value;
            }
        }
        return null;
    }
}
