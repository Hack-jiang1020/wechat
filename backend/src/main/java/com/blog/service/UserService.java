package com.blog.service;

import com.blog.common.BizException;
import com.blog.common.PageResult;
import com.blog.entity.BrowseRecord;
import com.blog.entity.User;
import com.blog.repository.BrowseRecordRepository;
import com.blog.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BrowseRecordRepository browseRecordRepository;
    private final WeChatService weChatService;

    public UserService(UserRepository userRepository,
                          BrowseRecordRepository browseRecordRepository,
                          WeChatService weChatService) {
        this.userRepository = userRepository;
        this.browseRecordRepository = browseRecordRepository;
        this.weChatService = weChatService;
    }

    public User login(String code, String mockCode, String nickname, String avatar) {
        String openid;
        if (weChatService.isEnabled()) {
            // 正式环境：用 wx.login 的 code 向微信服务器换取真实 openid
            openid = weChatService.code2Session(code);
            if (openid == null) {
                throw new BizException("微信登录校验失败，请重试");
            }
        } else {
            // 开发环境兜底：优先使用客户端持久化的 mockCode（身份稳定），否则以 code 模拟
            String devKey = StringUtils.hasText(mockCode) ? mockCode.trim() : code;
            openid = StringUtils.hasText(devKey) ? "dev_" + devKey : "wx_" + System.currentTimeMillis();
        }
        User user = userRepository.findByOpenid(openid).orElseGet(() -> {
            User u = new User();
            u.setOpenid(openid);
            u.setStatus(1);
            u.setCreateTime(LocalDateTime.now());
            return u;
        });
        if (StringUtils.hasText(nickname)) {
            user.setNickname(nickname);
        } else if (user.getNickname() == null) {
            user.setNickname("微信用户" + (user.getId() == null ? "" : user.getId()));
        }
        if (StringUtils.hasText(avatar)) {
            user.setAvatar(avatar);
        }
        user.setLastLoginTime(LocalDateTime.now());
        return userRepository.save(user);
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "用户不存在"));
    }

    public User updateProfile(Long id, String nickname, String avatar) {
        User user = getById(id);
        if (StringUtils.hasText(nickname)) {
            user.setNickname(nickname.trim());
        }
        if (StringUtils.hasText(avatar)) {
            user.setAvatar(avatar);
        }
        return userRepository.save(user);
    }

    public void addBrowseRecord(Long userId, Long articleId, String articleTitle, String cover) {
        BrowseRecord record = browseRecordRepository.findByUserIdAndArticleId(userId, articleId).orElseGet(() -> {
            BrowseRecord r = new BrowseRecord();
            r.setUserId(userId);
            r.setArticleId(articleId);
            return r;
        });
        record.setArticleTitle(articleTitle);
        record.setCover(cover);
        record.setCreateTime(LocalDateTime.now());
        browseRecordRepository.save(record);
    }

    public List<BrowseRecord> listBrowseRecords(Long userId, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(50, Math.max(1, size));
        return browseRecordRepository.findByUserIdOrderByCreateTimeDesc(userId)
                .stream()
                .skip((long) (safePage - 1) * safeSize)
                .limit(safeSize)
                .collect(java.util.stream.Collectors.toList());
    }

    public long countBrowseRecords(Long userId) {
        return browseRecordRepository.countByUserId(userId);
    }

    @Transactional
    public void clearBrowseRecords(Long userId) {
        browseRecordRepository.deleteByUserId(userId);
    }

    public PageResult<User> pageList(int page, int size, String keyword) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(50, Math.max(1, size));
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim() + "%";
                ps.add(cb.or(cb.like(root.get("nickname"), like), cb.like(root.get("openid"), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<User> pageResult = userRepository.findAll(
                spec, PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createTime")));
        return new PageResult<>(pageResult.getTotalElements(), pageResult.getContent());
    }

    public void updateStatus(Long id, Integer status) {
        User user = getById(id);
        user.setStatus(status == null ? 1 : status);
        userRepository.save(user);
    }
}
