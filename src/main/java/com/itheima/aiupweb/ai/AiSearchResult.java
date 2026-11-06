package com.itheima.aiupweb.ai;

import java.util.List;

public class AiSearchResult {

    private final List<RetrievedSource> sources;
    private final boolean degraded;
    private final String message;

    public AiSearchResult(List<RetrievedSource> sources, boolean degraded, String message) {
        this.sources = sources;
        this.degraded = degraded;
        this.message = message;
    }

    public List<RetrievedSource> getSources() {
        return sources;
    }

    public boolean isDegraded() {
        return degraded;
    }

    public String getMessage() {
        return message;
    }
}
