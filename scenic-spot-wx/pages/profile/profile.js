const { request, normalizeImageUrl } = require('../../utils/request');
const app = getApp();

Page({
  data: {
    avatarUrl: '',
    avatarFilePath: '',
    nickname: '',
    phone: '',
    oldPassword: '',
    newPassword: '',
    passwordModalVisible: false
  },

  onShow() {
    const user = wx.getStorageSync('user') || {};
    this.setData({
      avatarUrl: normalizeImageUrl(user.avatarUrl || ''),
      avatarFilePath: '',
      nickname: user.nickname || '',
      phone: user.phone || '',
      oldPassword: '',
      newPassword: '',
      passwordModalVisible: false
    });
  },

  chooseAvatar() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const file = res.tempFiles && res.tempFiles[0];
        if (!file) return;
        this.setData({ avatarUrl: file.tempFilePath, avatarFilePath: file.tempFilePath });
      }
    });
  },

  onNicknameInput(e) {
    this.setData({ nickname: e.detail.value });
  },

  onPhoneInput(e) {
    this.setData({ phone: e.detail.value });
  },

  onOldPasswordInput(e) {
    this.setData({ oldPassword: e.detail.value });
  },

  onNewPasswordInput(e) {
    this.setData({ newPassword: e.detail.value });
  },

  uploadAvatarIfNeeded() {
    if (!this.data.avatarFilePath) {
      return Promise.resolve(this.data.avatarUrl || '');
    }
    const app = getApp();
    return new Promise((resolve, reject) => {
      const header = {};
      if (app.globalData.token) {
        header.Authorization = `Bearer ${app.globalData.token}`;
      }
      wx.uploadFile({
        url: `${app.globalData.baseUrl}/api/user/avatar/upload`,
        filePath: this.data.avatarFilePath,
        name: 'file',
        header,
        success: (res) => {
          try {
            const body = JSON.parse(res.data || '{}');
            if (res.statusCode >= 400 || body.success === false) {
              reject(new Error(body.message || `上传失败(${res.statusCode})`));
              return;
            }
            const url = body.data && body.data.url ? body.data.url : '';
            resolve(url);
          } catch (err) {
            reject(err);
          }
        },
        fail: (err) => reject(err)
      });
    });
  },

  async saveProfile() {
    try {
      if (!this.data.nickname) {
        throw new Error('昵称不能为空');
      }
      const avatarUrl = await this.uploadAvatarIfNeeded();
      const payload = {
        nickname: this.data.nickname,
        avatarUrl,
        phone: this.data.phone
      };
      await request('/api/user/profile', 'PUT', payload);
      const user = wx.getStorageSync('user') || {};
      const nextUser = {
        ...user,
        nickname: this.data.nickname,
        avatarUrl,
        phone: this.data.phone
      };
      this.setData({ avatarUrl, avatarFilePath: '' });
      app.globalData.user = nextUser;
      wx.setStorageSync('user', nextUser);
      wx.showToast({ title: '保存成功', icon: 'success' });
    } catch (e) {
      wx.showToast({ title: e.message, icon: 'none' });
    }
  },

  openPasswordModal() {
    this.setData({ passwordModalVisible: true, oldPassword: '', newPassword: '' });
  },

  closePasswordModal() {
    this.setData({ passwordModalVisible: false, oldPassword: '', newPassword: '' });
  },

  noop() {},

  async changePassword() {
    try {
      if (!this.data.oldPassword || !this.data.newPassword) {
        throw new Error('请输入原密码和新密码');
      }
      await request('/api/user/password', 'PUT', {
        oldPassword: this.data.oldPassword,
        newPassword: this.data.newPassword
      });
      this.setData({ oldPassword: '', newPassword: '', passwordModalVisible: false });
      wx.showToast({ title: '密码修改成功', icon: 'success' });
    } catch (e) {
      wx.showToast({ title: e.message, icon: 'none' });
    }
  },

  logout() {
    app.globalData.token = '';
    app.globalData.user = null;
    wx.removeStorageSync('token');
    wx.removeStorageSync('user');
    wx.reLaunch({ url: '/pages/login/login' });
  }
});
