const { request, normalizeImageUrl } = require('../../utils/request');
const { hideTopNotice, pushFlashNotice, showTopNotice } = require('../../utils/notice');

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

Page({
  data: {
    defaultTicketImage: 'https://via.placeholder.com/240x240.png?text=Ticket',
    noticeBar: {
      visible: false,
      title: '',
      content: '',
      notificationId: ''
    },
    tabs: [
      { key: 'ALL', label: '全部' },
      { key: 'UNPAID', label: '待支付' },
      { key: 'PAID', label: '已支付' },
      { key: 'USED', label: '已核销' },
      { key: 'REFUNDED', label: '已退款' },
      { key: 'RESCHEDULED', label: '已改签' },
      { key: 'REFUNDING', label: '退款中' },
      { key: 'RESCHEDULING', label: '改签中' }
    ],
    activeTab: 'ALL',
    processingOrderNo: '',
    allOrders: [],
    list: []
  },
  onLoad() {
    this.applyFilter();
  },
  onShow() {
    this.load();
  },
  onHide() {
    hideTopNotice(this, false);
  },
  onUnload() {
    hideTopNotice(this, false);
  },
  async load() {
    try {
      const list = await request('/api/orders/my');
      this.setData({ allOrders: list || [] });
      this.applyFilter();
      wx.showToast({ title: '已刷新', icon: 'none' });
    } catch (e) {
      wx.showToast({ title: e.message, icon: 'none' });
    }
  },
  onRefreshTap() {
    this.load();
  },
  onTabTap(e) {
    this.setData({ activeTab: e.currentTarget.dataset.key });
    this.applyFilter();
  },
  applyFilter() {
    const key = this.data.activeTab;
    const list = (this.data.allOrders || [])
      .filter(item => key === 'ALL' || item.status === key)
      .map(item => ({
        ...item,
        amountYuanText: (Number(item.totalAmountCent || 0) / 100).toFixed(2),
        statusText: ORDER_STATUS_TEXT[item.status] || item.status,
        ticketImageUrl: normalizeImageUrl(item.ticketImageUrl || ''),
        ticketName: item.ticketName || '门票'
      }));
    this.setData({ list });
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
      this.load();
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
  goDetail(e) {
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
          this.load();
        } catch (err) {
          wx.showToast({ title: err.message, icon: 'none' });
        }
      }
    });
  },
  goAftersale(e) {
    const orderNo = e.currentTarget.dataset.orderno;
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
  }
});
