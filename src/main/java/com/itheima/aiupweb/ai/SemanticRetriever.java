package com.itheima.aiupweb.ai;

import com.itheima.aiupweb.common.AiUnavailableException;
import com.itheima.aiupweb.common.TextUtils;
import com.itheima.aiupweb.forum.Post;
import com.itheima.aiupweb.forum.PostRepository;
import com.itheima.aiupweb.forum.PostStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SemanticRetriever {

    private static final Logger log = LoggerFactory.getLogger(SemanticRetriever.class);

    private final VectorStore vectorStore;
    private final PostRepository postRepository;
    private final AiProperties properties;
    private final AiAvailability availability;

    public SemanticRetriever(VectorStore vectorStore, PostRepository postRepository,
                             AiProperties properties, AiAvailability availability) {
        this.vectorStore = vectorStore;
        this.postRepository = postRepository;
        this.properties = properties;
        this.availability = availability;
    }

    public List<RetrievedSource> retrieve(String query, int limit) {
        if (!availability.isAvailable()) {
            throw new AiUnavailableException(availability.statusMessage());
        }
        if (query == null || query.isBlank()) {
            return List.of();
        }
        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(properties.getTopK())
                    .similarityThreshold(properties.getSimilarityThreshold())
                    .build();
            List<Document> documents = vectorStore.similaritySearch(request);
            if (documents == null || documents.isEmpty()) {
                return List.of();
            }
            Map<Long, Post> posts = loadPublishedPosts(documents);
            Map<String, RetrievedSource> unique = new LinkedHashMap<>();
            for (Document document : documents) {
                RetrievedSource source = toSource(document, posts);
                if (source == null) {
                    continue;
                }
                String key = source.getPostId() + ":" + source.getSourceType() + ":" + source.getSourceId();
                unique.putIfAbsent(key, source);
                if (unique.size() >= limit) {
                    break;
                }
            }
            return new ArrayList<>(unique.values());
        } catch (RuntimeException ex) {
            log.warn("语义检索失败: {}", ex.getMessage());
            throw new AiUnavailableException("语义检索暂时不可用", ex);
        }
    }

    private Map<Long, Post> loadPublishedPosts(List<Document> documents) {
        Set<Long> ids = new HashSet<>();
        for (Document document : documents) {
            Long postId = parseLong(String.valueOf(document.getMetadata().get("postId")));
            if (postId != null) {
                ids.add(postId);
            }
        }
        Map<Long, Post> posts = new LinkedHashMap<>();
        postRepository.findAllById(ids).stream()
                .filter(post -> post.getStatus() == PostStatus.PUBLISHED)
                .forEach(post -> posts.put(post.getId(), post));
        return posts;
    }

    private RetrievedSource toSource(Document document, Map<Long, Post> posts) {
        Map<String, Object> metadata = document.getMetadata();
        Long postId = parseLong(String.valueOf(metadata.get("postId")));
        if (postId == null) {
            return null;
        }
        Post post = posts.get(postId);
        if (post == null) {
            return null;
        }
        String sourceType = String.valueOf(metadata.getOrDefault("sourceType", "POST"));
        Long sourceId = parseLong(String.valueOf(metadata.get("sourceId")));
        if (sourceId == null) {
            sourceId = postId;
        }
        String url = "POST".equals(sourceType)
                ? "/posts/" + postId
                : "/posts/" + postId + "#reply-" + sourceId;
        double score = document.getScore() == null ? 0.0 : document.getScore();
        String snippet = TextUtils.truncate(document.getText(), 700);
        return new RetrievedSource(postId, sourceType, sourceId, post.getTitle(), snippet, url, score);
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
