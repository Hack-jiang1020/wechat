# 个人博客微信小程序系统

基于 **SpringBoot + H2 内存数据库 + 微信小程序原生开发** 的轻量化个人笔记博客系统。无需安装任何数据库，打包后一个 jar 即可运行，开箱即用。

## 一、项目结构

```
个人博客微信小程序系统/
├── backend/                  # SpringBoot 后端（含管理后台页面）
│   ├── pom.xml               # Maven 工程配置
│   └── src/main/
│       ├── java/com/blog/
│       │   ├── config/       # 鉴权拦截器、Token 服务、日志拦截、Web 配置
│       │   ├── controller/   # 管理端接口 + 小程序端接口
│       │   ├── entity/       # 用户、文章、分类、留言、浏览记录、字典、日志实体
│       │   ├── repository/   # Spring Data JPA 数据访问层
│       │   ├── runner/       # H2 数据库自动建表 + 初始化基础数据
│       │   ├── service/      # 业务逻辑层
│       │   └── common/       # 统一响应、异常处理
│       └── resources/
│           ├── application.yml   # 应用配置（端口、H2、上传目录）
│           └── static/admin/     # 管理后台页面（纯原生 HTML/CSS/JS）
├── share-wxmal/              # 微信小程序端（原生开发）
│   ├── pages/index/          # 首页：搜索、分类筛选、文章列表、下拉刷新
│   ├── pages/detail/         # 详情页：富文本渲染、留言互动
│   ├── pages/profile/        # 个人中心：浏览记录、我的留言、编辑资料
│   ├── pages/login/          # 微信一键登录
│   ├── utils/                # 请求封装、Markdown 转换、时间工具
│   └── config.js             # 后端接口地址配置
├── maven-settings.xml        # 阿里云 Maven 镜像（加速国内依赖下载）
└── 启动后端.bat              # Windows 一键启动脚本
```

## 二、功能清单

### 管理员（后台 http://localhost:8080/admin）

| 模块 | 功能 |
| --- | --- |
| 仪表盘 | 文章/分类/用户/留言统计、热门文章排行 |
| 文章管理 | 新增、编辑、上下架、置顶、逻辑删除、条件检索、封面与内容图片上传、HTML 富文本编辑 |
| 分类管理 | 分类新增、编辑、排序、启停、删除（有文章时禁止删除） |
| 留言管理 | 留言审核（通过/拒绝）、管理员回复、删除、条件检索 |
| 用户管理 | 小程序用户列表、启用/禁用 |
| 数据字典 | 文章状态、留言状态、置顶标记等字典配置 |
| 系统日志 | 接口访问日志自动记录、一键清空 |
| 账号安全 | 修改后台登录密码 |

### 小程序用户

| 模块 | 功能 |
| --- | --- |
| 访客模式 | 默认访客浏览，无需登录即可阅读全部文章；留言互动与个人中心按需微信授权登录（`wx.login` 换 openid，头像昵称使用官方填写能力） |
| 文章浏览 | 首页文章卡片、分类筛选、关键词搜索、下拉刷新、分页加载、置顶展示 |
| 富文本阅读 | HTML 富文本 + Markdown 双格式渲染、代码块/引用/表格样式适配 |
| 留言互动 | 文章下留言、查看管理员回复、状态跟踪（待审核/通过/拒绝） |
| 个人中心 | 浏览记录、我的留言、编辑昵称、清空浏览记录 |

## 三、技术栈

- 后端：Java 8、SpringBoot 2.7.18、Spring Data JPA、H2 内存数据库、Maven
- 管理后台：原生 HTML/CSS/JavaScript（无外部依赖，离线可用）
- 小程序：微信小程序原生开发（WXML/WXSS/JS），`rich-text` 富文本渲染，内置轻量 Markdown 转换器
- 认证：轻量 Token（内存实现，区分管理员/普通用户两种角色）

## 四、快速开始

### 1. 后端构建与启动

环境要求：JDK 1.8+、Maven 3.6+（项目根目录已附 `maven-settings.xml` 阿里云镜像）。

```bash
cd backend
mvn -s ../maven-settings.xml clean package -DskipTests
java -jar target/blog-miniapp.jar
```

Windows 下也可以直接双击根目录的 **启动后端.bat**（若未打包会自动先执行 Maven 打包）。

启动成功后：

- 管理后台：http://localhost:8080/admin
- 默认管理员：`admin` / `admin123`（首次登录后请在“修改密码”中更换）
- H2 控制台：http://localhost:8080/h2-console （JDBC URL：`jdbc:h2:mem:blogdb`，用户名 `sa`，密码为空）

> **端口被占用**：8080 被其他程序占用时，修改 `backend/src/main/resources/application.yml` 中的 `server.port`，并同步修改 `share-wxmal/config.js` 中的 `baseUrl`。

### 2. 导入微信小程序

1. 打开微信开发者工具，选择「导入项目」，目录选择 `share-wxmal`；
2. AppID 使用测试号（`touristappid`）或你自己的小程序 AppID；
3. 详情设置中勾选「不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书」以便本地联调；
4. 真机调试时，将 `config.js` 中的 `baseUrl` 改为电脑局域网地址，例如 `http://192.168.1.100:8080`；
5. 编译运行即可。

> 说明：本项目富文本渲染采用微信原生 `rich-text` 组件 + 内置 Markdown 转换器，无需安装第三方 html2wxml 插件；若你的项目需要 html2wxml 组件，可在开发者工具中自行添加该插件并替换 `pages/detail/detail.js` 的渲染逻辑。

## 五、数据库说明

- 默认使用 **H2 内存模式**，项目启动时自动建表并写入基础数据（管理员、4 个分类、示例文章、示例留言、数据字典），无需手动执行 SQL；
- 内存模式数据在服务重启后会重置，适合演示与个人轻量使用；
- 如需持久化，将 `application.yml` 中数据库地址改为文件模式即可：

```yaml
url: jdbc:h2:file:./data/blog;MODE=MySQL;DB_CLOSE_DELAY=-1
```

## 六、主要接口一览

| 接口 | 说明 | 鉴权 |
| --- | --- | --- |
| `POST /api/user/login` | 小程序用户登录（code 换 token） | 无 |
| `GET /api/category/list` | 分类列表 | 无 |
| `GET /api/article/list` | 已发布文章分页（支持 keyword/categoryId） | 无 |
| `GET /api/article/hot` | 热门文章 TOP 6 | 无 |
| `GET /api/article/{id}` | 文章详情（含正文，自动累计浏览量/浏览记录） | 可选 token |
| `GET /api/comment/list` | 文章已通过留言 | 无 |
| `POST /api/comment/add` | 提交留言（待审核） | 用户 token |
| `GET /api/user/info`、`POST /api/user/info` | 获取/修改个人信息 | 用户 token |
| `GET /api/user/history`、`DELETE /api/user/history` | 浏览记录查询/清空 | 用户 token |
| `GET /api/user/comments` | 我的留言 | 用户 token |
| `POST /admin/api/login` | 管理员登录 | 无 |
| `GET /admin/api/stats/overview` | 后台统计 | 管理员 token |
| `GET|POST /admin/api/article/**` | 文章管理（列表/详情/保存/状态/置顶/删除） | 管理员 token |
| `GET|POST|DELETE /admin/api/category/**` | 分类管理 | 管理员 token |
| `GET|POST|DELETE /admin/api/comment/**` | 留言审核/回复/删除 | 管理员 token |
| `GET|POST /admin/api/user/**` | 用户列表/启停 | 管理员 token |
| `GET|POST|DELETE /admin/api/dict/**` | 数据字典 | 管理员 token |
| `GET|DELETE /admin/api/log/**` | 系统日志 | 管理员 token |
| `POST /admin/api/upload` | 图片上传（返回访问路径） | 管理员 token |

## 七、测试结果

已在本机完成 22 项接口自动化测试，**22/22 全部通过**，覆盖：

- 管理员登录、未授权拦截、仪表盘统计
- 小程序登录、用户信息、文章列表/详情（含正文）、分类列表
- 留言提交 → 后台审核 → 管理员回复 → 小程序可见（完整闭环）
- 后台新增/编辑/置顶/删除文章、新增/删除分类
- 图片上传及上传文件 HTTP 可访问性
- 系统日志记录、管理后台页面访问

## 八、常见问题

**1. 小程序请求提示“网络异常”/“url not in domain list”？**

检查后端是否已启动、`config.js` 的 `baseUrl` 是否正确，并在开发者工具中勾选“不校验合法域名”。

**2. 后台文章列表不显示正文？**

正文只在“编辑文章”弹窗和文章详情接口中返回，列表接口为控制体积不返回正文属正常设计。

**3. 忘记后台密码？**

H2 为内存模式时，删除项目后重启即恢复默认账号 `admin/admin123`；如需持久化保存请切换文件模式。

**4. 留言提交后小程序看不到？**

留言默认进入“待审核”状态，需在后台「留言管理」中点击“通过”后才会在小程序端展示。

## 九、UI 设计规范（v3）

管理后台与小程序端统一遵循「开发者向 · 阅读优先」设计规范：

- **双主题**：浅色（#f8fafc）/ 深色（#1e293b），自动跟随系统 `prefers-color-scheme`，支持手动切换并持久化（管理后台右上角按钮 / 小程序个人中心「外观」），优先级：手动设置 > 系统自动
- **色彩**：低饱和配色，唯一品牌高亮色靛蓝 #3b82f6，无刺眼色块、无渐变装饰
- **排版**：正文固定 16px、行高 1.8、字间距 0.01rem；标题 h1-h3 字号字重梯度递减
- **布局**：统一 6px 基础圆角、四边安全留白、固定留白梯度，卡片/段落不贴边不堆砌
- **层次**：仅靠字号、字重、透明度、间距、轻微底色区分层级，不用多重边框与阴影
- **交互**：柔和 hover/点击反馈，无剧烈闪烁动画；代码块深色底、横向滚动、正文可一键复制
- **极简纹理**：页面底层仅叠加极低透明度网格噪点，不干扰阅读

## 十、微信授权登录接入说明

本系统登录采用微信小程序标准授权流程：

1. **静默登录**：小程序端 `wx.login()` 获取临时 code → 后端调用微信接口 `sns/jscode2session` 换取真实 `openid`（同一微信用户在不同小程序下 openid 稳定唯一）
2. **头像昵称**：使用官方「头像昵称填写能力」——`<button open-type="chooseAvatar">` 选择头像 + `<input type="nickname">` 填写昵称（旧版 `wx.getUserProfile` 已被微信废弃，不再使用）
3. **上线前配置**：
   - 在 `backend/src/main/resources/application.yml` 的 `blog.wechat.appid / blog.wechat.secret` 填入小程序 AppID 与 AppSecret（微信公众平台 → 开发管理 → 开发设置）
   - 在小程序后台「开发管理 → 服务器域名」配置 request 合法域名（指向你部署的后端 HTTPS 域名）
   - 正式发布环境要求后端为 HTTPS
4. **开发兜底**：未配置 appid/secret 时，后端以 code 模拟 openid 完成登录，仅用于本地联调；上线前不配置会无法通过微信身份校验（返回“微信登录校验失败”）
## 十一、管理员绑定微信 / 强制登录 / 文章封面

1. **管理员绑定微信**：后台密码登录 → 「修改密码」页 → 点「生成绑定码」；用微信打开小程序「个人博客」→ 我的 → 管理员 · 微信确认 → 输入绑定码 → 「绑定到管理员账号」。
2. **扫码登录后台**：后台登录页选「微信登录」，配置了微信凭据时会生成小程序码，用绑定过管理员的微信扫码直达「管理员微信确认」页并自动带出验证码，点「确认登录后台」即登录；未配置凭据时自动降级为 6 位验证码模式。
3. **微信授权登录（进入小程序）**：小程序取消访客模式与自动登录，首页/详情/个人中心均设登录门禁；用户点击「微信授权登录」→ 点击授权头像（官方 chooseAvatar）→ 填写昵称 → 一键登录后进入。
3. **文章封面**：后台文章编辑弹窗支持封面上传（实时预览）；小程序首页列表卡片展示封面，未设置封面时显示浅灰占位块。
## 十二、上线发布清单（AppID: wx23ad1bc9fe81c726）

1. **后端部署**：准备一台服务器（Linux/Windows 均可），安装 JDK 8+；`mvn clean package -DskipTests` 后 `java -jar target/blog-miniapp.jar` 运行；建议用 Nginx 把 `https://你的域名` 反向代理到 8080 端口（需 ICP 备案域名 + HTTPS 证书）。
2. **填写微信凭据**：在 `application.yml` 填入 `blog.wechat.appid`（已填）和 `blog.wechat.secret`（微信公众平台 → 开发管理 → 开发设置 → AppSecret，只显示一次，自己保管）。
3. **小程序后台域名配置**（mp.weixin.qq.com → 开发管理 → 开发设置 → 服务器域名）：
   - request 合法域名：`https://你的域名`
   - uploadFile 合法域名：`https://你的域名`（用户头像上传）
   - downloadFile 合法域名：`https://你的域名`（封面/图片展示）
4. **小程序端**：`config.js` 的 `baseUrl` 改为 `https://你的域名`；开发者工具中用 AppID `wx23ad1bc9fe81c726` 导入，清缓存编译，真机预览走通全流程。
5. **提审发布**：小程序后台 → 版本管理 → 上传代码（开发者工具点“上传”）→ 提交审核（选好类目与简介）→ 审核通过后发布。
6. **上线后**：用真实微信重新绑定管理员（正式 openid 与开发 mock 不同）；修改后台默认密码 admin123；生产环境建议关闭 H2 控制台、数据库切换文件模式。