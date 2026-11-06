package com.itheima.aiupweb.ai;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    private final AiProperties properties;

    public TextChunker(AiProperties properties) {
        this.properties = properties;
    }

    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        int maxSize = Math.max(100, properties.getChunkSize());
        int overlap = Math.max(0, Math.min(properties.getChunkOverlap(), maxSize / 2));
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').strip();

        if (normalized.length() <= maxSize) {
            return List.of(normalized);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + maxSize, normalized.length());
            if (end < normalized.length()) {
                int boundary = findBoundary(normalized, start + maxSize / 2, end);
                if (boundary > start) {
                    end = boundary;
                }
            }
            String chunk = normalized.substring(start, end).strip();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }
            if (end >= normalized.length()) {
                break;
            }
            int nextStart = Math.max(end - overlap, start + 1);
            start = nextStart;
        }
        return chunks;
    }

    private int findBoundary(String text, int from, int to) {
        int paragraph = text.lastIndexOf("\n\n", to);
        if (paragraph >= from) {
            return paragraph;
        }
        int line = text.lastIndexOf('\n', to);
        if (line >= from) {
            return line;
        }
        int sentence = Math.max(text.lastIndexOf('。', to), text.lastIndexOf('.', to));
        if (sentence >= from) {
            return sentence + 1;
        }
        int space = text.lastIndexOf(' ', to);
        return space >= from ? space + 1 : to;
    }
}
