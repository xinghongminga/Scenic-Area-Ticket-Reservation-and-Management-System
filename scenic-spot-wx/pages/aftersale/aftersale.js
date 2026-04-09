const { request } = require('../../utils/request');

const TYPE_TEXT = {
  REFUND: '退款',
  RESCHEDULE: '改签'
};

const STATUS_TEXT = {
  SUBMITTED: '处理中',
  DONE: '已完成',
  REJECTED: '已拒绝'
};

Page({
  data: {
    orderNo: '',
    reason: '',
    types: [
      { key: 'REFUND', label: '退款' },
      { key: 'RESCHEDULE', label: '改签' }
    ],
    typeIndex: 0,
    loadingReschedule: false,
    rescheduleOptions: [],
    availableDateRange: [],
    targetDateIndex: 0,
    targetVisitDate: '',
    targetTimeslotRange: [],
    targetTimeslotIds: [],
    targetTimeslotIndex: 0,
    statusTabs: [
      { key: 'ALL', label: '全部' },
      { key: 'SUBMITTED', label: '处理中' },
      { key: 'DONE', label: '已完成' },
      { key: 'REJECTED', label: '已拒绝' }
    ],
    activeStatus: 'ALL',
    allList: [],
    list: []
  },
  onLoad(options) {
    if (options && options.orderNo) {
      this.setData({ orderNo: options.orderNo });
    }
  },
  onShow() {
    this.load();
  },
  getReqType() {
    const current = this.data.types[this.data.typeIndex];
    return current ? current.key : 'REFUND';
  },
  resetRescheduleForm() {
    this.setData({
      rescheduleOptions: [],
      availableDateRange: [],
      targetDateIndex: 0,
      targetVisitDate: '',
      targetTimeslotRange: [],
      targetTimeslotIds: [],
      targetTimeslotIndex: 0
    });
  },
  syncTimeslotsByDateIndex(index) {
    const option = (this.data.rescheduleOptions || [])[index];
    const timeslots = option && option.timeslots ? option.timeslots : [];
    this.setData({
      targetDateIndex: index,
      targetVisitDate: option ? option.date : '',
      targetTimeslotRange: timeslots.map(t => `${t.timeslotName}(余票${t.remainQty})`),
      targetTimeslotIds: timeslots.map(t => t.timeslotId),
      targetTimeslotIndex: 0
    });
  },
  onOrderNoInput(e) {
    this.setData({ orderNo: e.detail.value });
    if (this.getReqType() === 'RESCHEDULE') {
      this.resetRescheduleForm();
    }
  },
  onReasonInput(e) {
    this.setData({ reason: e.detail.value });
  },
  onTypeChange(e) {
    const nextIndex = Number(e.detail.value);
    this.setData({ typeIndex: nextIndex });
    if ((this.data.types[nextIndex] || {}).key === 'RESCHEDULE') {
      if (this.data.orderNo) {
        this.loadRescheduleOptions();
      } else {
        this.resetRescheduleForm();
      }
      return;
    }
    this.resetRescheduleForm();
  },
  onDateChange(e) {
    this.syncTimeslotsByDateIndex(Number(e.detail.value));
  },
  onTimeslotChange(e) {
    this.setData({ targetTimeslotIndex: Number(e.detail.value) });
  },
  async loadRescheduleOptions() {
    try {
      const orderNo = (this.data.orderNo || '').trim();
      if (!orderNo) {
        throw new Error('请先输入订单号');
      }
      this.setData({ loadingReschedule: true });
      const options = await request(`/api/aftersale/reschedule/options?orderNo=${encodeURIComponent(orderNo)}`);
      const list = options || [];
      if (!list.length) {
        this.resetRescheduleForm();
        wx.showToast({ title: '暂无可改签余票日期', icon: 'none' });
        return;
      }
      this.setData({
        rescheduleOptions: list,
        availableDateRange: list.map(item => item.date),
        targetDateIndex: 0
      });
      this.syncTimeslotsByDateIndex(0);
    } catch (e) {
      wx.showToast({ title: e.message, icon: 'none' });
    } finally {
      this.setData({ loadingReschedule: false });
    }
  },
  onStatusTabTap(e) {
    this.setData({ activeStatus: e.currentTarget.dataset.key });
    this.applyFilter();
  },
  async submit() {
    try {
      const reqType = this.getReqType();
      const payload = {
        orderNo: this.data.orderNo,
        reqType,
        reason: this.data.reason
      };
      if (reqType === 'RESCHEDULE') {
        if (!this.data.targetVisitDate) {
          throw new Error('改签请先选择目标日期');
        }
        const targetTimeslotId = this.data.targetTimeslotIds[this.data.targetTimeslotIndex];
        if (!targetTimeslotId) {
          throw new Error('改签请先选择可用时段');
        }
        payload.targetVisitDate = this.data.targetVisitDate;
        payload.targetTimeslotId = targetTimeslotId;
      }
      await request('/api/aftersale', 'POST', payload);
      wx.showToast({ title: '提交成功', icon: 'success' });
      this.resetRescheduleForm();
      this.setData({ reason: '' });
      this.load();
    } catch (e) {
      wx.showToast({ title: e.message, icon: 'none' });
    }
  },
  async load() {
    try {
      const list = await request('/api/aftersale/my');
      this.setData({ allList: list || [] });
      this.applyFilter();
    } catch (e) {
      wx.showToast({ title: e.message, icon: 'none' });
    }
  },
  applyFilter() {
    const key = this.data.activeStatus;
    const list = (this.data.allList || []).filter(item => key === 'ALL' || item.status === key);
    this.setData({
      list: list.map(item => ({
        ...item,
        reqTypeText: TYPE_TEXT[item.reqType] || item.reqType,
        statusText: STATUS_TEXT[item.status] || item.status
      }))
    });
  }
});
