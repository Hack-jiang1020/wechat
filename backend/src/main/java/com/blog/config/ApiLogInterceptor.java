package com.blog.config;

import com.blog.entity.SystemLog;
import com.blog.repository.SystemLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;

/**
 * 接口访问日志拦截器
 */
@Component
public class ApiLogInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiLogInterceptor.class);

    private final SystemLogRepository systemLogRepository;

    public ApiLogInterceptor(SystemLogRepository systemLogRepository) {
        this.systemLogRepository = systemLogRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("apiLogStart", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String uri = request.getRequestURI();
        if (uri.contains("/h2-console") || uri.startsWith("/uploads") || uri.contains(".")) {
            return;
        }
        try {
            Long start = (Long) request.getAttribute("apiLogStart");
            long cost = start == null ? 0 : System.currentTimeMillis() - start;
            SystemLog sysLog = new SystemLog();
            sysLog.setMethod(request.getMethod());
            sysLog.setPath(uri);
            sysLog.setParams(limit(request.getQueryString() == null ? "" : request.getQueryString(), 2000));
            sysLog.setIp(getIp(request));
            sysLog.setCostMs(cost);
            sysLog.setCreateTime(LocalDateTime.now());
            systemLogRepository.save(sysLog);
        } catch (Exception e) {
            log.warn("记录系统日志失败: {}", e.getMessage());
        }
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.trim().isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String limit(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() > max ? text.substring(0, max) : text;
    }
}
