package com.blog.service;

import com.blog.common.BizException;
import com.blog.common.PageResult;
import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.entity.User;
import com.blog.repository.ArticleRepository;
import com.blog.repository.CommentRepository;
import com.blog.repository.UserRepository;
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

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository,
                          ArticleRepository articleRepository,
                          UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
    }

    public Comment add(Long userId, Long articleId, String content) {
        if (articleId == null) {
            throw new BizException("文章参数缺失");
        }
        if (!StringUtils.hasText(content)) {
            throw new BizException("留言内容不能为空");
        }
        if (content.trim().length() > 1000) {
            throw new BizException("留言内容不能超过1000字");
        }
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new BizException(404, "文章不存在"));
        if (article.getDeleted() == 1 || article.getStatus() != 1) {
            throw new BizException(404, "文章不存在或未发布");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setArticleTitle(article.getTitle());
        comment.setUserId(userId);
        comment.setNickname(user.getNickname() == null ? "微信用户" : user.getNickname());
        comment.setAvatar(user.getAvatar());
        comment.setContent(content.trim());
        comment.setStatus(0);
        comment.setDeleted(0);
        comment.setCreateTime(LocalDateTime.now());
        return commentRepository.save(comment);
    }

    public List<Comment> listByArticle(Long articleId) {
        return commentRepository.findByArticleIdAndStatusAndDeletedOrderByCreateTimeDesc(articleId, 1, 0);
    }

    public List<Comment> listByUser(Long userId) {
        return commentRepository.findByUserIdAndDeletedOrderByCreateTimeDesc(userId, 0);
    }

    public PageResult<Comment> pageList(int page, int size, String keyword, Integer status, Long articleId) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(50, Math.max(1, size));
        Specification<Comment> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("deleted"), 0));
            if (status != null) {
                ps.add(cb.equal(root.get("status"), status));
            }
            if (articleId != null) {
                ps.add(cb.equal(root.get("articleId"), articleId));
            }
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim() + "%";
                ps.add(cb.or(cb.like(root.get("content"), like), cb.like(root.get("nickname"), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<Comment> pageResult = commentRepository.findAll(
                spec, PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createTime")));
        return new PageResult<>(pageResult.getTotalElements(), pageResult.getContent());
    }

    public void review(Long id, Integer status) {
        Comment comment = getById(id);
        comment.setStatus(status == null ? 1 : status);
        commentRepository.save(comment);
    }

    public void reply(Long id, String replyContent) {
        if (!StringUtils.hasText(replyContent)) {
            throw new BizException("回复内容不能为空");
        }
        Comment comment = getById(id);
        comment.setReplyContent(replyContent.trim());
        comment.setReplyTime(LocalDateTime.now());
        commentRepository.save(comment);
    }

    public void delete(Long id) {
        Comment comment = getById(id);
        comment.setDeleted(1);
        commentRepository.save(comment);
    }

    private Comment getById(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "留言不存在"));
    }
}
