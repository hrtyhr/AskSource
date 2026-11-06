package com.itheima.aiupweb.forum;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CategoryForm {

    @NotBlank(message = "请输入分类名称")
    @Size(min = 2, max = 50, message = "分类名称长度需为 2-50 个字符")
    private String name;

    @Pattern(regexp = "^[\\p{L}\\p{N}-]*$", message = "slug 只能包含字母、数字、中文和短横线")
    @Size(max = 80, message = "slug 过长")
    private String slug;

    @Size(max = 255, message = "简介过长")
    private String description;

    @Min(value = 0, message = "排序值不能为负数")
    @Max(value = 9999, message = "排序值过大")
    private int sortOrder;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
