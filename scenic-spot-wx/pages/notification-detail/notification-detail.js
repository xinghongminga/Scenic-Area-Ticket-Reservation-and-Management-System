const { request } = require('../../utils/request');

const TYPE_TEXT = {
  ORDER_PAID: '购票成功通知',
  ORDER_VERIFIED: '核销通知',
  AFTERSALE_APPROVED: '售后通过通知',
  AFTERSALE_REJECTED: '售后拒绝通知',
  SYSTEM: '系统通知'
};

Page({
  data: {
    detail: null,
    typeText: ''
  },

  onLoad(options) {
    if (!options.id) {
      wx.showToast({ title: '通知不存在', icon: 'none' });
      return;
    }
    this.loadDetail(options.id);
  },

  async loadDetail(id) {
    try {
      const detail = await request(`/api/user/notifications/${id}`);
      this.setData({
        detail,
        typeText: TYPE_TEXT[detail?.ntype] || '消息通知'
      });
      if (detail && !detail.isRead) {
        await request(`/api/user/notifications/${id}/read`, 'PUT');
      }
    } catch (e) {
      wx.showToast({ title: e.message, icon: 'none' });
    }
  }
});