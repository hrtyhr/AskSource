package com.itheima.aiupweb.forum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PostForm {

    @NotNull(message = "请选择分类")
    private Long categoryId;

    @NotBlank(message = "请输入标题")
    @Size(min = 5, max = 120, message = "标题长度需为 5-120 个字符")
    private String title;

    @NotBlank(message = "请输入正文")
    @Size(min = 10, max = 20000, message = "正文长度需为 10-20000 个字符")
    private String content;

    @Size(max = 200, message = "标签输入过长")
    private String tags;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
}
