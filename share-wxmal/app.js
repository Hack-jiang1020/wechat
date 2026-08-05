const THEME_KEY = 'blog_theme';

function systemTheme() {
  // 优先使用新 API，避免弃用警告
  try {
    const base = wx.getAppBaseInfo ? wx.getAppBaseInfo() : null;
    if (base && base.theme) return base.theme === 'dark' ? 'dark' : 'light';
  } catch (e) { /* ignore */ }
  try {
    const info = wx.getSystemInfoSync();
    return info.theme === 'dark' ? 'dark' : 'light';
  } catch (e) {
    return 'light';
  }
}

// 主题优先级：手动设置 > 系统自动检测
function resolveTheme() {
  const manual = wx.getStorageSync(THEME_KEY);
  if (manual === 'light' || manual === 'dark') return manual;
  return systemTheme();
}

App({
  globalData: {
    theme: 'light',
    themeClass: 'app-light'
  },

  onLaunch() {
    this.initTheme();
  },

  initTheme() {
    this.applyTheme(resolveTheme());
    const app = this;
    if (wx.onThemeChange) {
      wx.onThemeChange(function (res) {
        const manual = wx.getStorageSync(THEME_KEY);
        if (manual === 'light' || manual === 'dark') return;
        app.applyTheme(res.theme === 'dark' ? 'dark' : 'light');
      });
    }
  },

  applyTheme(theme) {
    this.globalData.theme = theme;
    this.globalData.themeClass = 'app-' + theme;
    const bg = theme === 'dark' ? '#1e293b' : '#f8fafc';
    if (wx.setBackgroundColor) {
      wx.setBackgroundColor({ backgroundColor: bg, backgroundColorTop: bg, backgroundColorBottom: bg });
    }
    if (wx.setNavigationBarColor) {
      wx.setNavigationBarColor({
        frontColor: theme === 'dark' ? '#ffffff' : '#000000',
        backgroundColor: bg
      });
    }
    const pages = getCurrentPages();
    if (pages.length) {
      const page = pages[pages.length - 1];
      if (page && page.setThemeData) page.setThemeData();
    }
  },

  // mode: 'system' | 'light' | 'dark'
  setTheme(mode) {
    if (mode === 'system') {
      wx.removeStorageSync(THEME_KEY);
    } else if (mode === 'light' || mode === 'dark') {
      wx.setStorageSync(THEME_KEY, mode);
    }
    this.applyTheme(resolveTheme());
  }
});
