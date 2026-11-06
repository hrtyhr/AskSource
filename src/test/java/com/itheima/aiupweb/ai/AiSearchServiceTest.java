package com.itheima.aiupweb.ai;

import com.itheima.aiupweb.common.AiUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiSearchServiceTest {

    @Mock
    private SemanticRetriever semanticRetriever;

    @Mock
    private KeywordRetriever keywordRetriever;

    @Mock
    private AiAvailability availability;

    @InjectMocks
    private AiSearchService service;

    @Test
    void fallsBackToKeywordWhenAiNotConfigured() {
        RetrievedSource keyword = source("关键词结果");
        when(availability.isAvailable()).thenReturn(false);
        when(availability.statusMessage()).thenReturn("未配置 DASHSCOPE_API_KEY");
        when(keywordRetriever.retrieve("Redis", 5)).thenReturn(List.of(keyword));

        AiSearchResult result = service.search("Redis", 5);

        assertThat(result.isDegraded()).isTrue();
        assertThat(result.getSources()).containsExactly(keyword);
        assertThat(result.getMessage()).contains("关键词");
        verify(semanticRetriever, never()).retrieve("Redis", 5);
    }

    @Test
    void usesSemanticResultsWhenAvailable() {
        RetrievedSource semantic = source("语义结果");
        when(availability.isAvailable()).thenReturn(true);
        when(semanticRetriever.retrieve("Redis", 5)).thenReturn(List.of(semantic));

        AiSearchResult result = service.search("Redis", 5);

        assertThat(result.isDegraded()).isFalse();
        assertThat(result.getSources()).containsExactly(semantic);
        verify(keywordRetriever, never()).retrieve("Redis", 5);
    }

    @Test
    void degradesWhenSemanticSearchFails() {
        RetrievedSource keyword = source("降级结果");
        when(availability.isAvailable()).thenReturn(true);
        when(semanticRetriever.retrieve("Redis", 5)).thenThrow(new AiUnavailableException("vector down"));
        when(keywordRetriever.retrieve("Redis", 5)).thenReturn(List.of(keyword));

        AiSearchResult result = service.search("Redis", 5);

        assertThat(result.isDegraded()).isTrue();
        assertThat(result.getSources()).containsExactly(keyword);
    }

    private RetrievedSource source(String title) {
        return new RetrievedSource(1L, "POST", 1L, title, "snippet", "/posts/1", 0.8);
    }
}
