package com.blog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信小程序服务
 * 正式环境：通过环境变量 BLOG_WECHAT_APPID / BLOG_WECHAT_SECRET 注入后自动启用真实校验与小程序码生成
 * 开发环境：未设置环境变量时走本地兜底登录，方便无凭据联调
 */
@Service
public class WeChatService {

    private static final Logger log = LoggerFactory.getLogger(WeChatService.class);

    private static final String CODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code";

    @Value("${blog.wechat.appid:}")
    private String appid;

    @Value("${blog.wechat.secret:}")
    private String secret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String accessToken;
    private long accessTokenExpireAt = 0;

    public boolean isEnabled() {
        return StringUtils.hasText(appid) && StringUtils.hasText(secret);
    }

    /**
     * 用 wx.login 的 code 向微信服务器换取 openid。
     * 返回 null 表示未配置、参数缺失或换取失败。
     */
    public String code2Session(String code) {
        if (!isEnabled() || !StringUtils.hasText(code)) {
            return null;
        }
        try {
            String url = String.format(CODE2SESSION_URL, appid.trim(), secret.trim(), code.trim());
            // 微信接口可能返回 text/plain，先按字符串接收再手动解析 JSON
            String resp = restTemplate.getForObject(url, String.class);
            Map<?, ?> json = objectMapper.readValue(resp == null ? "{}" : resp, Map.class);
            Object errcode = json.get("errcode");
            if (errcode != null && !"0".equals(String.valueOf(errcode))) {
                log.warn("code2Session 失败: errcode={}, errmsg={}", errcode, json.get("errmsg"));
                return null;
            }
            Object openid = json.get("openid");
            return openid == null ? null : String.valueOf(openid);
        } catch (Exception e) {
            log.warn("调用微信 code2Session 异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 生成小程序码（扫码直达「管理员微信确认」页），失败返回 null
     */
    public String generateMiniQr(String scene) {
        String token = getAccessToken();
        if (token == null) {
            return null;
        }
        try {
            String url = "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=" + token;
            Map<String, Object> body = new HashMap<>();
            body.put("scene", scene);
            body.put("page", "pages/admin/admin");
            body.put("check_path", false);
            body.put("env_version", "release");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<byte[]> resp = restTemplate.exchange(url, HttpMethod.POST, entity, byte[].class);
            byte[] data = resp.getBody();
            if (data == null || data.length < 100) {
                return null;
            }
            String head = new String(data, 0, Math.min(data.length, 200), StandardCharsets.UTF_8);
            if (head.trim().startsWith("{")) {
                log.warn("getwxacodeunlimit 失败: {}", head);
                return null;
            }
            return Base64.getEncoder().encodeToString(data);
        } catch (Exception e) {
            log.warn("生成小程序码异常: {}", e.getMessage());
            return null;
        }
    }

    private String getAccessToken() {
        if (!isEnabled()) {
            return null;
        }
        if (accessToken != null && System.currentTimeMillis() < accessTokenExpireAt) {
            return accessToken;
        }
        try {
            String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                    + appid.trim() + "&secret=" + secret.trim();
            String resp = restTemplate.getForObject(url, String.class);
            Map<?, ?> json = objectMapper.readValue(resp == null ? "{}" : resp, Map.class);
            Object token = json.get("access_token");
            if (token == null) {
                log.warn("获取 access_token 失败: {}", resp == null ? "null" : resp);
                return null;
            }
            accessToken = String.valueOf(token);
            accessTokenExpireAt = System.currentTimeMillis() + 100 * 60 * 1000L;
            return accessToken;
        } catch (Exception e) {
            log.warn("获取 access_token 异常: {}", e.getMessage());
            return null;
        }
    }
}