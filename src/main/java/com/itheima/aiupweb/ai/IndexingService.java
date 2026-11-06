package com.itheima.aiupweb.ai;

import com.itheima.aiupweb.common.TextUtils;
import com.itheima.aiupweb.forum.Post;
import com.itheima.aiupweb.forum.PostRepository;
import com.itheima.aiupweb.forum.PostStatus;
import com.itheima.aiupweb.forum.Reply;
import com.itheima.aiupweb.forum.ReplyRepository;
import com.itheima.aiupweb.forum.ReplyStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class IndexingService {

    private static final Logger log = LoggerFactory.getLogger(IndexingService.class);

    private final PostRepository postRepository;
    private final ReplyRepository replyRepository;
    private final AiDocumentStateRepository stateRepository;
    private final VectorStore vectorStore;
    private final TextChunker textChunker;
    private final AiAvailability availability;
    private final AiProperties properties;
    private final Executor aiExecutor;
    private final ConcurrentHashMap<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    public IndexingService(PostRepository postRepository, ReplyRepository replyRepository,
                           AiDocumentStateRepository stateRepository, VectorStore vectorStore,
                           TextChunker textChunker, AiAvailability availability, AiProperties properties,
                           @Qualifier("aiExecutor") Executor aiExecutor) {
        this.postRepository = postRepository;
        this.replyRepository = replyRepository;
        this.stateRepository = stateRepository;
        this.vectorStore = vectorStore;
        this.textChunker = textChunker;
        this.availability = availability;
        this.properties = properties;
        this.aiExecutor = aiExecutor;
    }

    @Async("aiExecutor")
    public void reindexPostAsync(Long postId) {
        reindexPost(postId);
    }

    public void reindexPost(Long postId) {
        if (!properties.isIndexingEnabled()) {
            return;
        }
        ReentrantLock lock = locks.computeIfAbsent(postId, ignored -> new ReentrantLock());
        lock.lock();
        try {
            doReindex(postId);
        } finally {
            lock.unlock();
        }
    }

    private void doReindex(Long postId) {
        Optional<Post> optionalPost = postRepository.findById(postId);
        if (optionalPost.isEmpty()) {
            stateRepository.deleteByPostId(postId);
            return;
        }
        Post post = optionalPost.get();

        if (!availability.isAvailable()) {
            mark(postId, IndexStatus.SKIPPED, 0, "AI 未配置，未建立索引", false);
            return;
        }

        if (post.getStatus() != PostStatus.PUBLISHED) {
            try {
                vectorStore.delete("postId == '" + postId + "'");
            } catch (RuntimeException ex) {
                log.warn("移除帖子 {} 的旧向量失败", postId, ex);
            }
            mark(postId, IndexStatus.SKIPPED, 0, "帖子不可见，已移除索引", false);
            return;
        }

        AiDocumentState state = stateRepository.findById(postId).orElseGet(() -> new AiDocumentState(postId));
        int attempts = state.getAttempts() + 1;
        mark(postId, IndexStatus.PENDING, attempts, null, true);

        try {
            List<Document> documents = buildDocuments(post);
            vectorStore.delete("postId == '" + postId + "'");
            if (!documents.isEmpty()) {
                vectorStore.add(documents);
            }
            mark(postId, IndexStatus.INDEXED, attempts, null, false);
            AiDocumentState indexed = stateRepository.findById(postId).orElse(null);
            if (indexed != null) {
                indexed.setChunkCount(documents.size());
                indexed.setUpdatedAt(LocalDateTime.now());
                stateRepository.save(indexed);
            }
            log.info("帖子 {} 索引完成，共 {} 个切片", postId, documents.size());
        } catch (RuntimeException ex) {
            String error = TextUtils.truncate(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(), 480);
            mark(postId, IndexStatus.FAILED, attempts, error, false);
            log.warn("帖子 {} 索引失败: {}", postId, error);
        }
    }

    private List<Document> buildDocuments(Post post) {
        List<Document> documents = new ArrayList<>();
        String postBase = "帖子标题：" + post.getTitle()
                + "\n分类：" + post.getCategory().getName()
                + "\n作者：" + post.getAuthor().getDisplayName()
                + "\n\n" + post.getContent();
        List<String> postChunks = textChunker.chunk(postBase);
        for (int i = 0; i < postChunks.size(); i++) {
            Map<String, Object> metadata = baseMetadata(post, "POST", post.getId(), post.getTitle(), i);
            metadata.put("url", "/posts/" + post.getId());
            documents.add(Document.builder()
                    .id("post-" + post.getId() + "-c" + i)
                    .text(postChunks.get(i))
                    .metadata(metadata)
                    .build());
        }

        List<Reply> replies = replyRepository.findByPostIdAndStatusOrderByCreatedAtAsc(
                post.getId(), ReplyStatus.PUBLISHED);
        for (Reply reply : replies) {
            String replyText = "帖子标题：" + post.getTitle()
                    + "\n回复者：" + reply.getAuthor().getDisplayName()
                    + "\n\n" + reply.getContent();
            List<String> replyChunks = textChunker.chunk(replyText);
            for (int i = 0; i < replyChunks.size(); i++) {
                Map<String, Object> metadata = baseMetadata(
                        post, "REPLY", reply.getId(), post.getTitle(), i);
                metadata.put("url", "/posts/" + post.getId() + "#reply-" + reply.getId());
                documents.add(Document.builder()
                        .id("reply-" + reply.getId() + "-c" + i)
                        .text(replyChunks.get(i))
                        .metadata(metadata)
                        .build());
            }
        }
        return documents;
    }

    private Map<String, Object> baseMetadata(Post post, String sourceType, Long sourceId,
                                             String title, int chunkIndex) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("postId", String.valueOf(post.getId()));
        metadata.put("postTitle", title);
        metadata.put("author", post.getAuthor().getUsername());
        metadata.put("category", post.getCategory().getSlug());
        metadata.put("sourceType", sourceType);
        metadata.put("sourceId", String.valueOf(sourceId));
        metadata.put("chunkIndex", chunkIndex);
        return metadata;
    }

    private void mark(Long postId, IndexStatus status, int attempts, String error, boolean incrementAttempts) {
        AiDocumentState state = stateRepository.findById(postId).orElseGet(() -> new AiDocumentState(postId));
        if (incrementAttempts) {
            state.setAttempts(attempts);
        }
        state.setStatus(status);
        state.setError(error);
        state.setUpdatedAt(LocalDateTime.now());
        stateRepository.save(state);
    }

    public void reindexAll() {
        for (Long postId : postRepository.findIdsByStatus(PostStatus.PUBLISHED)) {
            Long id = postId;
            aiExecutor.execute(() -> reindexPost(id));
        }
    }

    public long countByStatus(IndexStatus status) {
        return stateRepository.countByStatus(status);
    }

    public List<Long> recoverablePostIds() {
        return stateRepository.findRecoverablePostIds(
                List.of(IndexStatus.PENDING, IndexStatus.FAILED, IndexStatus.SKIPPED),
                properties.getIndexingRetryLimit());
    }
}
