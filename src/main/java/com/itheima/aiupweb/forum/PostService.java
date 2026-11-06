package com.itheima.aiupweb.forum;

import com.itheima.aiupweb.common.NotFoundException;
import com.itheima.aiupweb.forum.event.PostContentChangedEvent;
import com.itheima.aiupweb.user.User;
import com.itheima.aiupweb.user.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final ReplyRepository replyRepository;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    public PostService(PostRepository postRepository, ReplyRepository replyRepository,
                       CategoryService categoryService, TagService tagService,
                       UserService userService, ApplicationEventPublisher eventPublisher) {
        this.postRepository = postRepository;
        this.replyRepository = replyRepository;
        this.categoryService = categoryService;
        this.tagService = tagService;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Post create(Long userId, PostForm form) {
        User author = userService.requireById(userId);
        Category category = categoryService.requireById(form.getCategoryId());
        Post post = new Post();
        post.setAuthor(author);
        post.setCategory(category);
        post.setTitle(form.getTitle().trim());
        post.setContent(form.getContent().trim());
        post.setTags(tagService.resolveTags(form.getTags()));
        post.setStatus(PostStatus.PUBLISHED);
        Post saved = postRepository.save(post);
        publishChanged(saved.getId());
        return saved;
    }

    @Transactional
    public Post update(Long postId, Long actorId, boolean admin, PostForm form) {
        Post post = requirePost(postId);
        if (!admin && post.getStatus() != PostStatus.PUBLISHED) {
            throw new NotFoundException("帖子不存在");
        }
        checkOwnerOrAdmin(post.getAuthor().getId(), actorId, admin);
        Category category = categoryService.requireById(form.getCategoryId());
        post.setCategory(category);
        post.setTitle(form.getTitle().trim());
        post.setContent(form.getContent().trim());
        post.setTags(tagService.resolveTags(form.getTags()));
        clearSummary(post);
        publishChanged(post.getId());
        return post;
    }

    @Transactional
    public void delete(Long postId, Long actorId, boolean admin) {
        Post post = requirePost(postId);
        checkOwnerOrAdmin(post.getAuthor().getId(), actorId, admin);
        post.setStatus(PostStatus.DELETED);
        publishChanged(post.getId());
    }

    @Transactional
    public void hide(Long postId) {
        Post post = requirePost(postId);
        post.setStatus(PostStatus.HIDDEN);
        publishChanged(post.getId());
    }

    @Transactional
    public void restore(Long postId) {
        Post post = requirePost(postId);
        post.setStatus(PostStatus.PUBLISHED);
        publishChanged(post.getId());
    }

    @Transactional
    public Post viewPost(Long postId, boolean admin) {
        Post post = requirePost(postId);
        if (!post.isPublished() && !admin) {
            throw new NotFoundException("帖子不存在");
        }
        if (post.isPublished()) {
            postRepository.incrementViewCount(postId);
            post = requirePost(postId);
        }
        return post;
    }

    @Transactional(readOnly = true)
    public Post requirePost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("帖子不存在"));
    }

    @Transactional(readOnly = true)
    public Optional<Post> findPublished(Long postId) {
        return postRepository.findByIdAndStatus(postId, PostStatus.PUBLISHED);
    }

    @Transactional(readOnly = true)
    public Post requireEditable(Long postId, Long actorId, boolean admin) {
        Post post = requirePost(postId);
        if (!admin && post.getStatus() == PostStatus.DELETED) {
            throw new NotFoundException("帖子不存在");
        }
        checkOwnerOrAdmin(post.getAuthor().getId(), actorId, admin);
        return post;
    }

    @Transactional(readOnly = true)
    public List<Reply> listReplies(Long postId) {
        return replyRepository.findByPostIdAndStatusOrderByCreatedAtAsc(postId, ReplyStatus.PUBLISHED);
    }

    @Transactional
    public Reply addReply(Long postId, Long userId, ReplyForm form) {
        Post post = postRepository.findByIdAndStatus(postId, PostStatus.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("帖子不存在"));
        User author = userService.requireById(userId);
        Reply reply = new Reply();
        reply.setPost(post);
        reply.setAuthor(author);
        reply.setContent(form.getContent().trim());
        reply.setStatus(ReplyStatus.PUBLISHED);
        Reply saved = replyRepository.save(reply);
        post.setReplyCount(post.getReplyCount() + 1);
        clearSummary(post);
        publishChanged(post.getId());
        return saved;
    }

    @Transactional
    public void editReply(Long replyId, Long actorId, boolean admin, ReplyForm form) {
        Reply reply = replyRepository.findByIdAndStatus(replyId, ReplyStatus.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("回复不存在"));
        checkOwnerOrAdmin(reply.getAuthor().getId(), actorId, admin);
        reply.setContent(form.getContent().trim());
        Post post = reply.getPost();
        clearSummary(post);
        publishChanged(post.getId());
    }

    @Transactional
    public void deleteReply(Long replyId, Long actorId, boolean admin) {
        Reply reply = replyRepository.findByIdAndStatus(replyId, ReplyStatus.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("回复不存在"));
        checkOwnerOrAdmin(reply.getAuthor().getId(), actorId, admin);
        reply.setStatus(ReplyStatus.DELETED);
        Post post = reply.getPost();
        post.setReplyCount(Math.max(0, post.getReplyCount() - 1));
        clearSummary(post);
        publishChanged(post.getId());
    }

    @Transactional(readOnly = true)
    public Page<Post> search(PostSearchCriteria criteria, Pageable pageable) {
        return postRepository.findAll(PostSpecifications.byCriteria(criteria), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Post> searchByAuthor(String username, PostStatus status, Pageable pageable) {
        Specification<Post> spec = PostSpecifications.byAuthor(username);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return postRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Reply> searchRepliesByAuthor(String username, Pageable pageable) {
        return replyRepository.findByAuthorUsernameAndStatusOrderByCreatedAtDesc(
                username, ReplyStatus.PUBLISHED, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Post> adminSearch(String query, PostStatus status, Pageable pageable) {
        PostSearchCriteria criteria = new PostSearchCriteria(
                StringUtils.hasText(query) ? query.trim() : null,
                null,
                null,
                status);
        return postRepository.findAll(PostSpecifications.byCriteria(criteria), pageable);
    }

    @Transactional
    public void storeSummary(Long postId, String summary) {
        Post post = requirePost(postId);
        post.setAiSummary(summary);
        post.setSummaryUpdatedAt(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<Long> allPublishedIds() {
        return postRepository.findIdsByStatus(PostStatus.PUBLISHED);
    }

    @Transactional(readOnly = true)
    public long countPublished() {
        return postRepository.countByStatus(PostStatus.PUBLISHED);
    }

    @Transactional(readOnly = true)
    public long countReplies() {
        return replyRepository.count();
    }

    @Transactional(readOnly = true)
    public long countPostsByAuthor(Long authorId) {
        return postRepository.countByAuthorIdAndStatus(authorId, PostStatus.PUBLISHED);
    }

    @Transactional(readOnly = true)
    public long countRepliesByAuthor(Long authorId) {
        return replyRepository.countByAuthorIdAndStatus(authorId, ReplyStatus.PUBLISHED);
    }

    @Transactional(readOnly = true)
    public long countByStatus(PostStatus status) {
        return postRepository.countByStatus(status);
    }

    private void clearSummary(Post post) {
        post.setAiSummary(null);
        post.setSummaryUpdatedAt(null);
    }

    private void checkOwnerOrAdmin(Long ownerId, Long actorId, boolean admin) {
        if (!admin && !ownerId.equals(actorId)) {
            throw new AccessDeniedException("无权操作该内容");
        }
    }

    private void publishChanged(Long postId) {
        eventPublisher.publishEvent(new PostContentChangedEvent(postId));
    }
}
