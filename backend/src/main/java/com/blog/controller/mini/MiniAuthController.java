package com.blog.controller.mini;

import com.blog.common.Result;
import com.blog.config.TokenService;
import com.blog.entity.User;
import com.blog.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 小程序用户登录（轻量化：以 wx.login 的 code 作为 openid 模拟，接入微信服务端后替换即可）
 */
@RestController
@RequestMapping("/api/user")
public class MiniAuthController {

    private final UserService userService;
    private final TokenService tokenService;

    public MiniAuthController(UserService userService, TokenService tokenService) {
        this.userService = userService;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        String mockCode = body.get("mockCode");
        String nickname = body.get("nickname");
        String avatar = body.get("avatar");
        User user = userService.login(code, mockCode, nickname, avatar);
        String token = tokenService.create(TokenService.ROLE_USER, user.getId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("user", user);
        return Result.ok(data);
    }
}
