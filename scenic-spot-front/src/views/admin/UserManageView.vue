<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { request } from '../../api/http';
import { ElMessage, ElMessageBox } from 'element-plus';

const activeTab = ref('user');

const TAB_CONFIG = {
  user: {
    title: '普通用户',
    role: 'TOURIST',
    canCreate: false
  },
  admin: {
    title: '工作人员',
    roles: ['ADMIN', 'ANALYST', 'AUDITOR'],
    canCreate: true
  }
};

const STAFF_ROLE_OPTIONS = [
  { label: '管理员', value: 'ADMIN' },
  { label: '分析员', value: 'ANALYST' },
  { label: '审核员', value: 'AUDITOR' }
];

function formatRole(role) {
  const map = {
    ADMIN: '管理员',
    ANALYST: '分析员',
    AUDITOR: '审核员',
    TOURIST: '普通用户',
    STAFF: '工作人员',
    USER: '普通用户'
  };
  return map[role] || role;
}

function createSection() {
  return reactive({
    list: [],
    keyword: '',
    statusFilter: 'ALL',
    page: 1,
    pageSize: 10
  });
}

const sections = {
  user: createSection(),
  admin: createSection()
};

const dialogVisible = ref(false);
const editId = ref(null);
const saving = ref(false);

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  fullName: '',
  idCardNo: '',
  role: 'TOURIST',
  status: 'ACTIVE'
});

const currentSection = computed(() => sections[activeTab.value]);
const currentTabConfig = computed(() => TAB_CONFIG[activeTab.value]);

const pagedUserList = computed(() => {
  const section = sections.user;
  const start = (section.page - 1) * section.pageSize;
  return section.list.slice(start, start + section.pageSize);
});

const pagedAdminList = computed(() => {
  const section = sections.admin;
  const start = (section.page - 1) * section.pageSize;
  return section.list.slice(start, start + section.pageSize);
});

async function loadList(tabKey) {
  const section = sections[tabKey];
  const config = TAB_CONFIG[tabKey];
  try {
    const roles = config.roles || [config.role];
    const requests = roles.map((role) => {
      const params = new URLSearchParams();
      if (section.keyword) params.append('keyword', section.keyword);
      params.append('role', role);
      if (section.statusFilter !== 'ALL') params.append('status', section.statusFilter === 'ACTIVE' ? '1' : '0');
      const query = params.toString();
      const url = query ? `/api/admin/users?${query}` : '/api/admin/users';
      return request(url);
    });
    const result = await Promise.all(requests);
    section.list = result.flat().filter(Boolean);
    if (tabKey === 'admin') {
      section.list.sort((left, right) => {
        const roleOrder = { ADMIN: 1, ANALYST: 2, AUDITOR: 3 };
        return (roleOrder[left.role] || 99) - (roleOrder[right.role] || 99);
      });
    }
    section.page = 1;
  } catch (e) {
    ElMessage.error(`加载${config.title}列表失败`);
  }
}

function openForm(user) {
  const role = currentTabConfig.value.roles?.[0] || currentTabConfig.value.role;
  if (user) {
    editId.value = user.id;
    form.username = user.username || '';
    form.nickname = user.nickname || '';
    form.fullName = user.fullName || '';
    form.idCardNo = user.idCardNo || '';
    form.role = user.role || role;
    form.status = user.status === 1 ? 'ACTIVE' : 'DISABLED';
    form.password = '';
  } else {
    editId.value = null;
    form.username = '';
    form.password = '';
    form.nickname = '';
    form.fullName = '';
    form.idCardNo = '';
    form.role = role;
    form.status = 'ACTIVE';
  }
  dialogVisible.value = true;
}

async function saveUser() {
  const sectionRole = currentTabConfig.value.roles?.includes(form.role) ? form.role : (currentTabConfig.value.role || form.role);
  saving.value = true;
  try {
    if (editId.value) {
      const data = {
        nickname: form.nickname,
        fullName: form.fullName,
        idCardNo: form.idCardNo,
        role: sectionRole
      };
      await request(`/api/admin/users/${editId.value}`, { method: 'PUT', body: data });
      await request(`/api/admin/users/${editId.value}/status`, {
        method: 'PUT',
        body: { status: form.status === 'ACTIVE' ? 1 : 0 }
      });
    } else {
      const data = {
        username: form.username,
        password: form.password,
        nickname: form.nickname,
        fullName: form.fullName,
        idCardNo: form.idCardNo,
        role: sectionRole
      };
      await request('/api/admin/users/staff', { method: 'POST', body: data });
    }
    ElMessage.success('保存成功');
    dialogVisible.value = false;
    await loadList(activeTab.value);
  } catch (e) {
    ElMessage.error('保存失败');
  } finally {
    saving.value = false;
  }
}

async function deleteUser(id) {
  try {
    await ElMessageBox.confirm('系统不提供物理删除，是否改为禁用该用户?', '提示');
    await request(`/api/admin/users/${id}/status`, { method: 'PUT', body: { status: 0 } });
    ElMessage.success('用户已禁用');
    await loadList(activeTab.value);
  } catch (e) {
    // ignore cancel
  }
}

async function resetPassword(userId) {
  try {
    await ElMessageBox.confirm('确定重置此用户密码为 123456?', '提示');
    await request(`/api/admin/users/${userId}/password`, { method: 'PUT', body: { newPassword: '123456' } });
    ElMessage.success('密码已重置为 123456');
  } catch (e) {
    // ignore cancel
  }
}

async function toggleStatus(userId, currentStatus) {
  try {
    const newStatus = currentStatus === 1 || currentStatus === 'ACTIVE' ? 0 : 1;
    await request(`/api/admin/users/${userId}/status`, { method: 'PUT', body: { status: newStatus } });
    ElMessage.success('状态已更新');
    await loadList(activeTab.value);
  } catch (e) {
    ElMessage.error('更新状态失败');
  }
}

function search(tabKey) {
  loadList(tabKey);
}

function onTabChange(tabName) {
  if (!sections[tabName].list.length) {
    loadList(tabName);
  }
}

onMounted(async () => {
  await Promise.all([loadList('user'), loadList('admin')]);
});
</script>

<template>
  <div class="card">
    <div class="head">
      <h3>用户管理</h3>
      <el-button v-if="TAB_CONFIG[activeTab].canCreate" @click="openForm()" type="primary">新增工作人员</el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="普通用户" name="user">
        <div class="toolbar">
          <el-input v-model="sections.user.keyword" placeholder="搜索用户名/昵称/姓名/身份证/手机号" class="search-input" />
          <el-select v-model="sections.user.statusFilter" class="status-select">
            <el-option label="全部状态" value="ALL" />
            <el-option label="启用" value="ACTIVE" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
          <el-button @click="search('user')" type="primary">搜索</el-button>
        </div>

        <el-table :data="pagedUserList" stripe>
          <el-table-column type="index" width="50" />
          <el-table-column prop="nickname" label="昵称" />
          <el-table-column prop="fullName" label="姓名" min-width="110" />
          <el-table-column prop="phone" label="电话号码" min-width="140" />
          <el-table-column prop="idCardNo" label="身份证号" min-width="180" />
          <el-table-column label="角色" width="100">
            <template #default="{ row }">{{ formatRole(row.role) }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280">
            <template #default="{ row }">
              <el-button @click="openForm(row)" text type="primary" size="small">编辑</el-button>
              <el-button @click="resetPassword(row.id)" text type="warning" size="small">重置密码</el-button>
              <el-button @click="toggleStatus(row.id, row.status)" text type="info" size="small">
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button @click="deleteUser(row.id)" text type="danger" size="small">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pager">
          <el-pagination
            v-model:current-page="sections.user.page"
            v-model:page-size="sections.user.pageSize"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            :total="sections.user.list.length"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="工作人员" name="admin">
        <div class="toolbar">
          <el-input v-model="sections.admin.keyword" placeholder="搜索用户名/昵称/姓名/身份证/手机号" class="search-input" />
          <el-select v-model="sections.admin.statusFilter" class="status-select">
            <el-option label="全部状态" value="ALL" />
            <el-option label="启用" value="ACTIVE" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
          <el-button @click="search('admin')" type="primary">搜索</el-button>
          <el-button @click="openForm()" type="primary">新增工作人员</el-button>
        </div>

        <el-table :data="pagedAdminList" stripe>
          <el-table-column type="index" width="50" />
          <el-table-column prop="nickname" label="昵称" />
          <el-table-column prop="fullName" label="姓名" min-width="110" />
          <el-table-column prop="phone" label="电话号码" min-width="140" />
          <el-table-column prop="idCardNo" label="身份证号" min-width="180" />
          <el-table-column label="角色" width="100">
            <template #default="{ row }">{{ formatRole(row.role) }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280">
            <template #default="{ row }">
              <el-button @click="openForm(row)" text type="primary" size="small">编辑</el-button>
              <el-button @click="resetPassword(row.id)" text type="warning" size="small">重置密码</el-button>
              <el-button @click="toggleStatus(row.id, row.status)" text type="info" size="small">
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button @click="deleteUser(row.id)" text type="danger" size="small">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pager">
          <el-pagination
            v-model:current-page="sections.admin.page"
            v-model:page-size="sections.admin.pageSize"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            :total="sections.admin.list.length"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialogVisible" :title="editId ? `编辑${currentTabConfig.title}` : `新增${currentTabConfig.title}`" width="500px">
      <el-form label-width="120px">
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" :disabled="!!editId || activeTab === 'user'" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item v-if="!editId" label="密码" required>
          <el-input v-model="form.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="昵称" required>
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="form.fullName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="身份证号" required>
          <el-input v-model="form.idCardNo" placeholder="请输入身份证号" />
        </el-form-item>
        <el-form-item v-if="activeTab === 'admin'" label="岗位角色">
          <el-select v-model="form.role">
            <el-option v-for="item in STAFF_ROLE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :loading="saving" @click="saveUser" type="primary">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.card { background: #fff; border-radius: 14px; padding: 16px; }
.head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; gap: 12px; }
.toolbar { margin: 12px 0; display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.search-input { width: 320px; }
.status-select { width: 120px; }
.pager { display: flex; justify-content: center; margin-top: 12px; }
@media (max-width: 768px) {
  .head { flex-direction: column; align-items: flex-start; }
  .search-input { width: 100%; }
}
</style>

