package com.itheima.aiupweb.forum;

import com.itheima.aiupweb.common.BusinessException;
import com.itheima.aiupweb.common.SlugUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class TagService {

    private static final int MAX_TAGS = 5;
    private static final int MAX_TAG_LENGTH = 20;

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional
    public Set<Tag> resolveTags(String raw) {
        List<String> names = parseTagNames(raw);
        Map<String, Tag> resolved = new LinkedHashMap<>();
        for (String name : names) {
            String slug = SlugUtils.slugify(name);
            if (slug.isBlank()) {
                continue;
            }
            Tag tag = tagRepository.findBySlug(slug)
                    .or(() -> tagRepository.findByNameIgnoreCase(name))
                    .orElseGet(() -> tagRepository.save(new Tag(name, slug)));
            resolved.put(slug, tag);
        }
        return new LinkedHashSet<>(resolved.values());
    }

    public List<String> parseTagNames(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        String[] parts = raw.split("[,，]");
        LinkedHashMap<String, String> unique = new LinkedHashMap<>();
        for (String part : parts) {
            String name = part.trim().replaceAll("\\s+", " ");
            if (name.isBlank()) {
                continue;
            }
            if (name.length() > MAX_TAG_LENGTH) {
                throw new BusinessException("单个标签不能超过 " + MAX_TAG_LENGTH + " 个字符");
            }
            unique.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
        }
        if (unique.size() > MAX_TAGS) {
            throw new BusinessException("最多添加 " + MAX_TAGS + " 个标签");
        }
        return new ArrayList<>(unique.values());
    }

    public Set<String> tagNamesOf(Post post) {
        Set<String> names = new LinkedHashSet<>();
        post.getTags().forEach(tag -> names.add(tag.getName()));
        return names;
    }
}
