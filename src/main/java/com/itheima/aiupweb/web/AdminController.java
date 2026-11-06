package com.itheima.aiupweb.web;

import com.itheima.aiupweb.ai.AiAvailability;
import com.itheima.aiupweb.ai.ConversationRepository;
import com.itheima.aiupweb.ai.IndexStatus;
import com.itheima.aiupweb.ai.IndexingService;
import com.itheima.aiupweb.common.BusinessException;
import com.itheima.aiupweb.forum.Category;
import com.itheima.aiupweb.forum.CategoryForm;
import com.itheima.aiupweb.forum.CategoryService;
import com.itheima.aiupweb.forum.PostService;
import com.itheima.aiupweb.forum.PostStatus;
import com.itheima.aiupweb.user.CurrentUserService;
import com.itheima.aiupweb.user.User;
import com.itheima.aiupweb.user.UserService;
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

@Controller
public class AdminController {

    private final UserService userService;
    private final PostService postService;
    private final CategoryService categoryService;
    private final ConversationRepository conversationRepository;
    private final IndexingService indexingService;
    private final CurrentUserService currentUserService;
    private final AiAvailability aiAvailability;

    public AdminController(UserService userService, PostService postService,
                           CategoryService categoryService,
                           ConversationRepository conversationRepository, IndexingService indexingService,
                           CurrentUserService currentUserService, AiAvailability aiAvailability) {
        this.userService = userService;
        this.postService = postService;
        this.categoryService = categoryService;
        this.conversationRepository = conversationRepository;
        this.indexingService = indexingService;
        this.currentUserService = currentUserService;
        this.aiAvailability = aiAvailability;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("userCount", userService.countUsers());
        model.addAttribute("enabledUserCount", userService.countEnabledUsers());
        model.addAttribute("publishedCount", postService.countPublished());
        model.addAttribute("hiddenCount", postService.countByStatus(PostStatus.HIDDEN));
        model.addAttribute("deletedCount", postService.countByStatus(PostStatus.DELETED));
        model.addAttribute("replyCount", postService.countReplies());
        model.addAttribute("conversationCount", conversationRepository.count());
        model.addAttribute("indexedCount", indexingService.countByStatus(IndexStatus.INDEXED));
        model.addAttribute("failedCount", indexingService.countByStatus(IndexStatus.FAILED));
        model.addAttribute("skippedCount", indexingService.countByStatus(IndexStatus.SKIPPED));
        model.addAttribute("pendingCount", indexingService.countByStatus(IndexStatus.PENDING));
        model.addAttribute("aiAvailable", aiAvailability.isAvailable());
        model.addAttribute("aiStatus", aiAvailability.statusMessage());
        model.addAttribute("recentPosts", postService.adminSearch(null, null,
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent());
        return "admin/dashboard";
    }

    @GetMapping("/admin/posts")
    public String posts(@RequestParam(required = false) String q,
                        @RequestParam(required = false) String status,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        PostStatus postStatus = parseStatus(status);
        String query = StringUtils.hasText(q) ? q.trim() : null;
        if (query != null && query.length() > 100) {
            query = query.substring(0, 100);
        }
        var posts = postService.adminSearch(query, postStatus,
                PageRequest.of(Math.max(0, page), 15, Sort.by(Sort.Direction.DESC, "createdAt")));
        String statusValue = postStatus == null ? "" : postStatus.name();
        var params = new java.util.LinkedHashMap<String, String>();
        params.put("q", query);
        params.put("status", statusValue);
        model.addAttribute("posts", posts);
        model.addAttribute("pageLinks", new PageLinks(posts, "/admin/posts", params));
        model.addAttribute("q", query);
        model.addAttribute("status", statusValue);
        return "admin/posts";
    }

    @PostMapping("/admin/posts/{id}/hide")
    public String hidePost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        postService.hide(id);
        redirectAttributes.addFlashAttribute("flashSuccess", "帖子已隐藏");
        return "redirect:/admin/posts";
    }

    @PostMapping("/admin/posts/{id}/restore")
    public String restorePost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        postService.restore(id);
        redirectAttributes.addFlashAttribute("flashSuccess", "帖子已恢复");
        return "redirect:/admin/posts";
    }

    @PostMapping("/admin/posts/{id}/delete")
    public String deletePost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User admin = currentUserService.requireCurrentUser();
        postService.delete(id, admin.getId(), true);
        redirectAttributes.addFlashAttribute("flashSuccess", "帖子已删除");
        return "redirect:/admin/posts";
    }

    @GetMapping("/admin/users")
    public String users(@RequestParam(required = false) String q,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        String query = StringUtils.hasText(q) ? q.trim() : null;
        var users = userService.search(query,
                PageRequest.of(Math.max(0, page), 15, Sort.by(Sort.Direction.ASC, "id")));
        java.util.Map<String, String> params = new java.util.LinkedHashMap<>();
        params.put("q", query);
        model.addAttribute("users", users);
        model.addAttribute("pageLinks", new PageLinks(users, "/admin/users", params));
        model.addAttribute("q", query);
        return "admin/users";
    }

    @PostMapping("/admin/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User admin = currentUserService.requireCurrentUser();
        try {
            userService.toggleEnabled(id, admin.getId());
            redirectAttributes.addFlashAttribute("flashSuccess", "用户状态已更新");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/categories")
    public String categories(@RequestParam(required = false) Long editId, Model model) {
        CategoryForm form = new CategoryForm();
        if (editId != null) {
            Category category = categoryService.requireById(editId);
            form.setName(category.getName());
            form.setSlug(category.getSlug());
            form.setDescription(category.getDescription());
            form.setSortOrder(category.getSortOrder());
        }
        model.addAttribute("form", form);
        model.addAttribute("editId", editId);
        model.addAttribute("categories", categoryService.listAll());
        return "admin/categories";
    }

    @PostMapping("/admin/categories")
    public String createCategory(@Valid @ModelAttribute("form") CategoryForm form, BindingResult bindingResult,
                                 Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return renderCategories(model, null);
        }
        try {
            categoryService.create(form);
            redirectAttributes.addFlashAttribute("flashSuccess", "分类已创建");
            return "redirect:/admin/categories";
        } catch (BusinessException ex) {
            model.addAttribute("formError", ex.getMessage());
            return renderCategories(model, null);
        }
    }

    @PostMapping("/admin/categories/{id}")
    public String updateCategory(@PathVariable Long id, @Valid @ModelAttribute("form") CategoryForm form,
                                 BindingResult bindingResult, Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return renderCategories(model, id);
        }
        try {
            categoryService.update(id, form);
            redirectAttributes.addFlashAttribute("flashSuccess", "分类已更新");
            return "redirect:/admin/categories";
        } catch (BusinessException ex) {
            model.addAttribute("formError", ex.getMessage());
            return renderCategories(model, id);
        }
    }

    @PostMapping("/admin/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.delete(id);
            redirectAttributes.addFlashAttribute("flashSuccess", "分类已删除");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/admin/ai/reindex")
    public String reindex(RedirectAttributes redirectAttributes) {
        if (!aiAvailability.isAvailable()) {
            redirectAttributes.addFlashAttribute("flashError", aiAvailability.statusMessage());
            return "redirect:/admin";
        }
        indexingService.reindexAll();
        redirectAttributes.addFlashAttribute("flashSuccess", "已提交全量索引任务，请稍后查看状态");
        return "redirect:/admin";
    }

    private String renderCategories(Model model, Long editId) {
        model.addAttribute("categories", categoryService.listAll());
        model.addAttribute("editId", editId);
        return "admin/categories";
    }

    private PostStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return PostStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
