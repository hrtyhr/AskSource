package com.itheima.aiupweb.web;

import com.itheima.aiupweb.forum.PostSearchCriteria;
import com.itheima.aiupweb.forum.PostService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SearchController {

    private final PostService postService;

    public SearchController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        String query = StringUtils.hasText(q) ? q.trim() : null;
        if (query != null && query.length() > 100) {
            query = query.substring(0, 100);
        }
        var criteria = PostSearchCriteria.published(query, null, null);
        var posts = postService.search(criteria,
                PageRequest.of(Math.max(0, page), 10, Sort.by(Sort.Direction.DESC, "createdAt")));
        model.addAttribute("q", query);
        model.addAttribute("posts", posts);
        java.util.Map<String, String> pageParams = new java.util.LinkedHashMap<>();
        pageParams.put("q", query);
        model.addAttribute("pageLinks", new PageLinks(posts, "/search", pageParams));
        return "search";
    }
}
