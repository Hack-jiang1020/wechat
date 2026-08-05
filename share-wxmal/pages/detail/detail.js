const api = require('../../utils/request.js');
const util = require('../../utils/util.js');
const md = require('../../utils/markdown.js');

Page({
  data: {
    id: null,
    article: null,
    htmlNodes: '',
    loaded: false
  },

  onLoad(options) {
    this.setThemeData();
    const id = options.id;
    this.setData({ id: id });
    this.loadArticle(id);
  },

  setThemeData() {
    this.setData({ themeClass: getApp().globalData.themeClass });
  },

  onShow() {
    this.setThemeData();
  },

  onShareAppMessage() {
    const a = this.data.article;
    return {
      title: a ? a.title : '个人博客文章',
      path: '/pages/detail/detail?id=' + this.data.id
    };
  },

  loadArticle(id) {
    api.get('/api/article/' + id).then((article) => {
      article.cover = api.absUrl(article.cover);
      const html = this.buildHtml(article.content);
      this.setData({ article: article, htmlNodes: html, loaded: true });
      wx.setNavigationBarTitle({ title: article.title || '文章详情' });
    }).catch(() => {
      this.setData({ loaded: true });
    });
  },

  buildHtml(content) {
    if (!content) return '';
    const looksLikeHtml = /<\/?[a-z][\s\S]*>/i.test(content);
    let html = looksLikeHtml ? content : md.md2html(content);
    // 相对路径图片/链接转换为绝对地址
    html = html.replace(/(<img[^>]+src=["'])\/(?!\/)/g, '$1' + api.baseUrl + '/');
    html = html.replace(/(<a[^>]+href=["'])\/(?!\/)/g, '$1' + api.baseUrl + '/');
    // 移动端富文本样式增强
    html = html.replace(/<img /g, '<img style="max-width:100%;height:auto;border-radius:6px;display:block;margin:16px auto;" ');
    html = html.replace(/<pre>/g, '<pre style="background:#0f172a;color:#e2e8f0;padding:14px;border-radius:6px;overflow-x:auto;line-height:1.7;font-size:13px;">');
    html = html.replace(/<code>/g, '<code style="background:#f1f5f9;color:#1e293b;padding:2px 6px;border-radius:4px;font-size:13px;">');
    html = html.replace(/<pre><code style="[^"]*"/g, '<pre><code style="background:transparent;padding:0;color:inherit;"');
    html = html.replace(/<blockquote>/g, '<blockquote style="margin:16px 0;padding:12px 16px;background:#f1f5f9;border-left:4px solid #3b82f6;color:#475569;border-radius:6px;">');
    html = html.replace(/<table>/g, '<table style="width:100%;border-collapse:collapse;font-size:13px;">');
    html = html.replace(/<td|<th/g, function (m) {
      return m + ' style="border:1px solid #e2e8f0;padding:8px;text-align:left;"';
    });
    return html;
  },


  formatTime: util.formatTime
});
