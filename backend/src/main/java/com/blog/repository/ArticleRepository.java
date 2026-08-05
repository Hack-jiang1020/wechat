package com.blog.repository;

import com.blog.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {

    long countByDeleted(Integer deleted);

    long countByDeletedAndStatus(Integer deleted, Integer status);

    long countByDeletedAndCategoryId(Integer deleted, Long categoryId);

    List<Article> findTop6ByDeletedAndStatusOrderByViewsDesc(Integer deleted, Integer status);
}
