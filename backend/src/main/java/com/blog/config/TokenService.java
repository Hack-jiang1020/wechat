package com.blog.config;

import com.blog.common.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量 Token 认证服务（内存实现，重启即失效，符合轻量化个人系统定位）
 */
@Service
public class TokenService {

    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_USER = "user";

    @Value("${blog.token-expire-hours:168}")
    private long expireHours;

    private final Map<String, TokenInfo> tokenMap = new ConcurrentHashMap<>();

    public String create(String role, Long userId) {
        String token = role + ":" + UUID.randomUUID().toString().replace("-", "");
        tokenMap.put(token, new TokenInfo(role, userId, LocalDateTime.now()));
        return token;
    }

    public TokenInfo verify(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new BizException(401, "未登录或登录已过期");
        }
        TokenInfo info = tokenMap.get(token.trim());
        if (info == null) {
            throw new BizException(401, "未登录或登录已过期");
        }
        if (info.getCreateTime().plusHours(expireHours).isBefore(LocalDateTime.now())) {
            tokenMap.remove(token);
            throw new BizException(401, "登录已过期，请重新登录");
        }
        return info;
    }

    public TokenInfo verifyRole(String token, String role) {
        TokenInfo info = verify(token);
        if (!role.equals(info.getRole())) {
            throw new BizException(403, "无访问权限");
        }
        return info;
    }

    public void remove(String token) {
        if (token != null) {
            tokenMap.remove(token.trim());
        }
    }

    public static class TokenInfo {
        private final String role;
        private final Long userId;
        private final LocalDateTime createTime;

        public TokenInfo(String role, Long userId, LocalDateTime createTime) {
            this.role = role;
            this.userId = userId;
            this.createTime = createTime;
        }

        public String getRole() {
            return role;
        }

        public Long getUserId() {
            return userId;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }
    }
}
