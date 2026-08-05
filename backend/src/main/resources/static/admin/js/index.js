/* ---------- 工具 ---------- */
function esc(text) {
    if (text === null || text === undefined) return '';
    return String(text).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function fmtTime(time) {
    if (!time) return '-';
    return String(time).replace('T', ' ').substring(0, 19);
}

function showToast(msg) {
    const toast = document.getElementById('toast');
    toast.textContent = msg;
    toast.classList.add('show');
    clearTimeout(showToast.timer);
    showToast.timer = setTimeout(() => toast.classList.remove('show'), 2200);
}

function closeModal(id) {
    document.getElementById(id).classList.remove('show');
}

function openModal(id) {
    document.getElementById(id).classList.add('show');
}

function statusBadge(type, value) {
    if (type === 'article') {
        return value === 1 ? '<span class="badge badge-green">已发布</span>' : '<span class="badge badge-gray">草稿</span>';
    }
    if (type === 'comment') {
        return value === 0 ? '<span class="badge badge-orange">待审核</span>'
            : value === 1 ? '<span class="badge badge-green">已通过</span>'
                : '<span class="badge badge-red">已拒绝</span>';
    }
    return value === 1 ? '<span class="badge badge-green">启用</span>' : '<span class="badge badge-gray">停用</span>';
}

/* ---------- 动效工具 ---------- */
function showSkeleton(tbodyId, cols) {
    const tbody = document.getElementById(tbodyId);
    if (!tbody) return;
    let html = '';
    for (let i = 0; i < 6; i++) {
        html += '<tr class="skeleton-row">';
        for (let c = 0; c < cols; c++) {
            html += '<td><span class="skeleton"></span></td>';
        }
        html += '</tr>';
    }
    tbody.innerHTML = html;
}

/* ---------- 全局状态 ---------- */
const state = {
    section: 'dashboard',
    articlePage: 1,
    commentPage: 1,
    userPage: 1,
    logPage: 1,
    categories: [],
    replyCommentId: null,
    dictType: ''
};

const PAGE_SIZE = 10;
const LOG_PAGE_SIZE = 20;

/* ---------- 初始化 ---------- */
document.addEventListener('DOMContentLoaded', async () => {
    if (!Api.token) {
        location.href = 'login.html';
        return;
    }
    bindEvents();
    await loadAdminInfo();
    await loadCategories();
    switchSection('dashboard');
});

function bindEvents() {
    document.querySelectorAll('.nav-item[data-section]').forEach(item => {
        item.addEventListener('click', () => switchSection(item.dataset.section));
    });
    document.getElementById('navPassword').addEventListener('click', () => switchSection('password'));
    document.getElementById('navLogout').addEventListener('click', async () => {
        try { await Api.post('/admin/api/logout', {}); } catch (e) { /* ignore */ }
        localStorage.removeItem('blog_admin_token');
        location.href = 'login.html';
    });

    document.getElementById('articleSearchBtn').addEventListener('click', () => { state.articlePage = 1; loadArticles(); });
    document.getElementById('categorySearchBtn').addEventListener('click', loadCategories);
    document.getElementById('commentSearchBtn').addEventListener('click', () => { state.commentPage = 1; loadComments(); });
    document.getElementById('userSearchBtn').addEventListener('click', () => { state.userPage = 1; loadUsers(); });
    document.getElementById('dictSearchBtn').addEventListener('click', () => {
        state.dictType = document.getElementById('dictTypeFilter').value;
        loadDicts();
    });
    document.getElementById('clearLogBtn').addEventListener('click', clearLogs);
    document.getElementById('changePasswordBtn').addEventListener('click', changePassword);
    document.getElementById('coverFile').addEventListener('change', uploadCover);
    document.getElementById('articleCover').addEventListener('input', updateCoverPreview);
    document.getElementById('dictType').addEventListener('change', function () {
        document.getElementById('dictTypeCustom').style.display = this.value === '__custom__' ? 'block' : 'none';
    });

    ['articleKeyword', 'articleStatusFilter', 'commentKeyword', 'commentStatusFilter', 'userKeyword'].forEach(id => {
        document.getElementById(id).addEventListener('keydown', e => {
            if (e.key === 'Enter') {
                if (id.startsWith('article')) { state.articlePage = 1; loadArticles(); }
                else if (id.startsWith('comment')) { state.commentPage = 1; loadComments(); }
                else { state.userPage = 1; loadUsers(); }
            }
        });
    });
}

async function loadAdminInfo() {
    try {
        const admin = await Api.get('/admin/api/info');
        document.getElementById('topbarUser').textContent = (admin.nickname || admin.username) + '（管理员）';
    } catch (e) {
        showToast(e.message);
    }
}

async function loadCategories() {
    try {
        state.categories = await Api.get('/admin/api/category/list');
        const filter = document.getElementById('articleCategoryFilter');
        const articleCat = document.getElementById('articleCategory');
        filter.innerHTML = '<option value="">全部分类</option>';
        articleCat.innerHTML = '<option value="">请选择分类</option>';
        state.categories.forEach(c => {
            filter.innerHTML += `<option value="${c.id}">${esc(c.name)}</option>`;
            articleCat.innerHTML += `<option value="${c.id}">${esc(c.name)}</option>`;
        });
        if (state.section === 'categories') renderCategories();
    } catch (e) {
        showToast(e.message);
    }
}

/* ---------- 页面切换 ---------- */
function switchSection(name) {
    state.section = name;
    document.querySelectorAll('.section').forEach(s => s.style.display = 'none');
    document.getElementById('section-' + name).style.display = 'block';
    document.querySelectorAll('.nav-item[data-section]').forEach(n => n.classList.toggle('active', n.dataset.section === name));
    document.getElementById('navPassword').classList.toggle('active', name === 'password');
    const titles = {
        dashboard: '仪表盘', articles: '文章管理', categories: '分类管理', comments: '留言管理',
        users: '用户管理', dicts: '数据字典', logs: '系统日志', password: '修改密码'
    };
    document.getElementById('pageTitle').textContent = titles[name] || '';
    if (name === 'password') loadWxBindStatus();
    if (name === 'dashboard') loadDashboard();
    if (name === 'articles') loadArticles();
    if (name === 'categories') renderCategories();
    if (name === 'comments') loadComments();
    if (name === 'users') loadUsers();
    if (name === 'dicts') loadDicts();
    if (name === 'logs') loadLogs();
}

/* ---------- 仪表盘 ---------- */
async function loadDashboard() {
    try {
        const data = await Api.get('/admin/api/stats/overview');
        const cards = [
            { num: data.articleTotal, label: '文章总数' },
            { num: data.articlePublished, label: '已发布文章' },
            { num: data.categoryTotal, label: '分类数量' },
            { num: data.userTotal, label: '小程序用户' },
            { num: data.commentTotal, label: '留言总数' },
            { num: data.commentPending, label: '待审核留言' }
        ];
        document.getElementById('statCards').innerHTML = cards.map(c => `
            <div class="stat-card">
                <div><div class="stat-num">${c.num}</div><div class="stat-label">${c.label}</div></div>
            </div>`).join('');
        showSkeleton('hotTable', 5);
        document.getElementById('hotTable').innerHTML = (data.hotArticles || []).map(a => `
            <tr>
                <td>${esc(a.title)}</td>
                <td>${esc(a.categoryName || '-')}</td>
                <td>${a.views}</td>
                <td>${a.likes}</td>
                <td>${fmtTime(a.createTime)}</td>
            </tr>`).join('') || '<tr><td colspan="5" class="empty-tip">暂无文章</td></tr>';
    } catch (e) {
        showToast(e.message);
    }
}

/* ---------- 文章管理 ---------- */
async function loadArticles() {
    const keyword = document.getElementById('articleKeyword').value.trim();
    const categoryId = document.getElementById('articleCategoryFilter').value;
    const status = document.getElementById('articleStatusFilter').value;
    const params = new URLSearchParams({ page: state.articlePage, size: PAGE_SIZE });
    if (keyword) params.set('keyword', keyword);
    if (categoryId) params.set('categoryId', categoryId);
    if (status !== '') params.set('status', status);
    showSkeleton('articleTable', 8);
    try {
        const data = await Api.get('/admin/api/article/list?' + params.toString());
        const tbody = document.getElementById('articleTable');
        tbody.innerHTML = data.list.map(a => `
            <tr>
                <td>${a.cover ? `<img class="cover-thumb" src="${esc(a.cover)}" onerror="this.style.display='none'">` : '<span class="cover-thumb"></span>'}</td>
                <td title="${esc(a.title)}">${esc(a.title)}</td>
                <td>${esc(a.categoryName || '-')}</td>
                <td>${statusBadge('article', a.status)}</td>
                <td>${a.isTop === 1 ? '<span class="badge badge-purple">置顶</span>' : '-'}</td>
                <td>${a.views}</td>
                <td>${fmtTime(a.createTime)}</td>
                <td>
                    <button class="btn btn-sm" onclick="openArticleModal(${a.id})">编辑</button>
                    <button class="btn btn-sm" onclick="toggleTop(${a.id}, ${a.isTop === 1 ? 0 : 1})">${a.isTop === 1 ? '取消置顶' : '置顶'}</button>
                    <button class="btn btn-sm" onclick="toggleStatus(${a.id}, ${a.status === 1 ? 0 : 1})">${a.status === 1 ? '下架' : '发布'}</button>
                    <button class="btn btn-sm btn-danger" onclick="deleteArticle(${a.id})">删除</button>
                </td>
            </tr>`).join('') || '<tr><td colspan="8" class="empty-tip">暂无文章，点击右上角新增</td></tr>';
        renderPagination('articlePagination', data.total, state.articlePage, PAGE_SIZE, page => { state.articlePage = page; loadArticles(); });
    } catch (e) {
        showToast(e.message);
    }
}

function openArticleModal(id) {
    document.getElementById('articleModalTitle').textContent = id ? '编辑文章' : '新增文章';
    const form = {
        id: document.getElementById('articleId'),
        title: document.getElementById('articleTitle'),
        category: document.getElementById('articleCategory'),
        status: document.getElementById('articleStatus'),
        top: document.getElementById('articleTop'),
        summary: document.getElementById('articleSummary'),
        cover: document.getElementById('articleCover'),
        content: document.getElementById('articleContent')
    };
    form.id.value = '';
    form.title.value = '';
    form.category.value = '';
    form.status.value = '1';
    form.top.value = '0';
    form.summary.value = '';
    form.cover.value = '';
    form.content.value = '';
    if (id) {
        Api.get('/admin/api/article/' + id).then(a => {
            form.id.value = a.id;
            form.title.value = a.title || '';
            form.category.value = a.categoryId || '';
            form.status.value = a.status;
            form.top.value = a.isTop;
            form.summary.value = a.summary || '';
            form.cover.value = a.cover || '';
            form.content.value = a.content || '';
            updateCoverPreview();
        }).catch(e => showToast(e.message));
    }
    updateCoverPreview();
    openModal('articleModal');
}

async function saveArticle() {
    const payload = {
        id: document.getElementById('articleId').value || null,
        title: document.getElementById('articleTitle').value.trim(),
        categoryId: document.getElementById('articleCategory').value || null,
        status: parseInt(document.getElementById('articleStatus').value),
        isTop: parseInt(document.getElementById('articleTop').value),
        summary: document.getElementById('articleSummary').value.trim(),
        cover: document.getElementById('articleCover').value.trim(),
        content: document.getElementById('articleContent').value
    };
    if (!payload.title) return showToast('文章标题不能为空');
    if (!payload.categoryId) return showToast('请选择文章分类');
    const saveBtn = document.querySelector('#articleModal .modal-footer .btn-primary');
    saveBtn.disabled = true;
    saveBtn.textContent = '保存中...';
    try {
        await Api.post('/admin/api/article/save', payload);
        closeModal('articleModal');
        showToast('保存成功');
        loadArticles();
        loadDashboard();
    } catch (e) {
        saveBtn.disabled = false;
        saveBtn.textContent = '保存文章';
        showToast(e.message);
    }
}

async function toggleStatus(id, status) {
    try {
        await Api.post(`/admin/api/article/${id}/status`, { status });
        showToast(status === 1 ? '已发布' : '已下架');
        loadArticles();
    } catch (e) { showToast(e.message); }
}

async function toggleTop(id, isTop) {
    try {
        await Api.post(`/admin/api/article/${id}/top`, { isTop });
        showToast(isTop === 1 ? '已置顶' : '已取消置顶');
        loadArticles();
    } catch (e) { showToast(e.message); }
}

async function deleteArticle(id) {
    if (!confirm('确定删除该文章吗？（逻辑删除，可保留原始数据）')) return;
    try {
        await Api.del('/admin/api/article/' + id);
        showToast('删除成功');
        loadArticles();
        loadDashboard();
    } catch (e) { showToast(e.message); }
}

function insertTag(tag, prefix, suffix) {
    const ta = document.getElementById('articleContent');
    const start = ta.selectionStart;
    const end = ta.selectionEnd;
    const sel = ta.value.substring(start, end) || '内容';
    const tagName = tag.split(' ')[0];
    const open = prefix || `<${tag}>`;
    const close = suffix || `</${tagName}>`;
    const insert = open + sel + close;
    ta.value = ta.value.substring(0, start) + insert + ta.value.substring(end);
    ta.focus();
    ta.selectionStart = ta.selectionEnd = start + insert.length;
}

async function insertImage() {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    input.onchange = async () => {
        const file = input.files[0];
        if (!file) return;
        try {
            const data = await Api.upload(file);
            insertRaw(`<img src="${data.url}" alt="图片" style="max-width:100%;border-radius:8px">`);
        } catch (e) { showToast(e.message); }
    };
    input.click();
}

function insertRaw(html) {
    const ta = document.getElementById('articleContent');
    const start = ta.selectionStart;
    const end = ta.selectionEnd;
    ta.value = ta.value.substring(0, start) + html + ta.value.substring(end);
    ta.focus();
}

function updateCoverPreview() {
  const url = document.getElementById('articleCover').value.trim();
  const img = document.getElementById('articleCoverPreview');
  if (url) {
    img.src = url;
    img.style.display = 'inline-block';
  } else {
    img.style.display = 'none';
  }
}

async function uploadCover(e) {
    const file = e.target.files[0];
    if (!file) return;
    try {
        const data = await Api.upload(file);
        document.getElementById('articleCover').value = data.url;
        updateCoverPreview();
        showToast('封面上传成功');
    } catch (err) {
        showToast(err.message);
    }
    e.target.value = '';
}

/* ---------- 分类管理 ---------- */
function renderCategories() {
    const keyword = document.getElementById('categoryKeyword').value.trim().toLowerCase();
    const list = state.categories.filter(c => !keyword || c.name.toLowerCase().includes(keyword));
    document.getElementById('categoryTable').innerHTML = list.map(c => `
        <tr>
            <td>${c.id}</td>
            <td>${esc(c.name)}</td>
            <td>${c.sort}</td>
            <td>${statusBadge('common', c.status)}</td>
            <td>${esc(c.remark || '-')}</td>
            <td>${fmtTime(c.createTime)}</td>
            <td>
                <button class="btn btn-sm" onclick="openCategoryModal(${c.id})">编辑</button>
                <button class="btn btn-sm btn-danger" onclick="deleteCategory(${c.id})">删除</button>
            </td>
        </tr>`).join('') || '<tr><td colspan="7" class="empty-tip">暂无分类</td></tr>';
}

function openCategoryModal(id) {
    document.getElementById('categoryModalTitle').textContent = id ? '编辑分类' : '新增分类';
    document.getElementById('categoryId').value = '';
    document.getElementById('categoryName').value = '';
    document.getElementById('categorySort').value = '0';
    document.getElementById('categoryStatus').value = '1';
    document.getElementById('categoryRemark').value = '';
    if (id) {
        const c = state.categories.find(x => x.id === id);
        if (c) {
            document.getElementById('categoryId').value = c.id;
            document.getElementById('categoryName').value = c.name;
            document.getElementById('categorySort').value = c.sort;
            document.getElementById('categoryStatus').value = c.status;
            document.getElementById('categoryRemark').value = c.remark || '';
        }
    }
    openModal('categoryModal');
}

async function saveCategory() {
    const payload = {
        id: document.getElementById('categoryId').value || null,
        name: document.getElementById('categoryName').value.trim(),
        sort: parseInt(document.getElementById('categorySort').value || 0),
        status: parseInt(document.getElementById('categoryStatus').value),
        remark: document.getElementById('categoryRemark').value.trim()
    };
    if (!payload.name) return showToast('分类名称不能为空');
    try {
        await Api.post('/admin/api/category/save', payload);
        closeModal('categoryModal');
        showToast('保存成功');
        await loadCategories();
        loadDashboard();
    } catch (e) { showToast(e.message); }
}

async function deleteCategory(id) {
    if (!confirm('确定删除该分类吗？')) return;
    try {
        await Api.del('/admin/api/category/' + id);
        showToast('删除成功');
        await loadCategories();
        loadDashboard();
    } catch (e) { showToast(e.message); }
}

/* ---------- 留言管理 ---------- */
async function loadComments() {
    const keyword = document.getElementById('commentKeyword').value.trim();
    const status = document.getElementById('commentStatusFilter').value;
    const params = new URLSearchParams({ page: state.commentPage, size: PAGE_SIZE });
    if (keyword) params.set('keyword', keyword);
    if (status !== '') params.set('status', status);
    showSkeleton('commentTable', 7);
    try {
        const data = await Api.get('/admin/api/comment/list?' + params.toString());
        document.getElementById('commentTable').innerHTML = data.list.map(c => `
            <tr>
                <td>${c.avatar ? `<img class="avatar-thumb" src="${esc(c.avatar)}">` : '<span class="avatar-thumb">' + esc((c.nickname || '微')[0]) + '</span>'} ${esc(c.nickname || '匿名')}</td>
                <td title="${esc(c.articleTitle)}">${esc(c.articleTitle || '-')}</td>
                <td title="${esc(c.content)}">${esc(c.content)}</td>
                <td>${statusBadge('comment', c.status)}</td>
                <td>${c.replyContent ? `<div class="reply-box">${esc(c.replyContent)}<br><small>${fmtTime(c.replyTime)}</small></div>` : '-'}</td>
                <td>${fmtTime(c.createTime)}</td>
                <td>
                    ${c.status !== 1 ? `<button class="btn btn-sm btn-success" onclick="reviewComment(${c.id}, 1)">通过</button>` : ''}
                    ${c.status !== 2 ? `<button class="btn btn-sm" onclick="reviewComment(${c.id}, 2)">拒绝</button>` : ''}
                    <button class="btn btn-sm" onclick="openReplyModal(${c.id})">回复</button>
                    <button class="btn btn-sm btn-danger" onclick="deleteComment(${c.id})">删除</button>
                </td>
            </tr>`).join('') || '<tr><td colspan="7" class="empty-tip">暂无留言</td></tr>';
        renderPagination('commentPagination', data.total, state.commentPage, PAGE_SIZE, page => { state.commentPage = page; loadComments(); });
    } catch (e) { showToast(e.message); }
}

async function reviewComment(id, status) {
    try {
        await Api.post(`/admin/api/comment/${id}/review`, { status });
        showToast(status === 1 ? '已通过' : '已拒绝');
        loadComments();
        loadDashboard();
    } catch (e) { showToast(e.message); }
}

function openReplyModal(id) {
    state.replyCommentId = id;
    document.getElementById('replyContent').value = '';
    openModal('replyModal');
}

async function submitReply() {
    const content = document.getElementById('replyContent').value.trim();
    if (!content) return showToast('回复内容不能为空');
    try {
        await Api.post(`/admin/api/comment/${state.replyCommentId}/reply`, { replyContent: content });
        closeModal('replyModal');
        showToast('回复成功');
        loadComments();
    } catch (e) { showToast(e.message); }
}

async function deleteComment(id) {
    if (!confirm('确定删除该留言吗？')) return;
    try {
        await Api.del('/admin/api/comment/' + id);
        showToast('删除成功');
        loadComments();
        loadDashboard();
    } catch (e) { showToast(e.message); }
}

/* ---------- 用户管理 ---------- */
async function loadUsers() {
    const keyword = document.getElementById('userKeyword').value.trim();
    const params = new URLSearchParams({ page: state.userPage, size: PAGE_SIZE });
    if (keyword) params.set('keyword', keyword);
    showSkeleton('userTable', 8);
    try {
        const data = await Api.get('/admin/api/user/list?' + params.toString());
        document.getElementById('userTable').innerHTML = data.list.map(u => `
            <tr>
                <td>${u.id}</td>
                <td>${u.avatar ? `<img class="avatar-thumb" src="${esc(u.avatar)}">` : '<span class="avatar-thumb">' + esc((u.nickname || '微')[0]) + '</span>'}</td>
                <td>${esc(u.nickname || '微信用户')}</td>
                <td style="max-width:180px">${esc(u.openid)}</td>
                <td>${statusBadge('common', u.status)}</td>
                <td>${fmtTime(u.createTime)}</td>
                <td>${fmtTime(u.lastLoginTime)}</td>
                <td>
                    <button class="btn btn-sm" onclick="toggleUserStatus(${u.id}, ${u.status === 1 ? 0 : 1})">${u.status === 1 ? '禁用' : '启用'}</button>
                </td>
            </tr>`).join('') || '<tr><td colspan="8" class="empty-tip">暂无用户</td></tr>';
        renderPagination('userPagination', data.total, state.userPage, PAGE_SIZE, page => { state.userPage = page; loadUsers(); });
    } catch (e) { showToast(e.message); }
}

async function toggleUserStatus(id, status) {
    try {
        await Api.post(`/admin/api/user/${id}/status`, { status });
        showToast(status === 1 ? '已启用' : '已禁用');
        loadUsers();
    } catch (e) { showToast(e.message); }
}

/* ---------- 数据字典 ---------- */
async function loadDicts() {
    const url = state.dictType ? '/admin/api/dict/list?type=' + state.dictType : '/admin/api/dict/list';
    showSkeleton('dictTable', 8);
    try {
        const list = await Api.get(url);
        document.getElementById('dictTable').innerHTML = list.map(d => `
            <tr>
                <td>${d.id}</td>
                <td><span class="badge badge-blue">${esc(d.dictType)}</span></td>
                <td>${esc(d.dictLabel)}</td>
                <td>${esc(d.dictValue)}</td>
                <td>${d.sort}</td>
                <td>${statusBadge('common', d.status)}</td>
                <td>${esc(d.remark || '-')}</td>
                <td>
                    <button class="btn btn-sm" onclick="openDictModal(${d.id})">编辑</button>
                    <button class="btn btn-sm btn-danger" onclick="deleteDict(${d.id})">删除</button>
                </td>
            </tr>`).join('') || '<tr><td colspan="8" class="empty-tip">暂无字典数据</td></tr>';
    } catch (e) { showToast(e.message); }
}

function openDictModal(id) {
    document.getElementById('dictModalTitle').textContent = id ? '编辑字典' : '新增字典';
    document.getElementById('dictId').value = '';
    document.getElementById('dictType').value = 'article_status';
    document.getElementById('dictTypeCustom').style.display = 'none';
    document.getElementById('dictLabel').value = '';
    document.getElementById('dictValue').value = '';
    document.getElementById('dictSort').value = '0';
    document.getElementById('dictStatus').value = '1';
    document.getElementById('dictRemark').value = '';
    if (id) {
        Api.get('/admin/api/dict/list').then(list => {
            const d = list.find(x => x.id === id);
            if (!d) return;
            document.getElementById('dictId').value = d.id;
            const known = ['article_status', 'comment_status', 'is_top'];
            if (known.includes(d.dictType)) {
                document.getElementById('dictType').value = d.dictType;
            } else {
                document.getElementById('dictType').value = '__custom__';
                document.getElementById('dictTypeCustom').style.display = 'block';
                document.getElementById('dictTypeCustom').value = d.dictType;
            }
            document.getElementById('dictLabel').value = d.dictLabel;
            document.getElementById('dictValue').value = d.dictValue;
            document.getElementById('dictSort').value = d.sort;
            document.getElementById('dictStatus').value = d.status;
            document.getElementById('dictRemark').value = d.remark || '';
        }).catch(e => showToast(e.message));
    }
    openModal('dictModal');
}

async function saveDict() {
    let dictType = document.getElementById('dictType').value;
    if (dictType === '__custom__') {
        dictType = document.getElementById('dictTypeCustom').value.trim();
        if (!dictType) return showToast('请填写自定义字典类型');
    }
    const payload = {
        id: document.getElementById('dictId').value || null,
        dictType,
        dictLabel: document.getElementById('dictLabel').value.trim(),
        dictValue: document.getElementById('dictValue').value.trim(),
        sort: parseInt(document.getElementById('dictSort').value || 0),
        status: parseInt(document.getElementById('dictStatus').value),
        remark: document.getElementById('dictRemark').value.trim()
    };
    if (!payload.dictLabel || !payload.dictValue) return showToast('字典名称和值不能为空');
    try {
        await Api.post('/admin/api/dict/save', payload);
        closeModal('dictModal');
        showToast('保存成功');
        loadDicts();
    } catch (e) { showToast(e.message); }
}

async function deleteDict(id) {
    if (!confirm('确定删除该字典项吗？')) return;
    try {
        await Api.del('/admin/api/dict/' + id);
        showToast('删除成功');
        loadDicts();
    } catch (e) { showToast(e.message); }
}

/* ---------- 系统日志 ---------- */
async function loadLogs() {
    showSkeleton('logTable', 6);
    try {
        const data = await Api.get(`/admin/api/log/list?page=${state.logPage}&size=${LOG_PAGE_SIZE}`);
        document.getElementById('logTable').innerHTML = data.list.map(l => `
            <tr>
                <td>${l.id}</td>
                <td><span class="badge ${l.method === 'GET' ? 'badge-blue' : 'badge-purple'}">${esc(l.method)}</span></td>
                <td style="max-width:280px">${esc(l.path)}</td>
                <td>${esc(l.ip || '-')}</td>
                <td>${l.costMs}</td>
                <td>${fmtTime(l.createTime)}</td>
            </tr>`).join('') || '<tr><td colspan="6" class="empty-tip">暂无日志</td></tr>';
        renderPagination('logPagination', data.total, state.logPage, LOG_PAGE_SIZE, page => { state.logPage = page; loadLogs(); });
    } catch (e) { showToast(e.message); }
}

async function clearLogs() {
    if (!confirm('确定清空所有系统日志吗？')) return;
    try {
        await Api.del('/admin/api/log/clear');
        showToast('日志已清空');
        state.logPage = 1;
        loadLogs();
    } catch (e) { showToast(e.message); }
}

/* ---------- 修改密码 ---------- */
async function loadWxBindStatus() {
  const el = document.getElementById('wxBindStatus');
  if (!el) return;
  try {
    const info = await Api.get('/admin/api/info');
    if (info.openid) {
      el.innerHTML = '当前已绑定微信号：<b>' + esc(info.openid) + '</b>（需更换请先解绑）';
      el.className = 'wx-bind-status bound';
    } else {
      el.textContent = '当前未绑定微信号';
      el.className = 'wx-bind-status';
    }
  } catch (e) {
    el.textContent = '检测绑定状态失败';
  }
}

async function unbindWx() {
  if (!confirm('确定解绑当前绑定的微信号吗？解绑后需重新绑定才能用微信登录后台。')) return;
  try {
    await Api.post('/admin/api/wx/unbind', {});
    showToast('已解绑，可以绑定新的微信号');
    loadWxBindStatus();
  } catch (e) {
    showToast(e.message);
  }
}

async function bindWx() {
  const box = document.getElementById('wxBindBox');
  try {
    const data = await Api.post('/admin/api/wx/bind', {});
    box.innerHTML = '<div class="wx-code" style="text-align:center;font-size:38px;font-weight:800;letter-spacing:10px;color:var(--brand);padding:14px 0 4px">' + data.code + '</div>' +
      '<p style="color:var(--text-2);font-size:13px;text-align:center;line-height:1.9">5 分钟内有效，打开小程序「个人博客」<br>我的 → 管理员 · 微信确认 → 绑定到管理员账号</p>';
    box.style.display = 'block';
  } catch (e) {
    showToast(e.message);
  }
}

async function changePassword() {
    const oldPassword = document.getElementById('oldPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    if (!oldPassword || !newPassword) return showToast('请填写完整');
    if (newPassword.length < 6) return showToast('新密码长度不能少于6位');
    if (newPassword !== confirmPassword) return showToast('两次输入的新密码不一致');
    try {
        await Api.post('/admin/api/password', { oldPassword, newPassword });
        showToast('密码修改成功，请重新登录');
        setTimeout(() => {
            localStorage.removeItem('blog_admin_token');
            location.href = 'login.html';
        }, 1200);
    } catch (e) { showToast(e.message); }
}

/* ---------- 分页 ---------- */
function renderPagination(elId, total, page, size, onPage) {
    const totalPages = Math.max(1, Math.ceil(total / size));
    const el = document.getElementById(elId);
    if (totalPages <= 1) {
        el.innerHTML = '';
        return;
    }
    let html = `<span>共 ${total} 条，第 ${page}/${totalPages} 页</span>`;
    html += `<button class="btn btn-sm" ${page <= 1 ? 'disabled' : ''} onclick="goPage(${page - 1})">上一页</button>`;
    html += `<button class="btn btn-sm" ${page >= totalPages ? 'disabled' : ''} onclick="goPage(${page + 1})">下一页</button>`;
    el.innerHTML = html;
    window.goPage = onPage;
}
