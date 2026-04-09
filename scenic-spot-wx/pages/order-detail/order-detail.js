const { request } = require('../../utils/request');

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

const TICKET_STATUS_TEXT = {
  UNUSED: '未使用',
  USED: '已使用',
  REFUNDED: '已退款'
};

Page({
  data: {
    orderNo: '',
    loading: false,
    detail: null,
    itemList: [],
    ticketList: []
  },
  async loadQrImages(ticketList) {
    const app = getApp();
    const token = app.globalData.token || '';
    const orderNo = this.data.orderNo;
    const updated = await Promise.all((ticketList || []).map((ticket) => new Promise((resolve) => {
      if (!ticket.verifyCode) {
        resolve(ticket);
        return;
      }
      wx.downloadFile({
        url: `${app.globalData.baseUrl}/api/orders/${orderNo}/tickets/${encodeURIComponent(ticket.verifyCode)}/qr-image`,
        header: {
          Authorization: token ? `Bearer ${token}` : ''
        },
        success: (res) => {
          if (res.statusCode >= 200 && res.statusCode < 300 && res.tempFilePath) {
            resolve({ ...ticket, qrImageUrl: res.tempFilePath });
            return;
          }
          resolve(ticket);
        },
        fail: () => resolve(ticket)
      });
    })));
    this.setData({ ticketList: updated });
  },
  onLoad(options) {
    const orderNo = options.orderNo || '';
    this.setData({ orderNo });
    if (orderNo) {
      this.loadDetail();
    }
  },
  async loadDetail() {
    if (!this.data.orderNo) return;
    this.setData({ loading: true });
    try {
      const detail = await request(`/api/orders/${this.data.orderNo}`);
      const order = detail?.order || null;
      const orderView = order
        ? {
            ...order,
            statusText: ORDER_STATUS_TEXT[order.status] || order.status,
            totalAmountYuanText: (Number(order.totalAmountCent || 0) / 100).toFixed(2)
          }
        : null;
      const itemList = (detail?.items || []).map(i => ({
        ...i,
        unitPriceYuanText: (Number(i.unitPriceCent || 0) / 100).toFixed(2),
        amountYuanText: (Number(i.amountCent || 0) / 100).toFixed(2)
      }));
      const ticketList = (detail?.tickets || []).map(t => ({
        ...t,
        statusText: TICKET_STATUS_TEXT[t.status] || t.status
      }));
      this.setData({ detail: orderView, itemList, ticketList });
      if (ticketList.length) {
        await this.loadQrImages(ticketList);
      }
    } catch (err) {
      wx.showToast({ title: err.message, icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  }
});
