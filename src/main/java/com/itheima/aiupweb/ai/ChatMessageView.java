package com.itheima.aiupweb.ai;

import java.time.LocalDateTime;
import java.util.List;

public class ChatMessageView {

    private final String role;
    private final String html;
    private final LocalDateTime createdAt;
    private final List<RetrievedSource> sources;

    public ChatMessageView(String role, String html, LocalDateTime createdAt, List<RetrievedSource> sources) {
        this.role = role;
        this.html = html;
        this.createdAt = createdAt;
        this.sources = sources;
    }

    public String getRole() {
        return role;
    }

    public String getHtml() {
        return html;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<RetrievedSource> getSources() {
        return sources;
    }
}
