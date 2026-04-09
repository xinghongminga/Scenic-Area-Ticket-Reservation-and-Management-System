function request(path, method = 'GET', data = null) {
  const app = getApp();
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${app.globalData.baseUrl}${path}`,
      method,
      data,
      header: {
        Authorization: app.globalData.token ? `Bearer ${app.globalData.token}` : ''
      },
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

module.exports = { request };
