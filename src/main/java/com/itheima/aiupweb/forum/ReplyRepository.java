package com.itheima.aiupweb.forum;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReplyRepository extends JpaRepository<Reply, Long> {

    List<Reply> findByPostIdAndStatusOrderByCreatedAtAsc(Long postId, ReplyStatus status);

    long countByPostIdAndStatus(Long postId, ReplyStatus status);

    long countByAuthorIdAndStatus(Long authorId, ReplyStatus status);

    Optional<Reply> findByIdAndStatus(Long id, ReplyStatus status);

    @EntityGraph(attributePaths = {"post"})
    Page<Reply> findByAuthorUsernameAndStatusOrderByCreatedAtDesc(String username, ReplyStatus status,
                                                                  Pageable pageable);
}
