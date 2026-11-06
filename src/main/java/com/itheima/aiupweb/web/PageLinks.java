package com.itheima.aiupweb.web;

import org.springframework.data.domain.Page;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PageLinks {

    private final int currentPage;
    private final int totalPages;
    private final String previousUrl;
    private final String nextUrl;
    private final List<Link> links;

    public PageLinks(Page<?> page, String basePath, Map<String, String> params) {
        this.currentPage = page.getNumber();
        this.totalPages = page.getTotalPages();
        this.previousUrl = currentPage > 0 ? url(basePath, params, currentPage - 1) : null;
        this.nextUrl = currentPage + 1 < totalPages ? url(basePath, params, currentPage + 1) : null;

        int start = Math.max(0, currentPage - 2);
        int end = Math.min(totalPages, start + 5);
        start = Math.max(0, end - 5);
        List<Link> pageLinks = new ArrayList<>();
        for (int i = start; i < end; i++) {
            pageLinks.add(new Link(i, url(basePath, params, i), i == currentPage));
        }
        this.links = pageLinks;
    }

    private String url(String basePath, Map<String, String> params, int page) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(basePath);
        params.forEach((key, value) -> {
            if (StringUtils.hasText(value)) {
                builder.queryParam(key, value);
            }
        });
        builder.queryParam("page", page);
        return builder.build().encode().toUriString();
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public String getPreviousUrl() {
        return previousUrl;
    }

    public String getNextUrl() {
        return nextUrl;
    }

    public List<Link> getLinks() {
        return links;
    }

    public static class Link {

        private final int number;
        private final String url;
        private final boolean current;

        public Link(int number, String url, boolean current) {
            this.number = number;
            this.url = url;
            this.current = current;
        }

        public int getNumber() {
            return number;
        }

        public String getUrl() {
            return url;
        }

        public boolean isCurrent() {
            return current;
        }
    }
}
