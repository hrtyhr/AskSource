package com.itheima.aiupweb.ai;

import com.itheima.aiupweb.common.TextUtils;
import com.itheima.aiupweb.forum.Post;
import com.itheima.aiupweb.forum.PostSearchCriteria;
import com.itheima.aiupweb.forum.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class KeywordRetriever {

    private final PostService postService;

    public KeywordRetriever(PostService postService) {
        this.postService = postService;
    }

    public List<RetrievedSource> retrieve(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        Page<Post> page = postService.search(
                PostSearchCriteria.published(query.trim(), null, null),
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<RetrievedSource> sources = new ArrayList<>();
        for (Post post : page.getContent()) {
            sources.add(new RetrievedSource(
                    post.getId(),
                    "POST",
                    post.getId(),
                    post.getTitle(),
                    TextUtils.plainSnippet(post.getContent(), 500),
                    "/posts/" + post.getId(),
                    0.0));
        }
        return sources;
    }
}
