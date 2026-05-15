function request(path, method = 'GET', data = null) {
  const app = getApp();
  return new Promise((resolve, reject) => {
    const header = {};
    if (app.globalData.token) {
      header.Authorization = `Bearer ${app.globalData.token}`;
    }
    wx.request({
      url: `${app.globalData.baseUrl}${path}`,
      method,
      data,
      header,
      success(res) {
        const body = res.data || {};
        if (res.statusCode >= 400 || body.success === false) {
          reject(new Error(body.message || `请求失败(${res.statusCode})`));
          return;
        }
        resolve(body.data);
      },
      fail(err) {
        reject(err);
      }
    });
  });
}

function normalizeImageUrl(url) {
  if (!url) return '';
  const app = getApp();
  const baseUrl = app.globalData.baseUrl || '';
  if (url.startsWith('http://localhost:8080/')) {
    return url.replace('http://localhost:8080', baseUrl);
  }
  if (url.startsWith('https://localhost:8080/')) {
    return url.replace('https://localhost:8080', baseUrl);
  }
  if (url.startsWith('/uploads/')) {
    return `${baseUrl}${url}`;
  }
  return url;
}

module.exports = { request, normalizeImageUrl };
