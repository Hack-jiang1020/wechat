package com.blog.service;

import com.blog.common.BizException;
import com.blog.common.PageResult;
import com.blog.entity.Article;
import com.blog.entity.Category;
import com.blog.repository.ArticleRepository;
import com.blog.repository.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;

    public ArticleService(ArticleRepository articleRepository, CategoryRepository categoryRepository) {
        this.articleRepository = articleRepository;
        this.categoryRepository = categoryRepository;
    }

    public PageResult<Article> pageList(int page, int size, String keyword, Long categoryId, Integer status, boolean publishedOnly) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(50, Math.max(1, size));
        Specification<Article> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("deleted"), 0));
            if (publishedOnly) {
                ps.add(cb.equal(root.get("status"), 1));
            } else if (status != null) {
                ps.add(cb.equal(root.get("status"), status));
            }
            if (categoryId != null) {
                ps.add(cb.equal(root.get("categoryId"), categoryId));
            }
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim() + "%";
                ps.add(cb.or(cb.like(root.get("title"), like), cb.like(root.get("summary"), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Sort sort = Sort.by(Sort.Direction.DESC, "isTop").and(Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Article> pageResult = articleRepository.findAll(spec, PageRequest.of(safePage - 1, safeSize, sort));
        fillCategoryNames(pageResult.getContent());
        return new PageResult<>(pageResult.getTotalElements(), pageResult.getContent());
    }

    public List<Article> hotList() {
        List<Article> list = articleRepository.findTop6ByDeletedAndStatusOrderByViewsDesc(0, 1);
        fillCategoryNames(list);
        return list;
    }

    public Article getById(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "文章不存在"));
        if (article.getDeleted() != null && article.getDeleted() == 1) {
            throw new BizException(404, "文章不存在");
        }
        fillCategoryNames(java.util.Collections.singletonList(article));
        return article;
    }

    public Article getPublished(Long id) {
        Article article = getById(id);
        if (article.getStatus() == null || article.getStatus() != 1) {
            throw new BizException(404, "文章不存在或未发布");
        }
        return article;
    }

    public void increaseViews(Long id) {
        Article article = articleRepository.findById(id).orElse(null);
        if (article != null && article.getDeleted() == 0) {
            article.setViews((article.getViews() == null ? 0 : article.getViews()) + 1);
            articleRepository.save(article);
        }
    }

    public Article save(Article form) {
        if (!StringUtils.hasText(form.getTitle())) {
            throw new BizException("文章标题不能为空");
        }
        if (form.getCategoryId() == null) {
            throw new BizException("请选择文章分类");
        }
        LocalDateTime now = LocalDateTime.now();
        if (form.getId() == null) {
            form.setId(null);
            form.setDeleted(0);
            form.setViews(form.getViews() == null ? 0 : form.getViews());
            form.setLikes(form.getLikes() == null ? 0 : form.getLikes());
            form.setIsTop(form.getIsTop() == null ? 0 : form.getIsTop());
            form.setStatus(form.getStatus() == null ? 1 : form.getStatus());
            form.setCreateTime(now);
            form.setUpdateTime(now);
            return articleRepository.save(form);
        }
        Article exist = getById(form.getId());
        exist.setTitle(form.getTitle());
        exist.setSummary(form.getSummary());
        exist.setContent(form.getContent());
        exist.setCover(form.getCover());
        exist.setCategoryId(form.getCategoryId());
        exist.setStatus(form.getStatus() == null ? 1 : form.getStatus());
        exist.setIsTop(form.getIsTop() == null ? 0 : form.getIsTop());
        exist.setUpdateTime(now);
        return articleRepository.save(exist);
    }

    public void delete(Long id) {
        Article article = getById(id);
        article.setDeleted(1);
        article.setUpdateTime(LocalDateTime.now());
        articleRepository.save(article);
    }

    public void updateStatus(Long id, Integer status) {
        Article article = getById(id);
        article.setStatus(status == null ? 1 : status);
        article.setUpdateTime(LocalDateTime.now());
        articleRepository.save(article);
    }

    public void updateTop(Long id, Integer isTop) {
        Article article = getById(id);
        article.setIsTop(isTop == null ? 0 : isTop);
        article.setUpdateTime(LocalDateTime.now());
        articleRepository.save(article);
    }

    private void fillCategoryNames(List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            return;
        }
        List<Category> categories = categoryRepository.findByDeletedOrderBySortAscIdAsc(0);
        Map<Long, String> map = categories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
        for (Article article : articles) {
            if (article.getCategoryId() != null) {
                article.setCategoryName(map.get(article.getCategoryId()));
            }
        }
    }
}
