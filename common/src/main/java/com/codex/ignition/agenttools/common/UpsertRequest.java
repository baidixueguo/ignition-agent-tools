package com.codex.ignition.agenttools.common;

import java.util.List;

public class UpsertRequest {
    public String provider;
    public Boolean dryRun;
    public Boolean continueOnError;
    public List<TagSpec> items;
}
