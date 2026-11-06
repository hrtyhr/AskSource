package com.itheima.aiupweb.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownRendererTest {

    private final MarkdownRenderer renderer = new MarkdownRenderer();

    @Test
    void stripsScriptAndEventHandlers() {
        String html = renderer.render("""
                <script>alert('xss')</script>
                <img src="x" onerror="alert(1)">
                <a href="javascript:alert(1)">bad link</a>
                """);

        assertThat(html).doesNotContain("<script", "onerror", "javascript:");
        assertThat(html).contains("bad link");
    }

    @Test
    void keepsCodeLanguageAndAddsSafeLinkRel() {
        String html = renderer.render("""
                ```java
                System.out.println("hello");
                ```

                [文档](https://example.com)
                """);

        assertThat(html).contains("language-java");
        assertThat(html).contains("rel=\"nofollow noopener noreferrer\"");
        assertThat(html).contains("https://example.com");
    }

    @Test
    void returnsEmptyForBlankMarkdown() {
        assertThat(renderer.render(null)).isEmpty();
        assertThat(renderer.render(" ")).isEmpty();
    }
}
