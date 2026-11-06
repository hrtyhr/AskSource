package com.itheima.aiupweb.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkerTest {

    private TextChunker chunker;

    @BeforeEach
    void setUp() {
        AiProperties properties = new AiProperties();
        properties.setChunkSize(120);
        properties.setChunkOverlap(20);
        chunker = new TextChunker(properties);
    }

    @Test
    void returnsEmptyForBlankText() {
        assertThat(chunker.chunk("  \n ")).isEmpty();
        assertThat(chunker.chunk(null)).isEmpty();
    }

    @Test
    void keepsShortTextAsSingleChunk() {
        List<String> chunks = chunker.chunk("一个短的技术问题");
        assertThat(chunks).containsExactly("一个短的技术问题");
    }

    @Test
    void splitsLongTextWithinLimitAndCoversContent() {
        String text = ("第一段内容。".repeat(30)) + "\n\n" + ("第二段内容。".repeat(30));
        List<String> chunks = chunker.chunk(text);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk).isNotBlank();
            assertThat(chunk.length()).isLessThanOrEqualTo(120);
        });
        assertThat(String.join("", chunks)).contains("第一段内容");
        assertThat(String.join("", chunks)).contains("第二段内容");
    }

    @Test
    void prefersParagraphBoundary() {
        String first = "A".repeat(60);
        String second = "B".repeat(90);
        List<String> chunks = chunker.chunk(first + "\n\n" + second);

        assertThat(chunks.size()).isGreaterThan(1);
        assertThat(chunks.get(0)).isEqualTo(first);
    }
}
