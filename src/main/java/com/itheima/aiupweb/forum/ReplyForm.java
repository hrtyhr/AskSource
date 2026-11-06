package com.itheima.aiupweb.forum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReplyForm {

    @NotBlank(message = "请输入回复内容")
    @Size(min = 2, max = 5000, message = "回复长度需为 2-5000 个字符")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
