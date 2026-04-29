package com.codex.ignition.agenttools.common;

import java.util.Map;

public class TagSpec {
    public String path;
    public String kind;
    public String name;
    public String tagGroup;
    public Map<String, Object> attributes;

    public String valueSource;
    public String dataType;
    public Object value;
    public String opcServer;
    public String opcItemPath;
    public String expression;

    public String typePath;
    public Map<String, Object> parameters;
    public Map<String, Object> overrides;
}
