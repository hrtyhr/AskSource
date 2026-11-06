package com.itheima.aiupweb.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AiDocumentStateRepository extends JpaRepository<AiDocumentState, Long> {

    long countByStatus(IndexStatus status);

    @Query("""
            select s.postId from AiDocumentState s
            where s.status in :statuses and s.attempts < :maxAttempts
            """)
    List<Long> findRecoverablePostIds(@Param("statuses") Collection<IndexStatus> statuses,
                                      @Param("maxAttempts") int maxAttempts);

    void deleteByPostId(Long postId);
}
