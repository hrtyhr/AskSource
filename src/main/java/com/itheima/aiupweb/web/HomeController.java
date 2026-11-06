package com.itheima.aiupweb.web;

import com.itheima.aiupweb.ai.AiAvailability;
import com.itheima.aiupweb.forum.CategoryService;
import com.itheima.aiupweb.forum.PostSearchCriteria;
import com.itheima.aiupweb.forum.PostService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final CategoryService categoryService;
    private final PostService postService;
    private final AiAvailability aiAvailability;

    public HomeController(CategoryService categoryService, PostService postService,
                          AiAvailability aiAvailability) {
        this.categoryService = categoryService;
        this.postService = postService;
        this.aiAvailability = aiAvailability;
    }

    @GetMapping("/")
    public String home(Model model) {
        var categories = categoryService.listAll();
        var latest = postService.search(
                PostSearchCriteria.published(null, null, null),
                PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "createdAt")));
        var hot = postService.search(
                PostSearchCriteria.published(null, null, null),
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "replyCount", "viewCount", "createdAt")));
        model.addAttribute("categories", categories);
        model.addAttribute("latestPosts", latest.getContent());
        model.addAttribute("hotPosts", hot.getContent());
        model.addAttribute("postCount", postService.countPublished());
        model.addAttribute("aiAvailable", aiAvailability.isAvailable());
        model.addAttribute("aiStatus", aiAvailability.statusMessage());
        return "index";
    }
}
