const api = require('../../utils/request.js');

Page({
  data: {
    articleTotal: 0,
    themeMode: 'system'
  },

  onShow() {
    this.setThemeData();
    this.loadStat();
  },

  loadStat() {
    api.get('/api/article/list?page=1&size=1').then((data) => {
      this.setData({ articleTotal: data.total || 0 });
    }).catch(() => {});
  },

  setThemeData() {
    const manual = wx.getStorageSync('blog_theme');
    this.setData({
      themeClass: getApp().globalData.themeClass,
      themeMode: (manual === 'light' || manual === 'dark') ? manual : 'system'
    });
  },

  selectTheme(e) {
    getApp().setTheme(e.currentTarget.dataset.mode);
    this.setThemeData();
  }
});
