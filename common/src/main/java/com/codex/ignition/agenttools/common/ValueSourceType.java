package com.codex.ignition.agenttools.common;

public enum ValueSourceType {
    MEMORY("memory"),
    OPC("opc"),
    EXPRESSION("expression");

    private final String wireValue;

    ValueSourceType(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static ValueSourceType from(String raw) {
        if (raw == null) {
            return null;
        }
        for (ValueSourceType value : values()) {
            if (value.wireValue.equalsIgnoreCase(raw)) {
                return value;
            }
        }
        return null;
    }
}
