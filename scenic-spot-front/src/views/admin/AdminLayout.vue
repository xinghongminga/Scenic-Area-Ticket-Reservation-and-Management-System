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
      { to: '/video-jobs', label: '视频计数任务' },
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
.layout { min-height: 100vh; display: grid; grid-template-columns: 240px 1fr; }
.sider { background: linear-gradient(165deg, #0f172a, #1f2937); color: #f9fafb; padding: 20px 14px; display: grid; gap: 12px; align-content: start; }
.brand { padding: 0 10px 8px; }
.brand h2 { margin: 0; }
.brand p { margin: 8px 0 0; color: #cbd5e1; }
.menu { border-right: none; background: transparent; }
.menu :deep(.el-menu-item) { color: #d1fae5; border-radius: 10px; margin: 4px 8px; }
.menu :deep(.el-menu-item.is-active) { color: #0f172a; background: #99f6e4; }
.menu :deep(.el-menu-item:hover) { background: rgba(255, 255, 255, 0.12); }
.content { padding: 20px; background: linear-gradient(120deg, #f4f8fb, #f6f1e8); }

@media (max-width: 900px) {
  .layout { grid-template-columns: 1fr; }
  .sider { position: sticky; top: 0; z-index: 5; }
}
</style>
