package com.blog.repository;

import com.blog.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long>, JpaSpecificationExecutor<Comment> {

    List<Comment> findByArticleIdAndStatusAndDeletedOrderByCreateTimeDesc(Long articleId, Integer status, Integer deleted);

    List<Comment> findByUserIdAndDeletedOrderByCreateTimeDesc(Long userId, Integer deleted);

    long countByDeleted(Integer deleted);

    long countByDeletedAndStatus(Integer deleted, Integer status);
}
