package com.blog.controller.mini;

import com.blog.common.Result;
import com.blog.config.TokenService;
import com.blog.entity.Comment;
import com.blog.service.CommentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comment")
public class MiniCommentController {

    private final CommentService commentService;
    private final TokenService tokenService;

    public MiniCommentController(CommentService commentService, TokenService tokenService) {
        this.commentService = commentService;
        this.tokenService = tokenService;
    }

    @GetMapping("/list")
    public Result<List<Comment>> list(@RequestParam Long articleId) {
        return Result.ok(commentService.listByArticle(articleId));
    }

    @PostMapping("/add")
    public Result<Comment> add(@RequestHeader("token") String token, @RequestBody Map<String, Object> body) {
        TokenService.TokenInfo info = tokenService.verifyRole(token, TokenService.ROLE_USER);
        Long articleId = body.get("articleId") == null ? null : Long.valueOf(body.get("articleId").toString());
        String content = body.get("content") == null ? null : body.get("content").toString();
        return Result.ok(commentService.add(info.getUserId(), articleId, content));
    }
}
