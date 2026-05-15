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

const REASON_OPTIONS = {
  REFUND: ['行程变更', '重复下单', '临时有事无法出行', '其他原因'],
  RESCHEDULE: ['出行时间调整', '行程延后', '临时冲突', '其他原因']
};

Page({
  data: {
    orderNo: '',
    reason: '',
    reasonIndex: 0,
    reasonOptions: REASON_OPTIONS.REFUND,
    types: [
      { key: 'REFUND', label: '退款' },
      { key: 'RESCHEDULE', label: '改签' }
    ],
    typeIndex: 0,
    loadingReschedule: false,
    rescheduleModalVisible: false,
    rescheduleOptions: [],
    availableDateRange: [],
    targetDateIndex: 0,
    targetVisitDate: '',
    targetTimeslotRange: [],
    targetTimeslotIds: [],
    targetTimeslotIndex: 0,
    targetTicketId: '',
    targetTicketName: '',
    targetTicketPriceYuan: '',
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
    if (options && options.type) {
      const nextIndex = options.type === 'RESCHEDULE' ? 1 : 0;
      this.setData({
        typeIndex: nextIndex,
        reasonOptions: REASON_OPTIONS[options.type] || REASON_OPTIONS.REFUND
      });
      this.syncReasonByIndex(0, REASON_OPTIONS[options.type] || REASON_OPTIONS.REFUND);
      if (options.type === 'RESCHEDULE' && options.orderNo) {
        this.loadRescheduleOptions();
      }
      return;
    }
    this.syncReasonByIndex(0, REASON_OPTIONS.REFUND);
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
      rescheduleModalVisible: false,
      rescheduleOptions: [],
      availableDateRange: [],
      targetDateIndex: 0,
      targetVisitDate: '',
      targetTimeslotRange: [],
      targetTimeslotIds: [],
      targetTimeslotIndex: 0,
      targetTicketId: '',
      targetTicketName: '',
      targetTicketPriceYuan: ''
    });
  },
  getReasonOptions() {
    const reqType = this.getReqType();
    return REASON_OPTIONS[reqType] || REASON_OPTIONS.REFUND;
  },
  syncReasonByIndex(index, options) {
    const reasons = options || this.getReasonOptions();
    const safeIndex = Math.max(0, Math.min(index, reasons.length - 1));
    this.setData({
      reasonIndex: safeIndex,
      reason: reasons[safeIndex] || ''
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
      targetTimeslotIndex: 0,
      targetTicketId: option ? option.ticketId : '',
      targetTicketName: option ? option.ticketName : '',
      targetTicketPriceYuan: option ? option.ticketPriceYuan : ''
    });
  },
  onOrderNoInput(e) {
    this.setData({ orderNo: e.detail.value });
    if (this.getReqType() === 'RESCHEDULE') {
      this.resetRescheduleForm();
    }
  },
  onReasonChange(e) {
    this.syncReasonByIndex(Number(e.detail.value));
  },
  onTypeChange(e) {
    const nextIndex = Number(e.detail.value);
    const reqType = (this.data.types[nextIndex] || {}).key || 'REFUND';
    this.setData({
      typeIndex: nextIndex,
      reasonOptions: REASON_OPTIONS[reqType] || REASON_OPTIONS.REFUND
    });
    this.syncReasonByIndex(0, REASON_OPTIONS[reqType] || REASON_OPTIONS.REFUND);
    if (reqType === 'RESCHEDULE') {
      if (this.data.orderNo) {
        this.loadRescheduleOptions();
      } else {
        this.resetRescheduleForm();
      }
      return;
    }
    this.resetRescheduleForm();
  },
  openRescheduleModal() {
    if (!this.data.rescheduleOptions.length) {
      this.loadRescheduleOptions();
      return;
    }
    this.setData({ rescheduleModalVisible: true });
  },
  closeRescheduleModal() {
    this.setData({ rescheduleModalVisible: false });
  },
  selectRescheduleDate(e) {
    this.syncTimeslotsByDateIndex(Number(e.currentTarget.dataset.index));
  },
  selectRescheduleTimeslot(e) {
    const dateIndex = Number(e.currentTarget.dataset.dateIndex);
    const slotIndex = Number(e.currentTarget.dataset.slotIndex);
    this.syncTimeslotsByDateIndex(dateIndex);
    this.setData({ targetTimeslotIndex: slotIndex });
  },
  confirmRescheduleChoice() {
    if (!this.data.targetVisitDate) {
      wx.showToast({ title: '请先选择改签日期', icon: 'none' });
      return;
    }
    if (!this.data.targetTimeslotRange.length) {
      wx.showToast({ title: '该日期暂无可用时段', icon: 'none' });
      return;
    }
    this.setData({ rescheduleModalVisible: false });
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
      const list = (options || []).map(item => ({
        ...item,
        ticketPriceYuan: `￥${((item.ticketPriceCent || 0) / 100).toFixed(2)}`,
        optionKey: `${item.ticketId || 't'}-${item.date || 'd'}`
      }));
      if (!list.length) {
        this.resetRescheduleForm();
        wx.showToast({ title: '暂无可改签余票日期', icon: 'none' });
        return;
      }
      this.setData({
        rescheduleOptions: list,
        availableDateRange: list.map(item => item.date),
        targetDateIndex: 0,
        rescheduleModalVisible: true
      });
      this.syncTimeslotsByDateIndex(0);
    } catch (e) {
      wx.showToast({ title: e.message, icon: 'none' });
    } finally {
      this.setData({ loadingReschedule: false });
    }
  },
  noop() {},
  onStatusTabTap(e) {
    this.setData({ activeStatus: e.currentTarget.dataset.key });
    this.applyFilter();
  },
  async submit() {
    try {
      const reqType = this.getReqType();
      const reason = (this.data.reason || '').trim();
      if (!this.data.orderNo) {
        throw new Error('请先输入订单号');
      }
      if (!reason) {
        throw new Error('请选择售后理由');
      }
      const payload = {
        orderNo: this.data.orderNo,
        reqType,
        reason
      };
      if (reqType === 'RESCHEDULE') {
        if (!this.data.targetVisitDate) {
          throw new Error('改签请先选择目标日期');
        }
        if (!this.data.targetTicketId) {
          throw new Error('改签请先选择目标票种');
        }
        const targetTimeslotId = this.data.targetTimeslotIds[this.data.targetTimeslotIndex];
        if (!targetTimeslotId) {
          throw new Error('改签请先选择可用时段');
        }
        payload.targetVisitDate = this.data.targetVisitDate;
        payload.targetTimeslotId = targetTimeslotId;
        payload.targetTicketId = this.data.targetTicketId;
      }
      await request('/api/aftersale', 'POST', payload);
      wx.showToast({ title: '已提交审核', icon: 'success' });
      this.resetRescheduleForm();
      this.setData({ reason: '', reasonIndex: 0 });
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
