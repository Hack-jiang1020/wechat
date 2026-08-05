package com.blog.runner;

import com.blog.entity.AdminUser;
import com.blog.entity.Article;
import com.blog.entity.Category;
import com.blog.entity.Comment;
import com.blog.entity.DataDict;
import com.blog.entity.User;
import com.blog.repository.AdminUserRepository;
import com.blog.repository.ArticleRepository;
import com.blog.repository.CategoryRepository;
import com.blog.repository.CommentRepository;
import com.blog.repository.DataDictRepository;
import com.blog.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 启动时自动初始化 H2 内存数据库基础数据
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AdminUserRepository adminUserRepository;
    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final DataDictRepository dataDictRepository;

    @Value("${blog.admin.username:admin}")
    private String adminUsername;

    @Value("${blog.admin.password:admin123}")
    private String adminPassword;

    public DataInitializer(AdminUserRepository adminUserRepository,
                           CategoryRepository categoryRepository,
                           ArticleRepository articleRepository,
                           CommentRepository commentRepository,
                           UserRepository userRepository,
                           DataDictRepository dataDictRepository) {
        this.adminUserRepository = adminUserRepository;
        this.categoryRepository = categoryRepository;
        this.articleRepository = articleRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.dataDictRepository = dataDictRepository;
    }

    @Override
    public void run(String... args) {
        if (adminUserRepository.count() == 0) {
            AdminUser admin = new AdminUser();
            admin.setUsername(adminUsername);
            admin.setPassword(DigestUtils.md5DigestAsHex(adminPassword.getBytes(StandardCharsets.UTF_8)));
            admin.setNickname("超级管理员");
            admin.setStatus(1);
            admin.setCreateTime(LocalDateTime.now());
            adminUserRepository.save(admin);
            log.info("初始化管理员账号: {} / {}", adminUsername, adminPassword);
        }

        if (categoryRepository.count() == 0) {
            saveCategory("技术笔记", 1, "Java、Spring 等后端技术积累");
            saveCategory("生活随笔", 2, "日常记录与随想");
            saveCategory("学习心得", 3, "读书与学习总结");
            saveCategory("资源分享", 4, "好用工具与资源推荐");
        }

        if (userRepository.count() == 0) {
            User user = new User();
            user.setOpenid("wx_demo_user_001");
            user.setNickname("清风明月");
            user.setAvatar("");
            user.setStatus(1);
            user.setCreateTime(LocalDateTime.now().minusDays(3));
            user.setLastLoginTime(LocalDateTime.now());
            userRepository.save(user);
        }

        if (articleRepository.count() == 0) {
            Category tech = categoryRepository.findByDeletedOrderBySortAscIdAsc(0).get(0);
            Category life = categoryRepository.findByDeletedOrderBySortAscIdAsc(0).get(1);

            Article a1 = new Article();
            a1.setTitle("个人博客小程序系统架构解析");
            a1.setSummary("从 SpringBoot 到 H2，再到微信小程序端，一文看懂这套轻量博客系统是如何组织起来的。");
            a1.setContent(sampleContent1());
            a1.setCategoryId(tech.getId());
            a1.setStatus(1);
            a1.setIsTop(1);
            a1.setViews(328);
            a1.setLikes(18);
            a1.setDeleted(0);
            a1.setCreateTime(LocalDateTime.now().minusDays(7));
            a1.setUpdateTime(LocalDateTime.now().minusDays(7));
            articleRepository.save(a1);

            Article a2 = new Article();
            a2.setTitle("SpringBoot + H2 内存数据库实战");
            a2.setSummary("不需要安装数据库，一行配置即可拥有完整的 ORM 开发体验，适合个人项目快速落地。");
            a2.setContent(sampleContent2());
            a2.setCategoryId(tech.getId());
            a2.setStatus(1);
            a2.setIsTop(1);
            a2.setViews(265);
            a2.setLikes(12);
            a2.setDeleted(0);
            a2.setCreateTime(LocalDateTime.now().minusDays(5));
            a2.setUpdateTime(LocalDateTime.now().minusDays(5));
            articleRepository.save(a2);

            Article a3 = new Article();
            a3.setTitle("把笔记变成一篇好文章");
            a3.setSummary("如何整理零散的笔记，让它们成为一篇结构清晰、值得分享的博客文章。");
            a3.setContent(sampleContent3());
            a3.setCategoryId(life.getId());
            a3.setStatus(1);
            a3.setIsTop(0);
            a3.setViews(186);
            a3.setLikes(9);
            a3.setDeleted(0);
            a3.setCreateTime(LocalDateTime.now().minusDays(3));
            a3.setUpdateTime(LocalDateTime.now().minusDays(3));
            articleRepository.save(a3);

            Article a4 = new Article();
            a4.setTitle("一份待发布的草稿：微信小程序踩坑记录");
            a4.setSummary("（草稿状态，仅管理员可见）记录小程序开发中遇到的常见问题与解决办法。");
            a4.setContent("<h2>微信小程序常见问题</h2><p>1. 图片域名需要在后台配置合法域名。</p><p>2. rich-text 组件不支持部分样式。</p>");
            a4.setCategoryId(tech.getId());
            a4.setStatus(0);
            a4.setIsTop(0);
            a4.setViews(0);
            a4.setLikes(0);
            a4.setDeleted(0);
            a4.setCreateTime(LocalDateTime.now().minusDays(1));
            a4.setUpdateTime(LocalDateTime.now().minusDays(1));
            articleRepository.save(a4);

            if (commentRepository.count() == 0) {
                User user = userRepository.findByOpenid("wx_demo_user_001").orElse(null);
                if (user != null) {
                    Comment c1 = new Comment();
                    c1.setArticleId(a1.getId());
                    c1.setArticleTitle(a1.getTitle());
                    c1.setUserId(user.getId());
                    c1.setNickname(user.getNickname());
                    c1.setContent("架构很清晰，H2 内存数据库确实适合个人博客，学习了！");
                    c1.setStatus(1);
                    c1.setReplyContent("感谢支持，欢迎常来交流~");
                    c1.setReplyTime(LocalDateTime.now().minusDays(6));
                    c1.setDeleted(0);
                    c1.setCreateTime(LocalDateTime.now().minusDays(6));
                    commentRepository.save(c1);

                    Comment c2 = new Comment();
                    c2.setArticleId(a2.getId());
                    c2.setArticleTitle(a2.getTitle());
                    c2.setUserId(user.getId());
                    c2.setNickname(user.getNickname());
                    c2.setContent("请问 H2 的数据重启后会丢失吗？如何改成文件模式持久化？");
                    c2.setStatus(1);
                    c2.setReplyContent("当前是内存模式，重启会重置；把连接地址改成 jdbc:h2:file:./data/blog 即可持久化。");
                    c2.setReplyTime(LocalDateTime.now().minusDays(4));
                    c2.setDeleted(0);
                    c2.setCreateTime(LocalDateTime.now().minusDays(4));
                    commentRepository.save(c2);

                    Comment c3 = new Comment();
                    c3.setArticleId(a3.getId());
                    c3.setArticleTitle(a3.getTitle());
                    c3.setUserId(user.getId());
                    c3.setNickname(user.getNickname());
                    c3.setContent("写得很真诚，期待更多生活记录！");
                    c3.setStatus(0);
                    c3.setDeleted(0);
                    c3.setCreateTime(LocalDateTime.now().minusHours(2));
                    commentRepository.save(c3);
                }
            }
        }

        if (dataDictRepository.count() == 0) {
            saveDict("article_status", "草稿", "0", 1, "文章状态-草稿");
            saveDict("article_status", "已发布", "1", 2, "文章状态-发布");
            saveDict("comment_status", "待审核", "0", 1, "留言状态-待审核");
            saveDict("comment_status", "已通过", "1", 2, "留言状态-通过");
            saveDict("comment_status", "已拒绝", "2", 3, "留言状态-拒绝");
            saveDict("is_top", "普通", "0", 1, "是否置顶-否");
            saveDict("is_top", "置顶", "1", 2, "是否置顶-是");
        }

        log.info("H2 数据库基础数据初始化完成");
    }

    private void saveCategory(String name, int sort, String remark) {
        Category category = new Category();
        category.setName(name);
        category.setSort(sort);
        category.setStatus(1);
        category.setRemark(remark);
        category.setDeleted(0);
        category.setCreateTime(LocalDateTime.now());
        categoryRepository.save(category);
    }

    private void saveDict(String type, String label, String value, int sort, String remark) {
        DataDict dict = new DataDict();
        dict.setDictType(type);
        dict.setDictLabel(label);
        dict.setDictValue(value);
        dict.setSort(sort);
        dict.setStatus(1);
        dict.setRemark(remark);
        dict.setCreateTime(LocalDateTime.now());
        dataDictRepository.save(dict);
    }

    private String sampleContent1() {
        return "<h2>一、整体架构</h2>"
                + "<p>本系统采用 <strong>B/S 三层结构</strong>，后端基于 SpringBoot 2.x，数据存储使用轻量的 H2 内存数据库，"
                + "前端为微信小程序原生开发，整体部署简单、运行稳定。</p>"
                + "<ul><li>表现层：微信小程序 + 管理后台页面</li>"
                + "<li>业务层：文章、分类、留言、用户等业务服务</li>"
                + "<li>数据层：JPA + H2 内存数据库</li></ul>"
                + "<h2>二、核心模块</h2>"
                + "<p>系统包含<em>文章管理、分类管理、留言审核、数据字典、系统日志、文件上传</em>等核心能力，"
                + "管理员与普通用户权限分离。</p>"
                + "<blockquote>小提示：后端以 jar 包一键运行，默认端口 8080。</blockquote>";
    }

    private String sampleContent2() {
        return "<h2>为什么选择 H2</h2>"
                + "<p>H2 是 Java 生态中非常优秀的嵌入式数据库，无需单独安装服务，项目启动即用，"
                + "非常适合个人博客这类轻量化系统。</p>"
                + "<pre><code class=\"language-java\">spring.datasource.url=jdbc:h2:mem:blogdb;MODE=MySQL;DB_CLOSE_DELAY=-1</code></pre>"
                + "<h2>启动步骤</h2>"
                + "<ol><li>执行 <code>mvn clean package</code> 编译打包</li>"
                + "<li>运行 <code>java -jar target/blog-miniapp.jar</code></li>"
                + "<li>访问 <code>http://localhost:8080/admin</code> 进入后台</li></ol>";
    }

    private String sampleContent3() {
        return "<h2>从碎片到结构</h2>"
                + "<p>好的文章往往不是一气呵成，而是从<strong>零散笔记</strong>开始。先记录，再整理，最后成文。</p>"
                + "<h3>整理步骤</h3>"
                + "<ul><li>先定主题，明确这篇文章要解决什么问题</li>"
                + "<li>把相关笔记按逻辑顺序排列</li>"
                + "<li>补充例子和配图，让内容更易读</li>"
                + "<li>反复删减，留下真正有价值的部分</li></ul>"
                + "<hr/>"
                + "<p>写作本身就是一种思考，把笔记变成文章，也是把知识变成自己的过程。</p>";
    }
}
