package com.itheima.aiupweb.forum;

import com.itheima.aiupweb.common.BusinessException;
import com.itheima.aiupweb.common.NotFoundException;
import com.itheima.aiupweb.common.SlugUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;

    public CategoryService(CategoryRepository categoryRepository, PostRepository postRepository) {
        this.categoryRepository = categoryRepository;
        this.postRepository = postRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> listAll() {
        return categoryRepository.findAllByOrderBySortOrderAscNameAsc();
    }

    @Transactional(readOnly = true)
    public Category requireById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("分类不存在"));
    }

    @Transactional(readOnly = true)
    public Category requireBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("分类不存在"));
    }

    @Transactional
    public Category create(CategoryForm form) {
        String name = form.getName().trim();
        String slug = StringUtils.hasText(form.getSlug())
                ? SlugUtils.slugify(form.getSlug())
                : SlugUtils.slugify(name);
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException("分类名称已存在");
        }
        if (categoryRepository.existsBySlug(slug)) {
            throw new BusinessException("分类 slug 已存在");
        }
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(trimToNull(form.getDescription()));
        category.setSortOrder(form.getSortOrder());
        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(Long id, CategoryForm form) {
        Category category = requireById(id);
        String name = form.getName().trim();
        String slug = StringUtils.hasText(form.getSlug())
                ? SlugUtils.slugify(form.getSlug())
                : SlugUtils.slugify(name);
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException("分类名称已存在");
        }
        if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
            throw new BusinessException("分类 slug 已存在");
        }
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(trimToNull(form.getDescription()));
        category.setSortOrder(form.getSortOrder());
        return category;
    }

    @Transactional
    public void delete(Long id) {
        Category category = requireById(id);
        if (postRepository.countByCategoryId(id) > 0) {
            throw new BusinessException("该分类下仍有帖子，无法删除");
        }
        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public long count() {
        return categoryRepository.count();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
