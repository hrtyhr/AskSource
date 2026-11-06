package com.itheima.aiupweb.forum;

import com.itheima.aiupweb.common.TextUtils;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class PostSpecifications {

    private static final char LIKE_ESCAPE = '!';

    private PostSpecifications() {
    }

    public static Specification<Post> byCriteria(PostSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }
            if (StringUtils.hasText(criteria.categorySlug())) {
                predicates.add(cb.equal(root.get("category").get("slug"), criteria.categorySlug().trim()));
            }
            if (StringUtils.hasText(criteria.tagSlug())) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<Post> subRoot = subquery.from(Post.class);
                Join<Post, Tag> tagJoin = subRoot.join("tags", JoinType.INNER);
                subquery.select(subRoot.get("id"))
                        .where(cb.equal(subRoot.get("id"), root.get("id")),
                                cb.equal(tagJoin.get("slug"), criteria.tagSlug().trim()));
                predicates.add(cb.exists(subquery));
            }
            if (StringUtils.hasText(criteria.query())) {
                String pattern = "%" + TextUtils.escapeLike(criteria.query().trim().toLowerCase()) + "%";
                Join<Post, Tag> tagJoin = root.join("tags", JoinType.LEFT);
                Predicate title = cb.like(cb.lower(root.get("title")), pattern, LIKE_ESCAPE);
                Predicate content = cb.like(cb.lower(root.get("content")), pattern, LIKE_ESCAPE);
                Predicate tag = cb.like(cb.lower(tagJoin.get("name")), pattern, LIKE_ESCAPE);
                predicates.add(cb.or(title, content, tag));
                query.distinct(true);
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<Post> byAuthor(String username) {
        return (root, query, cb) -> cb.equal(root.get("author").get("username"), username);
    }
}
