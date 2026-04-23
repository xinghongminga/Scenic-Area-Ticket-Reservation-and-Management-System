<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue';
import { request } from '../api/http';
import { ElMessage } from 'element-plus';

const visible = ref(false);
const state = reactive({
  notifications: [],
  unreadCount: 0,
  loading: false
});
let pollTimer = null;

async function loadUnreadCount(silent = true) {
  try {
    const countData = await request('/api/user/notifications/unread-count');
    state.unreadCount = countData?.unreadCount || 0;
  } catch (e) {
    if (!silent) {
      ElMessage.error(e.message || '加载通知失败');
    }
  }
}

async function loadNotifications(showError = true) {
  try {
    state.loading = true;
    const data = await request('/api/user/notifications');
    state.notifications = Array.isArray(data) ? data : [];
    await loadUnreadCount(true);
  } catch (e) {
    if (showError) {
      ElMessage.error(e.message || '加载通知失败');
    }
  } finally {
    state.loading = false;
  }
}

async function markAsRead(id) {
  try {
    await request(`/api/user/notifications/${id}/read`, { method: 'PUT' });
    const notif = state.notifications.find(n => n.id === id);
    if (notif) {
      notif.isRead = 1;
      state.unreadCount = Math.max(0, state.unreadCount - 1);
    }
  } catch (e) {
    ElMessage.error('标记失败');
  }
}

async function markAllRead() {
  try {
    await request('/api/user/notifications/mark-all-read', { method: 'PUT' });
    state.notifications.forEach(n => n.isRead = 1);
    state.unreadCount = 0;
    ElMessage.success('已全部标记为已读');
  } catch (e) {
    ElMessage.error('标记失败');
  }
}

function getTagType(ntype) {
  const typeMap = {
    'ORDER_PAID': 'success',
    'ORDER_VERIFIED': 'success',
    'AFTERSALE_APPROVED': 'success',
    'AFTERSALE_REJECTED': 'warning',
    'SYSTEM': 'info'
  };
  return typeMap[ntype] || 'info';
}

function openNotificationCenter() {
  visible.value = true;
  loadNotifications(true);
}

onMounted(() => {
  // 后台轮询仅刷新未读数量，避免接口异常时反复打断用户。
  loadUnreadCount(true);
  pollTimer = setInterval(() => loadUnreadCount(true), 30000);
});

onBeforeUnmount(() => {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
});
</script>

<template>
  <div class="notification-center">
    <el-badge :value="state.unreadCount" :max="99" class="badge">
      <el-button icon="Bell" circle @click="openNotificationCenter" />
    </el-badge>

    <el-drawer v-model="visible" title="通知中心" size="400px">
      <div v-if="state.loading" class="loading">加载中...</div>
      <div v-else-if="state.notifications.length === 0" class="empty">暂无通知</div>
      <div v-else class="notifications-list">
        <div class="toolbar">
          <el-button type="primary" link @click="markAllRead" size="small">全部标记已读</el-button>
        </div>
        <div v-for="notif in state.notifications" :key="notif.id" class="notification-item" :class="{ unread: notif.isRead === 0 }">
          <div class="item-header">
            <span class="title">{{ notif.title }}</span>
            <el-tag :type="getTagType(notif.ntype)" size="small">{{ notif.ntype }}</el-tag>
          </div>
          <p class="content">{{ notif.content }}</p>
          <div class="item-footer">
            <span class="time">{{ new Date(notif.createdAt).toLocaleString() }}</span>
            <el-button v-if="notif.isRead === 0" link size="small" @click="markAsRead(notif.id)">标记已读</el-button>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.notification-center {
  display: inline-flex;
  align-items: center;
}
.badge {
  cursor: pointer;
}
.loading,
.empty {
  text-align: center;
  padding: 40px 20px;
  color: #999;
}
.notifications-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.toolbar {
  padding: 10px 12px;
  border-bottom: 1px solid #eee;
}
.notification-item {
  padding: 16px 12px;
  border-bottom: 1px solid #eee;
  transition: background-color 0.2s;
}
.notification-item:hover {
  background-color: #f5f5f5;
}
.notification-item.unread {
  background-color: #f0f9ff;
}
.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.title {
  font-weight: 500;
  color: #333;
}
.content {
  margin: 8px 0 0;
  color: #666;
  font-size: 13px;
  line-height: 1.5;
}
.item-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}
.time {
  font-size: 12px;
  color: #999;
}

@media (max-width: 768px) {
  :deep(.el-drawer) {
    width: 100% !important;
  }
}
</style>
