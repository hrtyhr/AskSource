package com.itheima.aiupweb.ai;

import com.itheima.aiupweb.common.AiUnavailableException;
import com.itheima.aiupweb.common.BusinessException;
import com.itheima.aiupweb.common.NotFoundException;
import com.itheima.aiupweb.common.RateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ChatStreamService {

    private static final Logger log = LoggerFactory.getLogger(ChatStreamService.class);

    private final ChatService chatService;
    private final ChatClient chatClient;
    private final AiAvailability availability;
    private final AiRateLimiter rateLimiter;
    private final KeywordRetriever keywordRetriever;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final Executor aiExecutor;

    public ChatStreamService(ChatService chatService, ChatClient chatClient, AiAvailability availability,
                             AiRateLimiter rateLimiter, KeywordRetriever keywordRetriever,
                             AiProperties properties, ObjectMapper objectMapper,
                             @Qualifier("aiExecutor") Executor aiExecutor) {
        this.chatService = chatService;
        this.chatClient = chatClient;
        this.availability = availability;
        this.rateLimiter = rateLimiter;
        this.keywordRetriever = keywordRetriever;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.aiExecutor = aiExecutor;
    }

    public SseEmitter stream(Long userId, Long conversationId, String message) {
        SseEmitter emitter = new SseEmitter(properties.getStreamTimeout().toMillis());
        if (!availability.isAvailable()) {
            aiExecutor.execute(() -> {
                try {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("message", availability.statusMessage());
                    payload.put("sources", keywordRetriever.retrieve(message, 5));
                    send(emitter, "degraded", payload);
                } catch (RuntimeException ex) {
                    log.warn("AI 降级检索失败", ex);
                    send(emitter, "error", Map.of("message", "检索服务暂时不可用"));
                } finally {
                    emitter.complete();
                }
            });
            return emitter;
        }

        try {
            rateLimiter.acquire(userId);
        } catch (RateLimitException ex) {
            aiExecutor.execute(() -> {
                send(emitter, "error", Map.of("message", ex.getMessage()));
                emitter.complete();
            });
            return emitter;
        }

        AtomicBoolean released = new AtomicBoolean(false);
        AtomicReference<Disposable> subscription = new AtomicReference<>();
        Runnable release = () -> {
            if (released.compareAndSet(false, true)) {
                rateLimiter.release(userId);
            }
        };
        Runnable cleanup = () -> {
            release.run();
            Disposable disposable = subscription.get();
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
            }
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(throwable -> cleanup.run());

        aiExecutor.execute(() -> {
            try {
                ChatPreparation preparation = chatService.prepare(userId, conversationId, message);
                send(emitter, "meta", Map.of(
                        "conversationId", preparation.getConversationId(),
                        "title", preparation.getConversationTitle()));
                send(emitter, "sources", Map.of("sources", preparation.getSources()));

                StringBuilder answer = new StringBuilder();
                Flux<String> flux = chatClient.prompt()
                        .system(preparation.getSystemPrompt())
                        .messages(preparation.getHistory())
                        .user(preparation.getUserPrompt())
                        .stream()
                        .content();

                Disposable disposable = flux.subscribe(
                        token -> {
                            answer.append(token);
                            send(emitter, "token", Map.of("text", token));
                        },
                        error -> {
                            log.warn("流式问答失败: {}", error.getMessage());
                            send(emitter, "error", Map.of("message", friendlyError(error)));
                            release.run();
                            emitter.complete();
                        },
                        () -> {
                            String content = answer.toString().strip();
                            if (content.isEmpty()) {
                                send(emitter, "error", Map.of("message", "AI 返回了空内容，请稍后重试"));
                            } else {
                                try {
                                    chatService.finish(userId, preparation.getConversationId(),
                                            content, preparation.getSources());
                                    send(emitter, "done", Map.of(
                                            "conversationId", preparation.getConversationId()));
                                } catch (RuntimeException ex) {
                                    log.warn("保存 AI 回复失败", ex);
                                    send(emitter, "error", Map.of("message", "回答已生成，但保存会话失败"));
                                }
                            }
                            release.run();
                            emitter.complete();
                        });
                subscription.set(disposable);
            } catch (RuntimeException ex) {
                log.warn("准备问答失败: {}", ex.getMessage());
                send(emitter, "error", Map.of("message", friendlyError(ex)));
                release.run();
                emitter.complete();
            }
        });
        return emitter;
    }

    private String friendlyError(Throwable throwable) {
        if (throwable instanceof BusinessException || throwable instanceof RateLimitException
                || throwable instanceof NotFoundException) {
            return throwable.getMessage();
        }
        if (throwable instanceof AiUnavailableException) {
            return "AI 服务暂时不可用，请稍后重试";
        }
        return "AI 服务暂时不可用，请稍后重试";
    }

    private void send(SseEmitter emitter, String event, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            emitter.send(SseEmitter.event().name(event).data(json, MediaType.TEXT_PLAIN));
        } catch (IOException | IllegalStateException | JacksonException ex) {
            log.debug("SSE 连接已关闭: {}", ex.getMessage());
        }
    }
}
