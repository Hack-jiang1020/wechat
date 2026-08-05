package com.blog.service;

import com.blog.common.BizException;
import com.blog.config.TokenService;
import com.blog.entity.AdminUser;
import com.blog.entity.AuthCode;
import com.blog.repository.AdminUserRepository;
import com.blog.repository.AuthCodeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 管理员账号与微信 openid 绑定 + 微信确认登录后台
 * 验证码持久化到 H2，服务重启不失效；每个验证码只能使用一次
 */
@Service
public class AdminWxService {

    private static final int EXPIRE_MINUTES = 10;

    private final AdminUserRepository adminUserRepository;
    private final AuthCodeRepository authCodeRepository;
    private final TokenService tokenService;

    public AdminWxService(AdminUserRepository adminUserRepository,
                          AuthCodeRepository authCodeRepository,
                          TokenService tokenService) {
        this.adminUserRepository = adminUserRepository;
        this.authCodeRepository = authCodeRepository;
        this.tokenService = tokenService;
    }

    /** 后台生成绑定码（需管理员已登录） */
    public String createBindCode(Long adminId) {
        String code = uniqueCode();
        AuthCode ac = new AuthCode();
        ac.setType("bind");
        ac.setCode(code);
        ac.setAdminId(adminId);
        ac.setUsed(0);
        ac.setExpireTime(LocalDateTime.now().plusMinutes(EXPIRE_MINUTES));
        ac.setCreateTime(LocalDateTime.now());
        authCodeRepository.save(ac);
        return code;
    }

    /** 小程序端输入绑定码，将当前微信 openid 绑定到管理员账号 */
    public AdminUser confirmBind(String code, String openid) {
        AuthCode ac = findValid("bind", code, "绑定码");
        AdminUser admin = adminUserRepository.findById(ac.getAdminId())
                .orElseThrow(() -> new BizException(404, "管理员不存在"));
        if (admin.getOpenid() != null && !admin.getOpenid().isEmpty()
                && !admin.getOpenid().equals(openid)) {
            throw new BizException("该管理员已绑定其他微信号，请先在后台解绑");
        }
        admin.setOpenid(openid);
        ac.setUsed(1);
        ac.setOpenid(openid);
        authCodeRepository.save(ac);
        return adminUserRepository.save(admin);
    }

    /** 后台登录页生成登录确认码 */
    public String createLoginCode() {
        String code = uniqueCode();
        AuthCode ac = new AuthCode();
        ac.setType("login");
        ac.setCode(code);
        ac.setUsed(0);
        ac.setExpireTime(LocalDateTime.now().plusMinutes(EXPIRE_MINUTES));
        ac.setCreateTime(LocalDateTime.now());
        authCodeRepository.save(ac);
        return code;
    }

    /** 小程序端确认登录：校验当前微信是否已绑定管理员 */
    public void confirmLogin(String code, String openid) {
        AuthCode ac = findValid("login", code, "确认码");
        AdminUser admin = adminUserRepository.findByOpenid(openid)
                .orElseThrow(() -> new BizException("该微信号未绑定管理员账号，请先在后台“绑定微信”"));
        ac.setAdminId(admin.getId());
        ac.setOpenid(openid);
        ac.setToken(tokenService.create(TokenService.ROLE_ADMIN, admin.getId()));
        ac.setUsed(1);
        authCodeRepository.save(ac);
    }

    /** 后台轮询确认状态，确认成功后返回管理员 token（一次性） */
    public Map<String, Object> checkLogin(String code) {
        Map<String, Object> data = new LinkedHashMap<>();
        AuthCode ac = authCodeRepository.findByTypeAndCode("login", code == null ? "" : code.trim())
                .orElse(null);
        if (ac == null || ac.getExpireTime().isBefore(LocalDateTime.now())) {
            data.put("status", "expired");
            return data;
        }
        if (ac.getUsed() == null || ac.getUsed() != 1 || ac.getToken() == null) {
            data.put("status", "pending");
            return data;
        }
        data.put("status", "confirmed");
        data.put("token", ac.getToken());
        ac.setToken(null); // token 一次性发放
        authCodeRepository.save(ac);
        return data;
    }


    /**
     * 统一验证码确认：自动识别「绑定码 / 登录确认码」，一个入口不再用错
     */
    public Map<String, Object> verify(String code, String openid) {
        AuthCode ac = authCodeRepository.findByCode(code == null ? "" : code.trim())
                .orElseThrow(() -> new BizException("验证码无效、已过期或已使用，请重新生成"));
        if (ac.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException("验证码已过期，请重新生成");
        }
        if (ac.getUsed() != null && ac.getUsed() == 1) {
            throw new BizException("验证码已被使用，请重新生成");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        if ("bind".equals(ac.getType())) {
            AdminUser admin = adminUserRepository.findById(ac.getAdminId())
                    .orElseThrow(() -> new BizException(404, "管理员不存在"));
            if (admin.getOpenid() != null && !admin.getOpenid().isEmpty()
                    && !admin.getOpenid().equals(openid)) {
                throw new BizException("该管理员已绑定其他微信号，请先在后台解绑");
            }
            admin.setOpenid(openid);
            adminUserRepository.save(admin);
            data.put("kind", "bind");
            data.put("message", "绑定成功，此微信号已是管理员");
        } else if ("login".equals(ac.getType())) {
            AdminUser admin = adminUserRepository.findByOpenid(openid)
                    .orElseThrow(() -> new BizException("该微信号未绑定管理员账号，请先在后台“绑定微信”"));
            ac.setAdminId(admin.getId());
            ac.setToken(tokenService.create(TokenService.ROLE_ADMIN, admin.getId()));
            data.put("kind", "login");
            data.put("message", "已确认，请回到电脑端管理后台");
        } else {
            throw new BizException("验证码类型异常，请重新生成");
        }
        ac.setOpenid(openid);
        ac.setUsed(1);
        authCodeRepository.save(ac);
        return data;
    }

    /** 解绑当前管理员绑定的微信号 */
    public void unbind(Long adminId) {
        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new BizException(404, "管理员不存在"));
        admin.setOpenid(null);
        adminUserRepository.save(admin);
    }
    private AuthCode findValid(String type, String code, String label) {
        AuthCode ac = authCodeRepository.findByTypeAndCode(type, code == null ? "" : code.trim())
                .orElseThrow(() -> new BizException(label + "无效、已过期或已使用，请重新生成"));
        if (ac.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException(label + "已过期，请重新生成");
        }
        if (ac.getUsed() != null && ac.getUsed() == 1) {
            throw new BizException(label + "已被使用，请重新生成");
        }
        return ac;
    }

    private String uniqueCode() {
        for (int i = 0; i < 20; i++) {
            String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
            if (!authCodeRepository.findByTypeAndCode("bind", code).isPresent()
                    && !authCodeRepository.findByTypeAndCode("login", code).isPresent()) {
                return code;
            }
        }
        throw new BizException("验证码生成失败，请重试");
    }
}