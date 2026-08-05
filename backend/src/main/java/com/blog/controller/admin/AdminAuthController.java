package com.blog.controller.admin;

import com.blog.common.BizException;
import com.blog.common.Result;
import com.blog.config.TokenService;
import com.blog.entity.AdminUser;
import com.blog.repository.AdminUserRepository;
import com.blog.service.AdminWxService;
import com.blog.service.WeChatService;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/api")
public class AdminAuthController {

    private final AdminUserRepository adminUserRepository;
    private final TokenService tokenService;
    private final AdminWxService adminWxService;
    private final WeChatService weChatService;

    public AdminAuthController(AdminUserRepository adminUserRepository,
                               TokenService tokenService,
                               AdminWxService adminWxService,
                               WeChatService weChatService) {
        this.adminUserRepository = adminUserRepository;
        this.tokenService = tokenService;
        this.adminWxService = adminWxService;
        this.weChatService = weChatService;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BizException("用户名和密码不能为空");
        }
        AdminUser admin = adminUserRepository.findByUsername(username.trim())
                .orElseThrow(() -> new BizException("用户名或密码错误"));
        String md5 = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
        if (!md5.equals(admin.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            throw new BizException("账号已被禁用，请联系系统管理员");
        }
        String token = tokenService.create(TokenService.ROLE_ADMIN, admin.getId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("username", admin.getUsername());
        data.put("nickname", admin.getNickname());
        data.put("avatar", admin.getAvatar());
        data.put("openid", admin.getOpenid());
        return Result.ok(data);
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> info(@RequestHeader("token") String token) {
        TokenService.TokenInfo info = tokenService.verifyRole(token, TokenService.ROLE_ADMIN);
        AdminUser admin = adminUserRepository.findById(info.getUserId())
                .orElseThrow(() -> new BizException(404, "管理员不存在"));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", admin.getId());
        data.put("username", admin.getUsername());
        data.put("nickname", admin.getNickname());
        data.put("avatar", admin.getAvatar());
        data.put("openid", admin.getOpenid());
        return Result.ok(data);
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("token") String token) {
        tokenService.remove(token);
        return Result.ok();
    }


    @PostMapping("/wx/bind")
    public Result<Map<String, Object>> createWxBind(@RequestHeader("token") String token) {
        TokenService.TokenInfo info = tokenService.verifyRole(token, TokenService.ROLE_ADMIN);
        String code = adminWxService.createBindCode(info.getUserId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", code);
        data.put("expireMinutes", 5);
        return Result.ok(data);
    }

    @PostMapping("/wx/login")
    public Result<Map<String, Object>> createWxLogin() {
        String code = adminWxService.createLoginCode();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", code);
        data.put("expireMinutes", 5);
        // 配置了微信凭据时生成小程序码，扫码直达小程序确认页；否则降级为验证码模式
        data.put("qrBase64", weChatService.generateMiniQr("code=" + code));
        return Result.ok(data);
    }

    @GetMapping("/wx/login/check")
    public Result<Map<String, Object>> checkWxLogin(@RequestParam("code") String code) {
        return Result.ok(adminWxService.checkLogin(code));
    }

    @PostMapping("/wx/unbind")
    public Result<Void> unbindWx(@RequestHeader("token") String token) {
        TokenService.TokenInfo info = tokenService.verifyRole(token, TokenService.ROLE_ADMIN);
        adminWxService.unbind(info.getUserId());
        return Result.ok();
    }
    @PostMapping("/password")
    public Result<Void> changePassword(@RequestHeader("token") String token, @RequestBody Map<String, String> body) {
        TokenService.TokenInfo info = tokenService.verifyRole(token, TokenService.ROLE_ADMIN);
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw new BizException("旧密码和新密码不能为空");
        }
        if (newPassword.length() < 6) {
            throw new BizException("新密码长度不能少于6位");
        }
        AdminUser admin = adminUserRepository.findById(info.getUserId())
                .orElseThrow(() -> new BizException(404, "管理员不存在"));
        String oldMd5 = DigestUtils.md5DigestAsHex(oldPassword.getBytes(StandardCharsets.UTF_8));
        if (!oldMd5.equals(admin.getPassword())) {
            throw new BizException("旧密码不正确");
        }
        admin.setPassword(DigestUtils.md5DigestAsHex(newPassword.getBytes(StandardCharsets.UTF_8)));
        admin.setUpdateTime(LocalDateTime.now());
        adminUserRepository.save(admin);
        return Result.ok();
    }
}
