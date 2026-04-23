const { request } = require('../../utils/request');
const { pushFlashNotice } = require('../../utils/notice');

Page({
  data: {
    visitDate: '',
    keyword: '',
    searchKeyword: '',
    defaultTicketImage: 'https://via.placeholder.com/300x300.png?text=Ticket',
    processingTicketId: null,
    tickets: []
  },

  // 将后端票种枚举转换为前端展示文案
  formatTicketType(ticketType) {
    const map = {
      SINGLE: '单人票',
      FAMILY: '家庭票',
      CHILD: '儿童票',
      STUDENT: '学生票',
      SENIOR: '老人票'
    };
    return map[ticketType] || ticketType || '未分类';
  },

  // 统一整理分时段库存，便于列表直接渲染
  formatInventory(rows) {
    const list = rows || [];
    const morning = list.find(r => Number(r.timeslotId) === 1);
    const afternoon = list.find(r => Number(r.timeslotId) === 2);
    const morningRemain = morning ? Number(morning.remainQty || 0) : null;
    const afternoonRemain = afternoon ? Number(afternoon.remainQty || 0) : null;
    const inventoryText = list.length
      ? list.map(r => `${r.timeslotName}: 余${r.remainQty}`).join(' | ')
      : '该日期暂无库存，请更换日期或联系管理员补库存';
    return { morningRemain, afternoonRemain, inventoryText };
  },

  // 更新单个门票的库存展示信息
  updateTicketInventory(id, rows) {
    const formatted = this.formatInventory(rows);
    const tickets = this.data.tickets.map(t => (
      t.id === id
        ? {
            ...t,
            ...formatted
          }
        : t
    ));
    this.setData({ tickets });
  },

  // 拉取单个门票在当前日期的库存
  async refreshTicketInventory(id, showToast = false) {
    const rows = await request(`/api/tickets/${id}/inventory?date=${this.data.visitDate}`);
    this.updateTicketInventory(id, rows || []);
    if (showToast) {
      wx.showToast({ title: rows && rows.length ? '库存已更新' : '暂无库存', icon: 'none' });
    }
    return rows || [];
  },

  // 并行刷新当前列表全部门票库存，单条失败不影响整体可用性
  async refreshAllInventory() {
    const ids = (this.data.tickets || []).map(t => t.id);
    if (!ids.length) return;
    await Promise.all(ids.map(async (id) => {
      try {
        await this.refreshTicketInventory(id, false);
      } catch (e) {
        // Ignore single ticket inventory errors, keep page usable.
      }
    }));
  },

  // 页面展示时初始化日期并加载门票，未登录则先跳转登录
  onShow() {
    if (!wx.getStorageSync('token')) {
      wx.navigateTo({ url: '/pages/login/login' });
      return;
    }
    if (!this.data.visitDate) {
      const d = new Date();
      const m = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      this.setData({ visitDate: `${d.getFullYear()}-${m}-${day}` });
    }
    this.loadTickets();
  },

  // 切换出行日期后重新拉取门票及对应库存
  onDateChange(e) {
    this.setData({ visitDate: e.detail.value });
    this.loadTickets();
  },

  // 记录搜索输入关键字
  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value || '' });
  },

  // 执行关键词搜索
  onSearch() {
    this.setData({ searchKeyword: (this.data.keyword || '').trim() });
    this.loadTickets();
  },

  // 重置搜索条件并刷新
  onResetSearch() {
    this.setData({ keyword: '', searchKeyword: '' });
    this.loadTickets();
  },

  // 拉取门票列表并做展示字段格式化
  async loadTickets() {
    try {
      const query = this.data.searchKeyword
        ? `/api/tickets?scenicId=1&keyword=${encodeURIComponent(this.data.searchKeyword)}`
        : '/api/tickets?scenicId=1';
      const list = await request(query);
      this.setData({
        tickets: (list || []).map(i => ({
          ...i,
          imageUrl: i.imageUrl || '',
          projectNames: i.projectNames || '未关联景区项目',
          qty: 1,
          qtyText: '1',
          ticketTypeText: this.formatTicketType(i.ticketType),
          priceYuanText: (Number(i.priceCent || 0) / 100).toFixed(2),
          slotHint: [i.morningEnabled === 1 ? '上午场' : '', i.afternoonEnabled === 1 ? '下午场' : ''].filter(Boolean).join(' / ') || '未配置场次',
          morningRemain: null,
          afternoonRemain: null,
          inventoryText: ''
        }))
      });
      await this.refreshAllInventory();
    } catch (e) {
      wx.showToast({ title: e.message, icon: 'none' });
    }
  },

  // 处理购票数量输入，保证数量至少为 1
  onQtyInput(e) {
    const id = Number(e.currentTarget.dataset.id);
    const qtyValue = Number(e.detail.value || 1);
    const qty = Number.isNaN(qtyValue) || qtyValue <= 0 ? 1 : qtyValue;
    const tickets = this.data.tickets.map(t => (t.id === id ? { ...t, qty, qtyText: String(qty) } : t));
    this.setData({ tickets });
  },

  // 手动刷新单个门票库存
  async loadInventory(e) {
    const id = Number(e.currentTarget.dataset.id);
    try {
      await this.refreshTicketInventory(id, true);
    } catch (err) {
      wx.showToast({ title: err.message, icon: 'none' });
    }
  },

  // 下单并支付：先选场次创建订单，再根据用户确认决定立即支付或稍后支付
  async createOrder(e) {
    const id = Number(e.currentTarget.dataset.id);
    if (this.data.processingTicketId === id) {
      return;
    }

    this.setData({ processingTicketId: id });

    const item = this.data.tickets.find(t => t.id === id);
    if (!item) {
      this.setData({ processingTicketId: null });
      return;
    }
    try {
      const inv = await request(`/api/tickets/${id}/inventory?date=${this.data.visitDate}`);
      if (!inv || !inv.length) throw new Error('没有可用时段库存');
      const available = inv.filter(s => Number(s.remainQty || 0) > 0);
      if (!available.length) throw new Error('该日期所有场次已售罄');
      const labels = available.map(s => `${s.timeslotName}（余票${s.remainQty}）`);

      // 第一步：让用户先选择购买场次
      wx.showActionSheet({
        itemList: labels,
        success: async (sel) => {
          const chosen = available[sel.tapIndex];
          if (!chosen) {
            this.setData({ processingTicketId: null });
            return;
          }
          try {
            // 第二步：创建订单（状态通常为待支付）
            const order = await request('/api/orders', 'POST', {
              ticketId: id,
              visitDate: this.data.visitDate,
              timeslotId: chosen.timeslotId,
              qty: item.qty || 1
            });
            const amount = (Number(order.totalAmountCent || 0) / 100).toFixed(2);

            // 第三步：确认是否立即支付
            wx.showModal({
              title: '确认支付',
              content: `场次：${chosen.timeslotName}\n本次支付金额 ¥${amount}，是否确认支付？`,
              confirmText: '确认支付',
              cancelText: '稍后支付',
              success: async (res) => {
                if (res.confirm) {
                  try {
                    wx.showLoading({ title: '正在支付...' });

                    // 第四步：发起支付，成功后写入闪存通知并跳转我的页面
                    const payResult = await request(`/api/orders/${order.orderNo}/pay`, 'POST');
                    await this.refreshTicketInventory(id, false);
                    pushFlashNotice({
                      notificationId: payResult?.notificationId || '',
                      title: payResult?.notificationTitle || '购票成功',
                      content: payResult?.notificationContent || `订单 ${order.orderNo} 支付成功，请前往消息中心查看。`
                    });
                    wx.switchTab({ url: '/pages/mine/mine' });
                  } catch (payErr) {
                    wx.showToast({ title: payErr.message, icon: 'none' });
                  } finally {
                    wx.hideLoading();
                    this.setData({ processingTicketId: null });
                  }
                  return;
                }

                // 用户选择稍后支付：保留待支付订单并提示
                wx.showToast({ title: '订单已创建，待支付', icon: 'none' });
                await this.refreshTicketInventory(id, false);
                this.setData({ processingTicketId: null });
                wx.switchTab({ url: '/pages/mine/mine' });
              }
            });
          } catch (createErr) {
            wx.showToast({ title: createErr.message, icon: 'none' });
            this.setData({ processingTicketId: null });
          }
        },
        fail: () => {
          this.setData({ processingTicketId: null });
        }
      });
    } catch (err) {
      wx.showToast({ title: err.message, icon: 'none' });
      this.setData({ processingTicketId: null });
    }
  }
});
