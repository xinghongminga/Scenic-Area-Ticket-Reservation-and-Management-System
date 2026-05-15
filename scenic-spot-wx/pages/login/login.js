const { request } = require('../../utils/request');
const app = getApp();

// 基本校验正则（非完全实名验证，但覆盖常见格式）
const PHONE_RE = /^\s*1\d{10}\s*$/;
const IDCARD_RE = /^(?:\d{15}|\d{17}[\dXx])$/;

Page({
  oauthLock: false,
  data: {
    mode: 'login',
    loginMethod: 'password',
    registerStep: 1,
    phone: '13800000001',
    nickname: '游客小程序',
    fullName: '',
    idCardNo: '',
    password: '',
    code: '',
    mockCode: '',
    oauthLoading: false
  },
  switchMode(e) {
    const mode = e.currentTarget.dataset.mode;
    this.setData({ mode, registerStep: mode === 'register' ? 1 : this.data.registerStep });
  },
  switchLoginMethod(e) {
    this.setData({ loginMethod: e.currentTarget.dataset.method });
  },
  toggleLoginMethod() {
    this.setData({ loginMethod: this.data.loginMethod === 'password' ? 'code' : 'password' });
  },
  nextRegisterStep() {
    if (!this.data.phone) {
      wx.showToast({ title: '请输入手机号', icon: 'none' });
      return;
    }
    if (!PHONE_RE.test(this.data.phone)) {
      wx.showToast({ title: '请输入有效手机号', icon: 'none' });
      return;
    }
    if (!this.data.code) {
      wx.showToast({ title: '请输入验证码', icon: 'none' });
      return;
    }
    this.setData({ registerStep: 2 });
  },
  prevRegisterStep() {
    this.setData({ registerStep: 1 });
  },
  onPhoneInput(e) {
    this.setData({ phone: e.detail.value });
  },
  onNicknameInput(e) {
    this.setData({ nickname: e.detail.value });
  },
  onFullNameInput(e) {
    this.setData({ fullName: e.detail.value });
  },
  onIdCardNoInput(e) {
    this.setData({ idCardNo: e.detail.value });
  },
  onPasswordInput(e) {
    this.setData({ password: e.detail.value });
  },
  onCodeInput(e) {
    this.setData({ code: e.detail.value });
  },
  async sendCode() {
    try {
      const data = await request('/api/auth/sendCode', 'POST', { phone: this.data.phone });
      this.setData({ mockCode: data.mockCode, code: data.mockCode });
    } catch (e) {
      wx.showToast({ title: e.message, icon: 'none' });
    }
  },
  async submit() {
    try {
      if (!this.data.phone) {
        throw new Error('请输入手机号');
      }
      if (this.data.mode === 'register' && this.data.registerStep !== 2) {
        throw new Error('请先完成手机号验证');
      }
      if (this.data.mode === 'register' && !this.data.nickname) {
        throw new Error('请输入昵称');
      }
      if (this.data.mode === 'register' && !this.data.fullName) {
        throw new Error('请输入姓名');
      }
      if (this.data.mode === 'register' && !this.data.idCardNo) {
        throw new Error('请输入身份证号');
      }
      if (this.data.mode === 'register' && !PHONE_RE.test(this.data.phone)) {
        throw new Error('请输入有效手机号');
      }
      if (this.data.mode === 'register' && !IDCARD_RE.test(this.data.idCardNo)) {
        throw new Error('请输入有效身份证号');
      }
      if (this.data.mode === 'register' && !this.data.password) {
        throw new Error('注册请设置密码');
      }
      if (this.data.mode === 'register' || (this.data.mode === 'login' && this.data.loginMethod === 'code')) {
        if (!this.data.code) {
          throw new Error('请输入验证码');
        }
      }
      if (this.data.mode === 'login' && this.data.loginMethod === 'password' && !this.data.password) {
        throw new Error('请输入密码');
      }

      let data;
      if (this.data.mode === 'register') {
        data = await request('/api/auth/registerByCode', 'POST', {
          phone: this.data.phone,
          code: this.data.code,
          nickname: this.data.nickname,
          fullName: this.data.fullName,
          idCardNo: this.data.idCardNo,
          username: this.data.phone,
          password: this.data.password
        });
      } else if (this.data.loginMethod === 'password') {
        data = await request('/api/auth/loginByPassword', 'POST', {
          username: this.data.phone,
          password: this.data.password
        });
      } else {
        data = await request('/api/auth/loginByCode', 'POST', {
          phone: this.data.phone,
          code: this.data.code,
          nickname: this.data.nickname
        });
      }
      this.handleLoginSuccess(data, {
        phone: this.data.phone,
        fullName: this.data.fullName,
        idCardNo: this.data.idCardNo
      });
    } catch (e) {
      wx.showToast({ title: e.message, icon: 'none' });
    }
  },
  async submitWechatThirdPartyLogin() {
    if (this.oauthLock || this.data.oauthLoading) {
      return;
    }
    this.oauthLock = true;
    this.setData({ oauthLoading: true });
    wx.showLoading({ title: '登录中', mask: true });
    let navigated = false;
    try {
      const profile = await this.getWechatProfile();
      const loginRes = await new Promise((resolve, reject) => {
        wx.login({
          success: resolve,
          fail: reject
        });
      });
      if (!loginRes || !loginRes.code) {
        throw new Error('微信登录凭证获取失败');
      }

      const data = await request('/api/auth/loginByWechatMini', 'POST', {
        code: loginRes.code,
        nickname: profile.nickName || this.data.nickname || '微信用户',
        avatarUrl: profile.avatarUrl || '',
        devOpenId: this.getOrCreateDevOpenId()
      });
      navigated = true;
      this.handleLoginSuccess(data, {
        nickname: data.nickname || profile.nickName || this.data.nickname || '微信用户',
        avatarUrl: data.avatarUrl || profile.avatarUrl || ''
      });
    } catch (e) {
      wx.showToast({ title: e.message, icon: 'none' });
    } finally {
      if (!navigated) {
        this.setData({ oauthLoading: false });
      }
      wx.hideLoading();
      this.oauthLock = false;
    }
  },
  async getWechatProfile() {
    try {
      const profileRes = await new Promise((resolve, reject) => {
        wx.getUserProfile({
          desc: '用于完善账号资料',
          success: resolve,
          fail: reject
        });
      });
      return profileRes && profileRes.userInfo ? profileRes.userInfo : {};
    } catch (e) {
      return {};
    }
  },
  getOrCreateDevOpenId() {
    const key = 'wxThirdPartyOpenIdDev';
    let openId = wx.getStorageSync(key) || '';
    if (!openId) {
      openId = `LOCAL_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`;
      wx.setStorageSync(key, openId);
    }
    return openId;
  },
  handleLoginSuccess(data, extraUser = {}) {
    app.globalData.token = data.token;
    app.globalData.user = { ...data, ...extraUser };
    wx.setStorageSync('token', data.token);
    wx.setStorageSync('user', { ...data, ...extraUser });
    wx.switchTab({
      url: '/pages/tickets/tickets',
      fail: () => {
        wx.reLaunch({ url: '/pages/tickets/tickets' });
      },
      complete: () => {
        if (this.data.oauthLoading) {
          this.setData({ oauthLoading: false });
        }
        wx.hideLoading();
      }
    });
  }
});
