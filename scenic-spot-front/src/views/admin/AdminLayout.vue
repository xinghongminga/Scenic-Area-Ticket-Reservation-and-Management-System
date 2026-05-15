<script setup>
import { computed } from 'vue';
import { RouterView, useRoute, useRouter } from 'vue-router';
import { clearAuth, getRole } from '../../utils/auth';
import NotificationCenter from '../../components/NotificationCenter.vue';

const router = useRouter();
const route = useRoute();
const role = computed(() => getRole());
const nickname = computed(() => localStorage.getItem('nickname') || '用户');
const roleText = computed(() => {
  const map = {
    ADMIN: '管理员',
    ANALYST: '分析员',
    AUDITOR: '审核员',
    TOURIST: '游客'
  };
  return map[role.value] || role.value || '未知角色';
});

const menu = computed(() => {
  const common = [
    { to: '/dashboard', label: '客流看板' },
    { to: '/reports', label: '报表分析' }
  ];
  if (role.value === 'ADMIN') {
    return [
      { to: '/tickets', label: '门票管理' },
      { to: '/orders', label: '订单管理' },
      { to: '/verify', label: '核验入园' },
      { to: '/aftersale', label: '售后审核' },
      { to: '/video-jobs', label: '景区监控' },
      { to: '/scenic', label: '景区管理' },
      { to: '/users', label: '用户管理' },
      { to: '/config', label: '系统配置' },
      ...common
    ];
  }
  if (role.value === 'AUDITOR') {
    return [{ to: '/aftersale', label: '售后审核' }, { to: '/verify', label: '核验入园' }, ...common];
  }
  return common;
});

const activePath = computed(() => route.path);

function go(path) {
  router.push(path);
}

function logout() {
  clearAuth();
  router.push('/login');
}
</script>

<template>
  <div class="layout">
    <aside class="sider">
      <div class="brand">
        <h2>Scenic Console</h2>
        <p>{{ nickname }} · {{ roleText }}</p>
      </div>
      <el-menu :default-active="activePath" class="menu" @select="go">
        <el-menu-item v-for="item in menu" :key="item.to" :index="item.to">{{ item.label }}</el-menu-item>
      </el-menu>
      <el-button type="danger" plain @click="logout" class="logout-btn">退出登录</el-button>
    </aside>
    <section class="content">
      <header class="header">
        <div></div>
        <NotificationCenter />
      </header>
      <main class="main">
        <RouterView />
      </main>
    </section>
  </div>
</template>

<style scoped>
.layout { min-height: 100vh; display: grid; grid-template-columns: 220px 1fr; background: #f2f3f5; }
.sider { background: #304156; color: #e5e7eb; padding: 16px 0; display: grid; gap: 10px; align-content: start; box-shadow: 2px 0 8px rgba(15, 23, 42, 0.12); }
.brand { padding: 0 16px 12px; border-bottom: 1px solid rgba(255, 255, 255, 0.08); }
.brand h2 { margin: 0; font-size: 20px; letter-spacing: 0.6px; color: #f9fafb; }
.brand p { margin: 6px 0 0; color: #9ca3af; font-size: 13px; }
.menu { border-right: none; background: transparent; }
.menu :deep(.el-menu-item) { color: #cfd8e3; margin: 4px 8px; height: 44px; line-height: 44px; font-size: 15px; font-weight: 500; border-radius: 4px; transition: background 0.15s ease, color 0.15s ease; }
.menu :deep(.el-menu-item.is-active) { color: #ffffff; background: #409eff; }
.menu :deep(.el-menu-item:hover) { background: rgba(255, 255, 255, 0.08); color: #ffffff; }
.logout-btn {
  width: calc(100% - 16px);
  margin: 6px 8px 0;
  border-color: rgba(255, 255, 255, 0.2);
  color: #ffffff;
  font-size: 14px;
  background: rgba(239, 68, 68, 0.92);
  box-shadow: 0 8px 18px rgba(239, 68, 68, 0.22);
}
.logout-btn:hover,
.logout-btn:focus {
  color: #ffffff;
  background: #dc2626;
  border-color: #dc2626;
}
.content { padding: 20px; background: #f2f3f5; }

@media (max-width: 900px) {
  .layout { grid-template-columns: 1fr; }
  .sider { position: sticky; top: 0; z-index: 5; }
}
</style>
