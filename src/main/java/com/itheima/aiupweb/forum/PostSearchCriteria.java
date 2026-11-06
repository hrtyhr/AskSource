package com.itheima.aiupweb.forum;

public record PostSearchCriteria(String query, String categorySlug, String tagSlug, PostStatus status) {

    public static PostSearchCriteria published(String query, String categorySlug, String tagSlug) {
        return new PostSearchCriteria(query, categorySlug, tagSlug, PostStatus.PUBLISHED);
    }
}
