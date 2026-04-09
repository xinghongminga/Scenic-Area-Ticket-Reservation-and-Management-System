const { request } = require('../../utils/request');
const app = getApp();

Page({
  data: {
    avatarUrl: '',
    nickname: '',
    phone: '',
    oldPassword: '',
    newPassword: ''
  },

  onShow() {
    const user = wx.getStorageSync('user') || {};
    this.setData({
      avatarUrl: user.avatarUrl || '',
      nickname: user.nickname || '',
      phone: user.phone || '',
      oldPassword: '',
      newPassword: ''
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
        this.setData({ avatarUrl: file.tempFilePath });
      }
    });
  },

  onNicknameInput(e) {
    this.setData({ nickname: e.detail.value });
  },

  onOldPasswordInput(e) {
    this.setData({ oldPassword: e.detail.value });
  },

  onNewPasswordInput(e) {
    this.setData({ newPassword: e.detail.value });
  },

  async saveProfile() {
    try {
      if (!this.data.nickname) {
        throw new Error('昵称不能为空');
      }
      await request('/api/user/profile', 'PUT', { nickname: this.data.nickname });
      const user = wx.getStorageSync('user') || {};
      const nextUser = {
        ...user,
        nickname: this.data.nickname,
        avatarUrl: this.data.avatarUrl
      };
      app.globalData.user = nextUser;
      wx.setStorageSync('user', nextUser);
      wx.showToast({ title: '保存成功', icon: 'success' });
    } catch (e) {
      wx.showToast({ title: e.message, icon: 'none' });
    }
  },

  async changePassword() {
    try {
      if (!this.data.oldPassword || !this.data.newPassword) {
        throw new Error('请输入原密码和新密码');
      }
      await request('/api/user/password', 'PUT', {
        oldPassword: this.data.oldPassword,
        newPassword: this.data.newPassword
      });
      this.setData({ oldPassword: '', newPassword: '' });
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
