package com.itheima.aiupweb.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextUtilsTest {

    @Test
    void escapesLikeWildcardsWithCustomEscapeCharacter() {
        assertThat(TextUtils.escapeLike("100%_done!")).isEqualTo("100!%!_done!!");
    }

    @Test
    void truncatesLongTextSafely() {
        assertThat(TextUtils.truncate("abcdef", 4)).isEqualTo("abc…");
        assertThat(TextUtils.truncate("abc", 10)).isEqualTo("abc");
    }

    @Test
    void plainSnippetRemovesMarkdownMarkers() {
        String snippet = TextUtils.plainSnippet("## 标题\n\n`code` 和 **重点**", 40);
        assertThat(snippet).doesNotContain("##", "**", "`");
        assertThat(snippet).contains("标题", "重点");
    }
}
