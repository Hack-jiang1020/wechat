package com.blog.controller.mini;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.config.TokenService;
import com.blog.entity.Article;
import com.blog.service.ArticleService;
import com.blog.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/article")
public class MiniArticleController {

    private final ArticleService articleService;
    private final UserService userService;
    private final TokenService tokenService;

    public MiniArticleController(ArticleService articleService, UserService userService, TokenService tokenService) {
        this.articleService = articleService;
        this.userService = userService;
        this.tokenService = tokenService;
    }

    @GetMapping("/list")
    public Result<PageResult<Article>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long categoryId) {
        return Result.ok(articleService.pageList(page, size, keyword, categoryId, null, true));
    }

    @GetMapping("/hot")
    public Result<List<Article>> hot() {
        return Result.ok(articleService.hotList());
    }

    @GetMapping("/{id}")
    public Result<Article> detail(@PathVariable Long id,
                                  @RequestHeader(value = "token", required = false) String token) {
        Article article = articleService.getPublished(id);
        articleService.increaseViews(id);
        article.setViews((article.getViews() == null ? 0 : article.getViews()) + 1);
        if (token != null) {
            try {
                TokenService.TokenInfo info = tokenService.verifyRole(token, TokenService.ROLE_USER);
                userService.addBrowseRecord(info.getUserId(), article.getId(), article.getTitle(), article.getCover());
            } catch (Exception ignored) {
                // 浏览记录可选，token 失效不影响阅读
            }
        }
        return Result.ok(article);
    }
}
