package com.itheima.aiupweb.web;

import com.itheima.aiupweb.ai.AiAvailability;
import com.itheima.aiupweb.common.BusinessException;
import com.itheima.aiupweb.common.MarkdownRenderer;
import com.itheima.aiupweb.forum.CategoryService;
import com.itheima.aiupweb.forum.Post;
import com.itheima.aiupweb.forum.PostForm;
import com.itheima.aiupweb.forum.PostSearchCriteria;
import com.itheima.aiupweb.forum.PostService;
import com.itheima.aiupweb.forum.Reply;
import com.itheima.aiupweb.forum.ReplyForm;
import com.itheima.aiupweb.forum.TagService;
import com.itheima.aiupweb.user.CurrentUserService;
import com.itheima.aiupweb.user.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PostController {

    private static final int PAGE_SIZE = 10;

    private final PostService postService;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final CurrentUserService currentUserService;
    private final MarkdownRenderer markdownRenderer;
    private final AiAvailability aiAvailability;

    public PostController(PostService postService, CategoryService categoryService, TagService tagService,
                          CurrentUserService currentUserService, MarkdownRenderer markdownRenderer,
                          AiAvailability aiAvailability) {
        this.postService = postService;
        this.categoryService = categoryService;
        this.tagService = tagService;
        this.currentUserService = currentUserService;
        this.markdownRenderer = markdownRenderer;
        this.aiAvailability = aiAvailability;
    }

    @GetMapping("/posts")
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String category,
                       @RequestParam(required = false) String tag,
                       @RequestParam(defaultValue = "latest") String sort,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        String query = normalizeQuery(q);
        var criteria = PostSearchCriteria.published(query, blankToNull(category), blankToNull(tag));
        var posts = postService.search(criteria, pageRequest(page, sort));
        Map<String, String> pageParams = new LinkedHashMap<>();
        pageParams.put("q", query);
        pageParams.put("category", blankToNull(category));
        pageParams.put("tag", blankToNull(tag));
        pageParams.put("sort", normalizeSort(sort));
        model.addAttribute("posts", posts);
        model.addAttribute("pageLinks", new PageLinks(posts, "/posts", pageParams));
        model.addAttribute("q", query);
        model.addAttribute("selectedCategory", blankToNull(category));
        model.addAttribute("selectedTag", blankToNull(tag));
        model.addAttribute("sort", normalizeSort(sort));
        model.addAttribute("categories", categoryService.listAll());
        return "posts/list";
    }

    @GetMapping("/posts/{id}")
    public String detail(@PathVariable Long id, Model model) {
        User viewer = currentUserService.currentUser().orElse(null);
        boolean admin = viewer != null && viewer.isAdmin();
        Post post = postService.viewPost(id, admin);
        List<Reply> replies = postService.listReplies(id);
        Map<Long, String> replyHtml = new LinkedHashMap<>();
        for (Reply reply : replies) {
            replyHtml.put(reply.getId(), markdownRenderer.render(reply.getContent()));
        }
        model.addAttribute("post", post);
        model.addAttribute("contentHtml", markdownRenderer.render(post.getContent()));
        model.addAttribute("summaryHtml", markdownRenderer.render(post.getAiSummary()));
        model.addAttribute("replies", replies);
        model.addAttribute("replyHtml", replyHtml);
        model.addAttribute("replyForm", new ReplyForm());
        model.addAttribute("aiAvailable", aiAvailability.isAvailable());
        model.addAttribute("aiStatus", aiAvailability.statusMessage());
        model.addAttribute("viewer", viewer);
        return "posts/detail";
    }

    @GetMapping("/posts/new")
    public String newPostForm(Model model) {
        PostForm form = new PostForm();
        var categories = categoryService.listAll();
        if (!categories.isEmpty()) {
            form.setCategoryId(categories.get(0).getId());
        }
        model.addAttribute("postForm", form);
        model.addAttribute("categories", categories);
        model.addAttribute("mode", "create");
        return "posts/form";
    }

    @PostMapping("/posts")
    public String createPost(@Valid @ModelAttribute("postForm") PostForm form, BindingResult bindingResult,
                             Model model, RedirectAttributes redirectAttributes) {
        User user = currentUserService.requireCurrentUser();
        if (bindingResult.hasErrors()) {
            return renderPostForm(model, "create", null);
        }
        try {
            Post post = postService.create(user.getId(), form);
            redirectAttributes.addFlashAttribute("flashSuccess", "帖子发布成功");
            return "redirect:/posts/" + post.getId();
        } catch (BusinessException ex) {
            model.addAttribute("formError", ex.getMessage());
            return renderPostForm(model, "create", null);
        }
    }

    @GetMapping("/posts/{id}/edit")
    public String editPostForm(@PathVariable Long id, Model model) {
        User user = currentUserService.requireCurrentUser();
        Post post = postService.requireEditable(id, user.getId(), user.isAdmin());
        PostForm form = new PostForm();
        form.setCategoryId(post.getCategory().getId());
        form.setTitle(post.getTitle());
        form.setContent(post.getContent());
        form.setTags(String.join(", ", tagService.tagNamesOf(post)));
        model.addAttribute("postForm", form);
        model.addAttribute("categories", categoryService.listAll());
        model.addAttribute("mode", "edit");
        model.addAttribute("postId", post.getId());
        return "posts/form";
    }

    @PostMapping("/posts/{id}/edit")
    public String updatePost(@PathVariable Long id, @Valid @ModelAttribute("postForm") PostForm form,
                             BindingResult bindingResult, Model model,
                             RedirectAttributes redirectAttributes) {
        User user = currentUserService.requireCurrentUser();
        if (bindingResult.hasErrors()) {
            return renderPostForm(model, "edit", id);
        }
        try {
            postService.update(id, user.getId(), user.isAdmin(), form);
            redirectAttributes.addFlashAttribute("flashSuccess", "帖子已更新");
            return "redirect:/posts/" + id;
        } catch (BusinessException ex) {
            model.addAttribute("formError", ex.getMessage());
            return renderPostForm(model, "edit", id);
        }
    }

    @PostMapping("/posts/{id}/delete")
    public String deletePost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = currentUserService.requireCurrentUser();
        postService.delete(id, user.getId(), user.isAdmin());
        redirectAttributes.addFlashAttribute("flashSuccess", "帖子已删除");
        return "redirect:/posts";
    }

    @PostMapping("/posts/{id}/replies")
    public String addReply(@PathVariable Long id, @Valid @ModelAttribute("replyForm") ReplyForm form,
                           BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        User user = currentUserService.requireCurrentUser();
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("flashError",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/posts/" + id + "#replies";
        }
        try {
            Reply reply = postService.addReply(id, user.getId(), form);
            redirectAttributes.addFlashAttribute("flashSuccess", "回复已发布");
            return "redirect:/posts/" + id + "#reply-" + reply.getId();
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/posts/" + id + "#replies";
        }
    }

    @PostMapping("/posts/{postId}/replies/{replyId}/edit")
    public String editReply(@PathVariable Long postId, @PathVariable Long replyId,
                            @Valid @ModelAttribute("replyForm") ReplyForm form, BindingResult bindingResult,
                            RedirectAttributes redirectAttributes) {
        User user = currentUserService.requireCurrentUser();
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("flashError",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/posts/" + postId + "#reply-" + replyId;
        }
        try {
            postService.editReply(replyId, user.getId(), user.isAdmin(), form);
            redirectAttributes.addFlashAttribute("flashSuccess", "回复已更新");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/posts/" + postId + "#reply-" + replyId;
    }

    @PostMapping("/posts/{postId}/replies/{replyId}/delete")
    public String deleteReply(@PathVariable Long postId, @PathVariable Long replyId,
                              RedirectAttributes redirectAttributes) {
        User user = currentUserService.requireCurrentUser();
        postService.deleteReply(replyId, user.getId(), user.isAdmin());
        redirectAttributes.addFlashAttribute("flashSuccess", "回复已删除");
        return "redirect:/posts/" + postId + "#replies";
    }

    private String renderPostForm(Model model, String mode, Long postId) {
        model.addAttribute("categories", categoryService.listAll());
        model.addAttribute("mode", mode);
        if (postId != null) {
            model.addAttribute("postId", postId);
        }
        return "posts/form";
    }

    private PageRequest pageRequest(int page, String sort) {
        String normalizedSort = normalizeSort(sort);
        Sort order = "hot".equals(normalizedSort)
                ? Sort.by(Sort.Direction.DESC, "replyCount", "viewCount", "createdAt")
                : Sort.by(Sort.Direction.DESC, "createdAt");
        return PageRequest.of(Math.max(0, page), PAGE_SIZE, order);
    }

    private String normalizeSort(String sort) {
        return "hot".equals(sort) ? "hot" : "latest";
    }

    private String normalizeQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        String trimmed = query.trim();
        return trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
