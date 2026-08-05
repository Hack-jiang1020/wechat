package com.blog.service;

import com.blog.common.BizException;
import com.blog.entity.Category;
import com.blog.repository.ArticleRepository;
import com.blog.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;

    public CategoryService(CategoryRepository categoryRepository, ArticleRepository articleRepository) {
        this.categoryRepository = categoryRepository;
        this.articleRepository = articleRepository;
    }

    public List<Category> listAll() {
        return categoryRepository.findByDeletedOrderBySortAscIdAsc(0);
    }

    public List<Category> listEnabled() {
        return listAll().stream().filter(c -> c.getStatus() != null && c.getStatus() == 1).collect(Collectors.toList());
    }

    public List<Category> listAll(String keyword) {
        List<Category> list = listAll();
        if (!StringUtils.hasText(keyword)) {
            return list;
        }
        String k = keyword.trim().toLowerCase();
        return list.stream()
                .filter(c -> c.getName().toLowerCase().contains(k))
                .collect(Collectors.toList());
    }

    public Category save(Category form) {
        if (!StringUtils.hasText(form.getName())) {
            throw new BizException("分类名称不能为空");
        }
        if (form.getId() == null) {
            form.setDeleted(0);
            form.setSort(form.getSort() == null ? 0 : form.getSort());
            form.setStatus(form.getStatus() == null ? 1 : form.getStatus());
            form.setCreateTime(LocalDateTime.now());
            return categoryRepository.save(form);
        }
        Category exist = categoryRepository.findById(form.getId())
                .orElseThrow(() -> new BizException(404, "分类不存在"));
        exist.setName(form.getName());
        exist.setSort(form.getSort() == null ? 0 : form.getSort());
        exist.setStatus(form.getStatus() == null ? 1 : form.getStatus());
        exist.setRemark(form.getRemark());
        return categoryRepository.save(exist);
    }

    public void delete(Long id) {
        Category exist = categoryRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "分类不存在"));
        long count = articleRepository.countByDeletedAndCategoryId(0, id);
        if (count > 0) {
            throw new BizException("该分类下存在 " + count + " 篇文章，请先移动或删除文章");
        }
        exist.setDeleted(1);
        categoryRepository.save(exist);
    }
}
