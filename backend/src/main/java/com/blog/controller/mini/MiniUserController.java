package com.blog.controller.mini;

import com.blog.common.Result;
import com.blog.config.TokenService;
import com.blog.entity.BrowseRecord;
import com.blog.entity.Comment;
import com.blog.entity.User;
import com.blog.service.AdminWxService;
import com.blog.service.CommentService;
import com.blog.service.UploadService;
import com.blog.service.UserService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class MiniUserController {

    private final UserService userService;
    private final CommentService commentService;
    private final TokenService tokenService;
    private final UploadService uploadService;
    private final AdminWxService adminWxService;

    public MiniUserController(UserService userService,
                              CommentService commentService,
                              TokenService tokenService,
                              UploadService uploadService,
                              AdminWxService adminWxService) {
        this.userService = userService;
        this.commentService = commentService;
        this.tokenService = tokenService;
        this.uploadService = uploadService;
        this.adminWxService = adminWxService;
    }

    @GetMapping("/info")
    public Result<User> info(@RequestHeader("token") String token) {
        TokenService.TokenInfo info = tokenService.verifyRole(token, TokenService.ROLE_USER);
        return Result.ok(userService.getById(info.getUserId()));
    }

    @PostMapping("/info")
    public Result<User> updateInfo(@RequestHeader("token") String token, @RequestBody Map<String, String> body) {
        TokenService.TokenInfo info = tokenService.verifyRole(token, TokenService.ROLE_USER);
        return Result.ok(userService.updateProfile(info.getUserId(), body.get("nickname"), body.get("avatar")));
    }

    @GetMapping("/history")
    public Result<Map<String, Object>> history(@RequestHeader("token") String token,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        TokenService.TokenInfo info = tokenService.verifyRole(token, TokenService.ROLE_USER);
        List<BrowseRecord> list = userService.listBrowseRecords(info.getUserId(), page, size);
        long total = userService.countBrowseRecords(info.getUserId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", total);
        data.put("list", list);
        return Result.ok(data);
    }

    @DeleteMapping("/history")
    public Result<Void> clearHistory(@RequestHeader("token") String token) {
        TokenService.TokenInfo info = tokenService.verifyRole(token, TokenService.ROLE_USER);
        userService.clearBrowseRecords(info.getUserId());
        return Result.ok();
    }



    @PostMapping("/admin/bind")
    public Result<Void> bindAdmin(@RequestHeader("token") String token, @RequestBody Map<String, String> body) {
        TokenService.TokenInfo info = tokenService.verifyRole(token, TokenService.ROLE_USER);
        User user = userService.getById(info.getUserId());
        adminWxService.confirmBind(body.get("code"), user.getOpenid());
        return Result.ok();
    }

    @PostMapping("/admin/confirm")
    public Result<Void> confirmAdminLogin(@RequestHeader("token") String token, @RequestBody Map<String, String> body) {
        TokenService.TokenInfo info = tokenService.verifyRole(token, TokenService.ROLE_USER);
        User user = userService.getById(info.getUserId());
        adminWxService.confirmLogin(body.get("code"), user.getOpenid());
        return Result.ok();
    }

    @PostMapping("/admin/verify")
    public Result<Map<String, Object>> verifyAdminCode(@RequestHeader("token") String token,
                                                       @RequestBody Map<String, String> body) {
        TokenService.TokenInfo info = tokenService.verifyRole(token, TokenService.ROLE_USER);
        User user = userService.getById(info.getUserId());
        return Result.ok(adminWxService.verify(body.get("code"), user.getOpenid()));
    }
    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestHeader("token") String token,
                                                    @RequestParam("file") MultipartFile file) {
        tokenService.verifyRole(token, TokenService.ROLE_USER);
        String url = uploadService.upload(file);
        Map<String, String> data = new LinkedHashMap<>();
        data.put("url", url);
        return Result.ok(data);
    }
    @GetMapping("/comments")
    public Result<Map<String, Object>> comments(@RequestHeader("token") String token,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        TokenService.TokenInfo info = tokenService.verifyRole(token, TokenService.ROLE_USER);
        List<Comment> all = commentService.listByUser(info.getUserId());
        int safePage = Math.max(1, page);
        int safeSize = Math.min(50, Math.max(1, size));
        List<Comment> list = all.stream()
                .skip((long) (safePage - 1) * safeSize)
                .limit(safeSize)
                .collect(java.util.stream.Collectors.toList());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", all.size());
        data.put("list", list);
        return Result.ok(data);
    }
}
