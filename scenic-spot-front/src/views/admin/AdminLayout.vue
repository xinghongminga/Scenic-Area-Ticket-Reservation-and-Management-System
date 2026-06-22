<script setup>
import { computed, ref, watch } from 'vue';
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

const reportMenu = {
  label: '报表分析',
  children: [
    { to: '/reports/flow', label: '客流报表' },
    { to: '/reports/sales', label: '销量报表' }
  ]
};

const menu = computed(() => {
  const home = { to: '/home', label: '首页' };
  const common = [
    { to: '/dashboard', label: '客流看板' },
    reportMenu
  ];
  if (role.value === 'ADMIN') {
    return [
      home,
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
    return [home, { to: '/aftersale', label: '售后审核' }, { to: '/verify', label: '核验入园' }, ...common];
  }
  return [home, ...common];
});

const activePath = computed(() => route.path);

const defaultOpeneds = computed(() => {
  const opened = [];
  if (activePath.value.startsWith('/reports/')) {
    opened.push(reportMenu.label);
  }
  return opened;
});

const menuKey = ref(0);
watch(activePath, (next, prev) => {
  const nextUnder = next.startsWith('/reports/');
  const prevUnder = prev && prev.startsWith('/reports/');
  if (nextUnder !== prevUnder) {
    menuKey.value++;
  }
});

function go(path) {
  router.push(path);
}

function logout() {
  clearAuth();
  router.push('/login');
}

// ---- Tab bar ----
const pageLabelMap = {
  '/home': '首页',
  '/dashboard': '客流看板',
  '/tickets': '门票管理',
  '/orders': '订单管理',
  '/verify': '核验入园',
  '/aftersale': '售后审核',
  '/video-jobs': '景区监控',
  '/scenic': '景区管理',
  '/users': '用户管理',
  '/config': '系统配置',
  '/reports/flow': '客流报表',
  '/reports/sales': '销量报表'
};

const tabs = ref([]);

watch(() => route.path, (path) => {
  if (path === '/' || path === '/home') return;
  if (!tabs.value.find(t => t.path === path)) {
    tabs.value.push({ path, label: pageLabelMap[path] || path });
  }
}, { immediate: true });

function closeTab(path, event) {
  event.stopPropagation();
  const idx = tabs.value.findIndex(t => t.path === path);
  if (idx === -1) return;
  tabs.value.splice(idx, 1);
  if (route.path === path) {
    const next = tabs.value[idx] || tabs.value[idx - 1];
    if (next) {
      router.push(next.path);
    } else {
      router.push('/home');
    }
  }
}
</script>

<template>
  <div class="layout">
    <aside class="sider">
      <div class="brand">
        <h2>景区后台管理系统</h2>
        <p>{{ nickname }} | {{ roleText }}</p>
      </div>
      <el-menu :key="menuKey" :default-active="activePath" :default-openeds="defaultOpeneds" class="menu" @select="go">
        <template v-for="item in menu" :key="item.to || item.label">
          <el-sub-menu v-if="item.children" :index="item.label">
            <template #title>
              <span>{{ item.label }}</span>
            </template>
            <el-menu-item v-for="child in item.children" :key="child.to" :index="child.to">
              {{ child.label }}
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="item.to">{{ item.label }}</el-menu-item>
        </template>
      </el-menu>
      <el-button type="danger" plain @click="logout" class="logout-btn">退出登录</el-button>
    </aside>
    <section class="content">
      <header class="header">
        <div class="tabs-bar">
          <span
            v-for="tab in tabs"
            :key="tab.path"
            class="tab-item"
            :class="{ active: route.path === tab.path }"
            @click="go(tab.path)"
          >
            {{ tab.label }}
            <button class="tab-close" @click="closeTab(tab.path, $event)">×</button>
          </span>
        </div>
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

/* el-sub-menu dark theme overrides */
.menu :deep(.el-sub-menu__title) { color: #cfd8e3; margin: 4px 8px; height: 44px; line-height: 44px; font-size: 15px; font-weight: 500; border-radius: 4px; }
.menu :deep(.el-sub-menu__title:hover) { background: rgba(255, 255, 255, 0.08); color: #ffffff; }
.menu :deep(.el-sub-menu .el-menu) { background: rgba(0, 0, 0, 0.15); }
.menu :deep(.el-sub-menu .el-menu-item) { padding-left: 48px !important; font-size: 14px; }

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

/* Header & tab bar */
.content { display: grid; grid-template-rows: auto 1fr; padding: 0; background: #f2f3f5; min-width: 0; }
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 20px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
}
.tabs-bar { display: flex; gap: 4px; flex: 1; overflow-x: auto; align-items: center; }
.tabs-bar::-webkit-scrollbar { height: 3px; }
.tabs-bar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 2px; }

.tab-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
  padding: 6px 10px;
  font-size: 13px;
  color: #64748b;
  background: #f1f5f9;
  border-radius: 6px;
  cursor: pointer;
  user-select: none;
  transition: background 0.15s, color 0.15s;
  border: 1px solid transparent;
}
.tab-item:hover { background: #e2e8f0; color: #334155; }
.tab-item.active { background: #fff; color: #1e293b; font-weight: 600; border-color: #409eff; box-shadow: 0 0 0 1px #409eff; }

.tab-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border: none;
  background: transparent;
  color: #94a3b8;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  border-radius: 4px;
  padding: 0;
  margin-left: 2px;
}
.tab-close:hover { background: #cbd5e1; color: #334155; }

.main { padding: 20px; overflow-x: auto; min-width: 0; }

@media (max-width: 900px) {
  .layout { grid-template-columns: 1fr; }
  .sider { position: sticky; top: 0; z-index: 5; }
  .header { flex-wrap: wrap; padding: 8px 12px; }
  .tabs-bar { order: 2; flex-basis: 100%; }
}
</style>
