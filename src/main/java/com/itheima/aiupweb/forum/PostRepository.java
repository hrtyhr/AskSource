package com.itheima.aiupweb.forum;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    Optional<Post> findByIdAndStatus(Long id, PostStatus status);

    Page<Post> findByAuthorUsernameAndStatus(String username, PostStatus status, Pageable pageable);

    long countByStatus(PostStatus status);

    long countByCategoryId(Long categoryId);

    long countByAuthorIdAndStatus(Long authorId, PostStatus status);

    List<Post> findAllByStatus(PostStatus status);

    @Query("select p.id from Post p where p.status = :status")
    List<Long> findIdsByStatus(@Param("status") PostStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :id")
    int incrementViewCount(@Param("id") Long id);
}
