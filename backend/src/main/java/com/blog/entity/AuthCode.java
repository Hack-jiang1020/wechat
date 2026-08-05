package com.blog.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 管理员微信绑定/登录验证码（持久化，重启不失效）
 */
@Entity
@Table(name = "t_auth_code")
public class AuthCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** bind=绑定码  login=登录确认码 */
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String code;

    /** bind: 目标管理员ID；login: 确认后回填 */
    private Long adminId;

    /** 确认人的 openid */
    private String openid;

    /** login: 确认后发放的管理员 token（一次性） */
    @Column(length = 200)
    private String token;

    /** 0 未使用 1 已使用 */
    private Integer used = 0;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }
    public String getOpenid() { return openid; }
    public void setOpenid(String openid) { this.openid = openid; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Integer getUsed() { return used; }
    public void setUsed(Integer used) { this.used = used; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}