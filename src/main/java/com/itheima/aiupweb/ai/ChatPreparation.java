package com.itheima.aiupweb.ai;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

public class ChatPreparation {

    private final Long conversationId;
    private final String conversationTitle;
    private final List<RetrievedSource> sources;
    private final List<Message> history;
    private final String systemPrompt;
    private final String userPrompt;

    public ChatPreparation(Long conversationId, String conversationTitle, List<RetrievedSource> sources,
                           List<Message> history, String systemPrompt, String userPrompt) {
        this.conversationId = conversationId;
        this.conversationTitle = conversationTitle;
        this.sources = sources;
        this.history = history;
        this.systemPrompt = systemPrompt;
        this.userPrompt = userPrompt;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public String getConversationTitle() {
        return conversationTitle;
    }

    public List<RetrievedSource> getSources() {
        return sources;
    }

    public List<Message> getHistory() {
        return history;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String getUserPrompt() {
        return userPrompt;
    }
}
