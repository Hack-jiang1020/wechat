const api = require('../../utils/request.js');
const util = require('../../utils/util.js');

Page({
  data: {
    categories: [],
    activeCategory: 0,
    keyword: '',
    articles: [],
    page: 1,
    total: 0,
    loading: false,
    finished: false,
    initialized: false,
  },

  onLoad() {
    this.setThemeData();
    this.loadCategories();
    this.loadArticles(true);
  },

  onShow() {
    this.setThemeData();
    if (!this.data.initialized && this.data.articles.length === 0) {
      this.loadCategories();
      this.loadArticles(true);
    }
  },

  setThemeData() {
    this.setData({
      themeClass: getApp().globalData.themeClass
    });
  },

  onPullDownRefresh() {
    this.loadCategories();
    this.loadArticles(true).finally(() => wx.stopPullDownRefresh());
  },

  onReachBottom() {
    if (!this.data.finished && !this.data.loading) {
      this.loadArticles();
    }
  },

  onShareAppMessage() {
    return {
      title: '个人博客 - 记录与分享',
      path: '/pages/index/index'
    };
  },

  loadCategories() {
    api.get('/api/category/list').then((list) => {
      this.setData({ categories: list || [] });
    }).catch(() => {});
  },

  loadArticles(reset) {
    if (this.data.loading) return Promise.resolve();
    const page = reset ? 1 : this.data.page + 1;
    this.setData({ loading: true });
    return api.get('/api/article/list', {
      page: page,
      size: 10,
      keyword: this.data.keyword,
      categoryId: this.data.activeCategory || ''
    }).then((data) => {
      const list = data.list || [];
      list.forEach(function (item) {
        item.coverChar = (item.title || '博').charAt(0);
        item.cover = api.absUrl(item.cover);
      });
      this.setData({
        articles: reset ? list : this.data.articles.concat(list),
        page: page,
        total: data.total || 0,
        finished: this.data.articles.length + list.length >= (data.total || 0),
        loading: false,
        initialized: true
      });
    }).catch(() => {
      this.setData({ loading: false, initialized: true });
    });
  },

  onCategoryTap(e) {
    const id = e.currentTarget.dataset.id || 0;
    if (id === this.data.activeCategory) return;
    this.setData({ activeCategory: id, articles: [], finished: false, initialized: false });
    this.loadArticles(true);
  },

  onSearchInput(e) {
    this.setData({ keyword: e.detail.value });
  },

  onSearch() {
    this.setData({ articles: [], finished: false, initialized: false });
    this.loadArticles(true);
  },

  onClearSearch() {
    this.setData({ keyword: '' });
    this.setData({ articles: [], finished: false, initialized: false });
    this.loadArticles(true);
  },

  goDetail(e) {
    wx.navigateTo({
      url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id
    });
  },

  formatTime: util.formatTime,
  formatViews: util.formatViews
});
