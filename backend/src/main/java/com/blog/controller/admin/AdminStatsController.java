package com.blog.controller.admin;

import com.blog.common.Result;
import com.blog.entity.Article;
import com.blog.repository.ArticleRepository;
import com.blog.repository.CategoryRepository;
import com.blog.repository.CommentRepository;
import com.blog.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/stats")
public class AdminStatsController {

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public AdminStatsController(ArticleRepository articleRepository,
                                CategoryRepository categoryRepository,
                                CommentRepository commentRepository,
                                UserRepository userRepository) {
        this.articleRepository = articleRepository;
        this.categoryRepository = categoryRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("articleTotal", articleRepository.countByDeleted(0));
        data.put("articlePublished", articleRepository.countByDeletedAndStatus(0, 1));
        data.put("categoryTotal", categoryRepository.countByDeleted(0));
        data.put("userTotal", userRepository.count());
        data.put("commentTotal", commentRepository.countByDeleted(0));
        data.put("commentPending", commentRepository.countByDeletedAndStatus(0, 0));
        List<Article> hot = articleRepository.findTop6ByDeletedAndStatusOrderByViewsDesc(0, 1);
        data.put("hotArticles", hot);
        return Result.ok(data);
    }
}
