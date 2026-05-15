const { request, normalizeImageUrl } = require('../../utils/request');
const { consumeFlashNotice, hideTopNotice, pushFlashNotice, showTopNotice } = require('../../utils/notice');

const ORDER_STATUS_TEXT = {
  UNPAID: '待支付',
  PAID: '已支付',
  USED: '已核销',
  REFUNDED: '已退款',
  RESCHEDULED: '已改签',
  REFUNDING: '退款中',
  RESCHEDULING: '改签中',
  DELETED: '已删除'
};

const TYPE_TEXT = {
  REFUND: '退款',
  RESCHEDULE: '改签'
};

const AFTERSALE_STATUS_TEXT = {
  SUBMITTED: '处理中',
  DONE: '已完成',
  REJECTED: '已拒绝'
};

const ROLE_TEXT = {
  ADMIN: '管理员',
  ANALYST: '分析员',
  AUDITOR: '审核员',
  TOURIST: '游客',
  STAFF: '工作人员',
  USER: '普通用户'
};

Page({
  data: {
    defaultTicketImage: 'https://via.placeholder.com/240x240.png?text=Ticket',
    profile: {
      nickname: '游客',
      role: 'TOURIST',
      roleText: '游客',
      phone: '',
      avatarUrl: ''
    },
    noticeBar: {
      visible: false,
      title: '',
      content: '',
      notificationId: ''
    },
    section: 'orders',
    orderTabs: [
      { key: 'ALL', label: '全部' },
      { key: 'UNPAID', label: '待支付' },
      { key: 'PAID', label: '已支付' },
      { key: 'USED', label: '已核销' },
      { key: 'REFUNDED', label: '已退款' }
    ],
    activeOrderTab: 'ALL',
    processingOrderNo: '',
    allOrders: [],
    orders: [],
    aftersaleStatusTabs: [
      { key: 'ALL', label: '全部' },
      { key: 'SUBMITTED', label: '处理中' },
      { key: 'DONE', label: '已完成' },
      { key: 'REJECTED', label: '已拒绝' }
    ],
    activeAftersaleStatus: 'ALL',
    allAftersales: [],
    aftersales: []
  },

  onShow() {
    if (!wx.getStorageSync('token')) {
      wx.navigateTo({ url: '/pages/login/login' });
      return;
    }
    this.loadProfile();
    this.loadOrders();
    this.loadAftersales();
    const flashNotice = consumeFlashNotice();
    if (flashNotice) {
      showTopNotice(this, flashNotice);
    }
  },

  onHide() {
    hideTopNotice(this, false);
  },

  onUnload() {
    hideTopNotice(this, false);
  },

  async loadProfile() {
    try {
      const me = await request('/api/auth/me');
      const cached = wx.getStorageSync('user') || {};
      this.setData({
        profile: {
          nickname: me?.nickname || cached.nickname || '游客',
          role: me?.role || cached.role || 'TOURIST',
          roleText: ROLE_TEXT[me?.role || cached.role || 'TOURIST'] || (me?.role || cached.role || 'TOURIST'),
          phone: cached.phone || '',
          avatarUrl: normalizeImageUrl(cached.avatarUrl || '')
        }
      });
    } catch (e) {
      const cached = wx.getStorageSync('user') || {};
      this.setData({
        profile: {
          nickname: cached.nickname || '游客',
          role: cached.role || 'TOURIST',
          roleText: ROLE_TEXT[cached.role || 'TOURIST'] || (cached.role || 'TOURIST'),
          phone: cached.phone || '',
          avatarUrl: normalizeImageUrl(cached.avatarUrl || '')
        }
      });
    }
  },

  goProfile() {
    wx.navigateTo({ url: '/pages/profile/profile' });
  },

  onSectionChange(e) {
    this.setData({ section: e.currentTarget.dataset.section });
  },

  async loadOrders() {
    try {
      const list = await request('/api/orders/my');
      this.setData({ allOrders: list || [] });
      this.applyOrderFilter();
    } catch (e) {
      wx.showToast({ title: e.message, icon: 'none' });
    }
  },

  onOrderTabTap(e) {
    this.setData({ activeOrderTab: e.currentTarget.dataset.key });
    this.applyOrderFilter();
  },

  applyOrderFilter() {
    const key = this.data.activeOrderTab;
    const list = (this.data.allOrders || [])
      .filter(item => key === 'ALL' || item.status === key)
      .map(item => ({
        ...item,
        amountYuanText: (Number(item.totalAmountCent || 0) / 100).toFixed(2),
        statusText: ORDER_STATUS_TEXT[item.status] || item.status,
        ticketName: item.ticketName || '门票',
        ticketImageUrl: normalizeImageUrl(item.ticketImageUrl || '')
      }));
    this.setData({ orders: list });
  },

  async payNow(e) {
    const orderNo = e.currentTarget.dataset.orderno;
    try {
      this.setData({ processingOrderNo: orderNo });
      wx.showLoading({ title: '正在支付...' });
      const payResult = await request(`/api/orders/${orderNo}/pay`, 'POST');
      const notice = {
        notificationId: payResult?.notificationId || '',
        title: payResult?.notificationTitle || '购票成功',
        content: payResult?.notificationContent || `订单 ${orderNo} 支付成功，请前往消息中心查看。`
      };
      pushFlashNotice(notice);
      showTopNotice(this, notice);
      this.loadOrders();
    } catch (err) {
      wx.showToast({ title: err.message, icon: 'none' });
    } finally {
      wx.hideLoading();
      this.setData({ processingOrderNo: '' });
    }
  },

  closeNoticeBar() {
    hideTopNotice(this);
  },

  goOrderDetail(e) {
    const orderNo = e.currentTarget.dataset.orderno;
    if (!orderNo) return;
    wx.navigateTo({ url: `/pages/order-detail/order-detail?orderNo=${orderNo}` });
  },

  async deleteOrder(e) {
    const orderNo = e.currentTarget.dataset.orderno;
    if (!orderNo) return;
    wx.showModal({
      title: '删除订单',
      content: '删除后将不在订单列表显示，是否继续？',
      confirmColor: '#dc2626',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          await request(`/api/orders/${orderNo}`, 'DELETE');
          wx.showToast({ title: '已删除', icon: 'success' });
          this.loadOrders();
        } catch (err) {
          wx.showToast({ title: err.message, icon: 'none' });
        }
      }
    });
  },

  applyAftersale(e) {
    const orderNo = e.currentTarget.dataset.orderno;
    if (!orderNo) return;
    const typeList = ['申请退款', '申请改签'];
    wx.showActionSheet({
      itemList: typeList,
      success: (res) => {
        const reqType = res.tapIndex === 1 ? 'RESCHEDULE' : 'REFUND';
        wx.navigateTo({
          url: `/pages/aftersale/aftersale?orderNo=${encodeURIComponent(orderNo)}&type=${reqType}`
        });
      }
    });
  },

  onAftersaleStatusTabTap(e) {
    this.setData({ activeAftersaleStatus: e.currentTarget.dataset.key });
    this.applyAftersaleFilter();
  },

  async loadAftersales() {
    try {
      const list = await request('/api/aftersale/my');
      this.setData({ allAftersales: list || [] });
      this.applyAftersaleFilter();
    } catch (e) {
      wx.showToast({ title: e.message, icon: 'none' });
    }
  },

  applyAftersaleFilter() {
    const key = this.data.activeAftersaleStatus;
    const list = (this.data.allAftersales || []).filter(item => key === 'ALL' || item.status === key);
    this.setData({
      aftersales: list.map(item => ({
        ...item,
        reqTypeText: TYPE_TEXT[item.reqType] || item.reqType,
        statusText: AFTERSALE_STATUS_TEXT[item.status] || item.status
      }))
    });
  }
});
