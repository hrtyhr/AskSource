package com.itheima.aiupweb.ai;

public class RetrievedSource {

    private Long postId;
    private String sourceType;
    private Long sourceId;
    private String title;
    private String snippet;
    private String url;
    private double score;

    public RetrievedSource() {
    }

    public RetrievedSource(Long postId, String sourceType, Long sourceId, String title,
                           String snippet, String url, double score) {
        this.postId = postId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.title = title;
        this.snippet = snippet;
        this.url = url;
        this.score = score;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
