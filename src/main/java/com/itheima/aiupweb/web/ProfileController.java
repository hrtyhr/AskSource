package com.itheima.aiupweb.web;

import com.itheima.aiupweb.common.MarkdownRenderer;
import com.itheima.aiupweb.common.NotFoundException;
import com.itheima.aiupweb.forum.PostStatus;
import com.itheima.aiupweb.forum.PostService;
import com.itheima.aiupweb.forum.Reply;
import com.itheima.aiupweb.user.User;
import com.itheima.aiupweb.user.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ProfileController {

    private final UserService userService;
    private final PostService postService;
    private final MarkdownRenderer markdownRenderer;

    public ProfileController(UserService userService, PostService postService,
                             MarkdownRenderer markdownRenderer) {
        this.userService = userService;
        this.postService = postService;
        this.markdownRenderer = markdownRenderer;
    }

    @GetMapping("/users/{username}")
    public String profile(@PathVariable String username,
                          @RequestParam(defaultValue = "0") int page,
                          Model model) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
        var posts = postService.searchByAuthor(user.getUsername(), PostStatus.PUBLISHED,
                PageRequest.of(Math.max(0, page), 10, Sort.by(Sort.Direction.DESC, "createdAt")));
        var repliesPage = postService.searchRepliesByAuthor(user.getUsername(),
                PageRequest.of(0, 10));
        List<Reply> replies = repliesPage.getContent();
        Map<Long, String> replyHtml = new LinkedHashMap<>();
        for (Reply reply : replies) {
            replyHtml.put(reply.getId(), markdownRenderer.render(reply.getContent()));
        }
        model.addAttribute("profileUser", user);
        model.addAttribute("posts", posts);
        model.addAttribute("pageLinks", new PageLinks(posts, "/users/" + user.getUsername(),
                java.util.Collections.emptyMap()));
        model.addAttribute("replies", replies);
        model.addAttribute("replyHtml", replyHtml);
        model.addAttribute("postTotal", postService.countPostsByAuthor(user.getId()));
        model.addAttribute("replyTotal", postService.countRepliesByAuthor(user.getId()));
        return "users/profile";
    }
}
