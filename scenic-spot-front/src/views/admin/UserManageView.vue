<script setup>
import { computed, reactive } from 'vue';
import { request } from '../../api/http';
import { ElMessage, ElMessageBox } from 'element-plus';

const ROLE_OPTIONS = [
  { label: '管理员', value: 'ADMIN' },
  { label: '分析员', value: 'ANALYST' },
  { label: '审核员', value: 'AUDITOR' }
];

function formatRole(role) {
  const map = {
    ADMIN: '管理员',
    ANALYST: '分析员',
    AUDITOR: '审核员',
    TOURIST: '游客',
    STAFF: '工作人员',
    USER: '普通用户'
  };
  return map[role] || role;
}

const state = reactive({
  list: [],
  showForm: false,
  editId: null,
  keyword: '',
  roleFilter: 'ALL',
  statusFilter: 'ALL',
  page: 1,
  pageSize: 10
});

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  fullName: '',
  idCardNo: '',
  role: 'ANALYST',
  status: 'ACTIVE'
});

const pagedList = computed(() => {
  const start = (state.page - 1) * state.pageSize;
  return state.list.slice(start, start + state.pageSize);
});

async function loadList() {
  try {
    const params = new URLSearchParams();
    if (state.keyword) params.append('keyword', state.keyword);
    if (state.roleFilter !== 'ALL') params.append('role', state.roleFilter);
    if (state.statusFilter !== 'ALL') params.append('status', state.statusFilter === 'ACTIVE' ? '1' : '0');
    const query = params.toString();
    const url = query ? `/api/admin/users?${query}` : '/api/admin/users';
    state.list = (await request(url)) || [];
    state.page = 1;
  } catch (e) {
    ElMessage.error('加载用户列表失败');
  }
}

function openForm(user) {
  if (user) {
    state.editId = user.id;
    form.username = user.username || '';
    form.nickname = user.nickname || '';
    form.fullName = user.fullName || '';
    form.idCardNo = user.idCardNo || '';
    form.role = user.role || 'ANALYST';
    form.status = user.status === 1 ? 'ACTIVE' : 'DISABLED';
    form.password = '';
  } else {
    state.editId = null;
    form.username = '';
    form.password = '';
    form.nickname = '';
    form.fullName = '';
    form.idCardNo = '';
    form.role = 'ANALYST';
    form.status = 'ACTIVE';
  }
  state.showForm = true;
}

async function saveUser() {
  try {
    if (state.editId) {
      const data = {
        nickname: form.nickname,
        fullName: form.fullName,
        idCardNo: form.idCardNo,
        role: form.role
      };
      await request(`/api/admin/users/${state.editId}`, { method: 'PUT', body: data });
      await request(`/api/admin/users/${state.editId}/status`, {
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
        role: form.role
      };
      await request('/api/admin/users/staff', { method: 'POST', body: data });
    }
    ElMessage.success('保存成功');
    state.showForm = false;
    await loadList();
  } catch (e) {
    ElMessage.error('保存失败');
  }
}

async function deleteUser(id) {
  try {
    await ElMessageBox.confirm('系统不提供物理删除，是否改为禁用该用户?', '提示');
    await request(`/api/admin/users/${id}/status`, { method: 'PUT', body: { status: 0 } });
    ElMessage.success('用户已禁用');
    await loadList();
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
    await loadList();
  } catch (e) {
    ElMessage.error('更新状态失败');
  }
}

function search() {
  loadList();
}

loadList();
</script>

<template>
  <div class="card">
    <h3>用户管理</h3>
    <el-button @click="openForm()" type="primary">新增用户</el-button>

    <div style="margin: 12px 0; display: flex; gap: 8px; align-items: center; flex-wrap: wrap;">
      <el-input v-model="state.keyword" placeholder="搜索用户名/昵称/姓名/身份证/手机号" style="width: 320px;" />
      <el-select v-model="state.roleFilter" style="width: 120px;">
        <el-option label="全部角色" value="ALL" />
        <el-option label="管理员" value="ADMIN" />
        <el-option label="分析员" value="ANALYST" />
        <el-option label="审核员" value="AUDITOR" />
        <el-option label="游客" value="TOURIST" />
      </el-select>
      <el-select v-model="state.statusFilter" style="width: 120px;">
        <el-option label="全部状态" value="ALL" />
        <el-option label="启用" value="ACTIVE" />
        <el-option label="禁用" value="DISABLED" />
      </el-select>
      <el-button @click="search" type="primary">搜索</el-button>
    </div>

    <el-table :data="pagedList" stripe>
      <el-table-column type="index" width="50" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="fullName" label="姓名" min-width="110" />
      <el-table-column prop="idCardNo" label="身份证号" min-width="180" />
      <el-table-column label="角色" width="100">
        <template #default="{ row }">{{ formatRole(row.role) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? 'ACTIVE' : 'DISABLED' }}</el-tag>
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
        v-model:current-page="state.page"
        v-model:page-size="state.pageSize"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        :total="state.list.length"
      />
    </div>

    <el-dialog v-model="state.showForm" :title="state.editId ? '编辑用户' : '新增用户'" width="500px">
      <el-form label-width="120px">
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" :disabled="!!state.editId" placeholder="请输入用户名" />
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
        <el-form-item label="密码" :required="!state.editId">
          <el-input v-model="form.password" type="password" :placeholder="state.editId ? '不改可留空' : '请输入密码'" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role">
            <el-option v-for="item in ROLE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
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
        <el-button @click="state.showForm = false">取消</el-button>
        <el-button @click="saveUser" type="primary">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.card { background: #fff; border-radius: 14px; padding: 16px; }
.pager { display: flex; justify-content: center; margin-top: 12px; }
</style>

