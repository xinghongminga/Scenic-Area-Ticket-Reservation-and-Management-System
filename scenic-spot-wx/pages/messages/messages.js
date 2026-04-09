const { request } = require('../../utils/request');

const TYPE_TEXT = {
  ORDER_PAID: '购票成功',
  ORDER_VERIFIED: '核销提醒',
  AFTERSALE_APPROVED: '售后结果',
  AFTERSALE_REJECTED: '售后结果',
  SYSTEM: '系统通知'
};

Page({
  data: {
    unreadCount: 0,
    loading: false,
    list: []
  },

  onShow() {
    this.loadNotifications();
  },

  async loadNotifications() {
    try {
      this.setData({ loading: true });
      const [list, unread] = await Promise.all([
        request('/api/user/notifications?pageNo=1&pageSize=50'),
        request('/api/user/notifications/unread-count')
      ]);
      this.setData({
        list: (list || []).map(item => ({
          ...item,
          typeText: TYPE_TEXT[item.ntype] || '消息通知'
        })),
        unreadCount: unread?.unreadCount || 0
      });
    } catch (e) {
      wx.showToast({ title: e.message, icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  async markAllRead() {
    try {
      await request('/api/user/notifications/mark-all-read', 'PUT');
      this.setData({
        unreadCount: 0,
        list: (this.data.list || []).map(item => ({ ...item, isRead: 1 }))
      });
      wx.showToast({ title: '已全部标记已读', icon: 'success' });
    } catch (e) {
      wx.showToast({ title: e.message, icon: 'none' });
    }
  },

  async openDetail(e) {
    const id = String(e.currentTarget.dataset.id || '');
    if (!id) return;
    const current = (this.data.list || []).find(item => String(item.id) === id);
    if (current && !current.isRead) {
      try {
        await request(`/api/user/notifications/${id}/read`, 'PUT');
      } catch (_) {
      }
      this.setData({
        unreadCount: Math.max(0, Number(this.data.unreadCount || 0) - 1),
        list: (this.data.list || []).map(item => (String(item.id) === id ? { ...item, isRead: 1 } : item))
      });
    }
    wx.navigateTo({ url: `/pages/notification-detail/notification-detail?id=${id}` });
  }
});