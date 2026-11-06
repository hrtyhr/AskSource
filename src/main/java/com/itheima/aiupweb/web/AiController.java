package com.itheima.aiupweb.web;

import com.itheima.aiupweb.ai.AiAvailability;
import com.itheima.aiupweb.ai.AiRateLimiter;
import com.itheima.aiupweb.ai.AiSearchService;
import com.itheima.aiupweb.ai.ChatMessageView;
import com.itheima.aiupweb.ai.ChatService;
import com.itheima.aiupweb.ai.ChatStreamService;
import com.itheima.aiupweb.ai.Conversation;
import com.itheima.aiupweb.ai.SummaryService;
import com.itheima.aiupweb.common.RateLimitException;
import com.itheima.aiupweb.common.AiUnavailableException;
import com.itheima.aiupweb.common.BusinessException;
import com.itheima.aiupweb.common.NotFoundException;
import com.itheima.aiupweb.user.CurrentUserService;
import com.itheima.aiupweb.user.User;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class AiController {

    private final CurrentUserService currentUserService;
    private final ChatService chatService;
    private final ChatStreamService chatStreamService;
    private final AiSearchService aiSearchService;
    private final AiAvailability aiAvailability;
    private final SummaryService summaryService;
    private final AiRateLimiter aiRateLimiter;

    public AiController(CurrentUserService currentUserService, ChatService chatService,
                        ChatStreamService chatStreamService, AiSearchService aiSearchService,
                        AiAvailability aiAvailability, SummaryService summaryService,
                        AiRateLimiter aiRateLimiter) {
        this.currentUserService = currentUserService;
        this.chatService = chatService;
        this.chatStreamService = chatStreamService;
        this.aiSearchService = aiSearchService;
        this.aiAvailability = aiAvailability;
        this.summaryService = summaryService;
        this.aiRateLimiter = aiRateLimiter;
    }

    @GetMapping("/ai")
    public String assistant(@RequestParam(required = false) Long conversationId, Model model) {
        User user = currentUserService.requireCurrentUser();
        List<Conversation> conversations = chatService.listConversations(user.getId());
        List<ChatMessageView> messages = List.of();
        Conversation selected = null;
        if (conversationId != null) {
            selected = chatService.requireConversation(user.getId(), conversationId);
            messages = chatService.messageViews(user.getId(), conversationId);
        }
        model.addAttribute("conversations", conversations);
        model.addAttribute("selectedConversation", selected);
        model.addAttribute("messages", messages);
        model.addAttribute("aiAvailable", aiAvailability.isAvailable());
        model.addAttribute("aiStatus", aiAvailability.statusMessage());
        return "ai/index";
    }

    @GetMapping("/ai/search")
    public String semanticSearch(@RequestParam(required = false) String q, Model model) {
        String query = StringUtils.hasText(q) ? q.trim() : null;
        if (query != null && query.length() > 200) {
            query = query.substring(0, 200);
        }
        model.addAttribute("query", query);
        model.addAttribute("result", query == null ? null : aiSearchService.search(query, 8));
        model.addAttribute("aiAvailable", aiAvailability.isAvailable());
        model.addAttribute("aiStatus", aiAvailability.statusMessage());
        return "ai/search";
    }

    @PostMapping(value = "/api/ai/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter chat(@RequestBody ChatRequest request) {
        User user = currentUserService.requireCurrentUser();
        return chatStreamService.stream(user.getId(), request.getConversationId(), request.getMessage());
    }

    @GetMapping("/api/ai/conversations/{id}/messages")
    @ResponseBody
    public ResponseEntity<List<ChatMessageView>> messages(@PathVariable Long id) {
        User user = currentUserService.requireCurrentUser();
        try {
            return ResponseEntity.ok(chatService.messageViews(user.getId(), id));
        } catch (NotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/posts/{postId}/summary")
    public String summarize(@PathVariable Long postId,
                            @RequestParam(defaultValue = "false") boolean force,
                            RedirectAttributes redirectAttributes) {
        User user = currentUserService.requireCurrentUser();
        if (!aiAvailability.isAvailable()) {
            redirectAttributes.addFlashAttribute("flashError", aiAvailability.statusMessage());
            return "redirect:/posts/" + postId;
        }
        try {
            aiRateLimiter.acquire(user.getId());
        } catch (RateLimitException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/posts/" + postId;
        }
        try {
            summaryService.generate(postId, force);
            redirectAttributes.addFlashAttribute("flashSuccess", "AI 导读已生成");
        } catch (AiUnavailableException | BusinessException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } finally {
            aiRateLimiter.release(user.getId());
        }
        return "redirect:/posts/" + postId;
    }
}
