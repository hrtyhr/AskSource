package com.itheima.aiupweb.ai;

import com.itheima.aiupweb.common.AiUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiSearchService {

    private static final Logger log = LoggerFactory.getLogger(AiSearchService.class);

    private final SemanticRetriever semanticRetriever;
    private final KeywordRetriever keywordRetriever;
    private final AiAvailability availability;

    public AiSearchService(SemanticRetriever semanticRetriever, KeywordRetriever keywordRetriever,
                           AiAvailability availability) {
        this.semanticRetriever = semanticRetriever;
        this.keywordRetriever = keywordRetriever;
        this.availability = availability;
    }

    public AiSearchResult search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return new AiSearchResult(List.of(), false, null);
        }
        if (!availability.isAvailable()) {
            return new AiSearchResult(keywordRetriever.retrieve(query, limit), true,
                    availability.statusMessage() + "，当前展示关键词匹配结果。");
        }
        try {
            List<RetrievedSource> sources = semanticRetriever.retrieve(query, limit);
            return new AiSearchResult(sources, false, null);
        } catch (AiUnavailableException ex) {
            log.warn("语义检索失败，降级为关键词检索: {}", ex.getMessage());
            return new AiSearchResult(keywordRetriever.retrieve(query, limit), true,
                    "语义检索暂时不可用，当前展示关键词匹配结果。");
        }
    }
}
