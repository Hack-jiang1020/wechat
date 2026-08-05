const config = require('../config.js');

function request(url, method, data) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token') || '';
    wx.request({
      url: config.baseUrl + url,
      method: method || 'GET',
      data: data || {},
      header: {
        'Content-Type': 'application/json',
        'token': token
      },
      success(res) {
        if (res.data && res.data.code === 200) {
          resolve(res.data.data);
        } else {
          const msg = (res.data && res.data.msg) || '请求失败';
          if (res.data && res.data.code === 401) {
            wx.removeStorageSync('token');
            wx.removeStorageSync('userInfo');
          }
          wx.showToast({ title: msg, icon: 'none' });
          reject(new Error(msg));
        }
      },
      fail(err) {
        wx.showToast({ title: '网络异常，请检查后端服务是否启动', icon: 'none' });
        reject(err);
      }
    });
  });
}

function absUrl(path) {
  if (!path) return '';
  if (/^https?:\/\//.test(path)) return path;
  if (path.indexOf('wxfile://') === 0 || path.indexOf('http://tmp') === 0) return path;
  return config.baseUrl + (path.charAt(0) === '/' ? path : '/' + path);
}

function uploadAvatar(filePath) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token') || '';
    wx.uploadFile({
      url: config.baseUrl + '/api/user/avatar',
      filePath: filePath,
      name: 'file',
      header: { 'token': token },
      success(res) {
        try {
          const json = JSON.parse(res.data);
          if (json.code === 200) resolve(json.data);
          else reject(new Error(json.msg || '上传失败'));
        } catch (e) { reject(new Error('上传响应异常')); }
      },
      fail: reject
    });
  });
}

function uploadFile(filePath) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token') || '';
    wx.uploadFile({
      url: config.baseUrl + '/admin/api/upload',
      filePath: filePath,
      name: 'file',
      header: { 'token': token },
      success(res) {
        try {
          const json = JSON.parse(res.data);
          if (json.code === 200) {
            resolve(json.data);
          } else {
            reject(new Error(json.msg || '上传失败'));
          }
        } catch (e) {
          reject(new Error('上传响应异常'));
        }
      },
      fail: reject
    });
  });
}

module.exports = {
  request: request,
  get: (url) => request(url, 'GET'),
  post: (url, data) => request(url, 'POST', data),
  del: (url) => request(url, 'DELETE'),
  uploadFile: uploadFile,
  uploadAvatar: uploadAvatar,
  absUrl: absUrl,
  baseUrl: config.baseUrl
};
