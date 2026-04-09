function pushFlashNotice(notice) {
  const app = getApp();
  app.globalData.flashNotice = {
    title: notice?.title || '消息提醒',
    content: notice?.content || '',
    notificationId: notice?.notificationId || '',
    createdAt: Date.now()
  };
}

function consumeFlashNotice() {
  const app = getApp();
  const notice = app.globalData.flashNotice;
  app.globalData.flashNotice = null;
  return notice;
}

function showTopNotice(page, notice) {
  if (!page || !notice) return;
  hideTopNotice(page, false);
  page.setData({
    noticeBar: {
      visible: true,
      title: notice.title || '消息提醒',
      content: notice.content || '',
      notificationId: notice.notificationId || ''
    }
  });
  page.__noticeTimer = setTimeout(() => {
    if (page && page.setData) {
      page.setData({ 'noticeBar.visible': false });
    }
    page.__noticeTimer = null;
  }, 3000);
}

function hideTopNotice(page, updateView = true) {
  if (!page) return;
  if (page.__noticeTimer) {
    clearTimeout(page.__noticeTimer);
    page.__noticeTimer = null;
  }
  if (updateView && page.setData) {
    page.setData({ 'noticeBar.visible': false });
  }
}

module.exports = {
  pushFlashNotice,
  consumeFlashNotice,
  showTopNotice,
  hideTopNotice
};