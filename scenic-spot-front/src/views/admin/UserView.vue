<script setup>
import { computed, reactive, ref } from 'vue';
import { request } from '../../api/http';
import { ElMessage, ElMessageBox } from 'element-plus';

const visible = ref(false);
const isEdit = ref(false);
const state = reactive({
  list: [],
  form: { id: 0, nickname: '', fullName: '', phone: '', role: 'STAFF', status: 1, password: '' },
  passwordVisible: false
});
const pager = reactive({ page: 1, pageSize: 10 });
const pagedList = computed(() => {
  const start = (pager.page - 1) * pager.pageSize;
  return state.list.slice(start, start + pager.pageSize);
});

async function load() {
  try {
    state.list = await request('/api/admin/users');
    pager.page = 1;
  } catch (e) {
    ElMessage.error(e.message || '加载失败');
  }
}

function openCreate() {
  isEdit.value = false;
  state.form = { id: 0, nickname: '', fullName: '', phone: '', role: 'STAFF', status: 1, password: '' };
  state.passwordVisible = false;
  visible.value = true;
}

function openEdit(row) {
  isEdit.value = true;
  state.form = { id: row.id, nickname: row.nickname || '', fullName: row.fullName || '', phone: row.phone || '', role: row.role, status: row.status, password: '' };
  state.passwordVisible = false;
  visible.value = true;
}

async function save() {
  if (!state.form.nickname || !state.form.phone) {
    ElMessage.warning('姓名和电话必填');
    return;
  }
  if (!isEdit.value && !state.form.password) {
    ElMessage.warning('新增用户密码必填');
    return;
  }
  try {
    if (isEdit.value) {
      await request(`/api/admin/users/${state.form.id}`, { method: 'PUT', body: state.form });
      ElMessage.success('更新成功');
    } else {
      await request('/api/admin/users', { method: 'POST', body: state.form });
      ElMessage.success('创建成功');
    }
    visible.value = false;
    load();
  } catch (e) {
    ElMessage.error(e.message || '保存失败');
  }
}

async function deleteUser(id) {
  try {
    await ElMessageBox.confirm('确认删除此用户?', '警告', { type: 'warning' });
    await request(`/api/admin/users/${id}`, { method: 'DELETE' });
    ElMessage.success('删除成功');
    load();
  } catch (e) {
    if (e === 'cancel' || e === 'close') return;
    ElMessage.error(e.message || '删除失败');
  }
}

async function toggleStatus(id) {
  try {
    await request(`/api/admin/users/${id}/status`, { method: 'PUT' });
    ElMessage.success('状态更新成功');
    load();
  } catch (e) {
    ElMessage.error(e.message || '状态更新失败');
  }
}

async function resetPassword(id) {
  try {
    await ElMessageBox.confirm('确认重置此用户密码为 123456?', '提示', { type: 'info' });
    await request(`/api/admin/users/${id}/reset-password`, { method: 'PUT' });
    ElMessage.success('密码已重置为 123456');
    load();
  } catch (e) {
    if (e === 'cancel' || e === 'close') return;
    ElMessage.error(e.message || '重置失败');
  }
}

const roleMap = {
  ADMIN: '管理员',
  AUDITOR: '审核员',
  ANALYST: '分析员',
  STAFF: '员工',
  TOURIST: '游客'
};

function roleName(role) {
  return roleMap[role] || role;
}

load();
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="head">
        <div>
          <h3>用户管理</h3>
          <p>管理系统内所有用户账号。</p>
        </div>
        <el-button @click="openCreate" type="primary">+ 创建员工</el-button>
      </div>
    </template>

    <el-table :data="pagedList" border stripe class="data-table">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="nickname" label="昵称" width="100" />
      <el-table-column prop="fullName" label="姓名" width="100" />
      <el-table-column prop="phone" label="电话" width="130" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'">{{ roleName(row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button @click="openEdit(row)" link type="primary" size="small">编辑</el-button>
          <el-button @click="resetPassword(row.id)" link type="warning" size="small">重置密码</el-button>
          <el-button @click="toggleStatus(row.id)" link type="warning" size="small">{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
          <el-button @click="deleteUser(row.id)" link type="danger" size="small">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager">
      <el-pagination
        v-model:current-page="pager.page"
        v-model:page-size="pager.pageSize"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        :total="state.list.length"
      />
    </div>

    <el-dialog v-model="visible" :title="isEdit ? '编辑用户' : '创建员工'" width="500px">
      <el-form label-width="100px">
        <el-form-item label="昵称">
          <el-input v-model="state.form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="state.form.fullName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="state.form.phone" placeholder="请输入电话" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="state.form.role">
            <el-option label="员工" value="STAFF" />
            <el-option label="管理员" value="ADMIN" />
            <el-option label="审核员" value="AUDITOR" />
            <el-option label="分析员" value="ANALYST" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码">
          <el-input v-model="state.form.password" type="password" placeholder="请输入初始密码" show-password />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="state.form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button @click="save" type="primary">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.el-card { border-radius: 16px; }
.head { display: flex; align-items: flex-start; justify-content: space-between; }
.head h3 { margin: 0; font-size: 22px; font-weight: 700; color: #0f172a; }
.head p { margin: 8px 0 0; color: #64748b; font-size: 15px; }

.data-table {
  border-radius: 14px;
  overflow: hidden;
  font-size: 15px;
}
.data-table :deep(th.el-table__cell) {
  background: #f8fafc;
  color: #334155;
  font-size: 15px;
  font-weight: 600;
}
.data-table :deep(td.el-table__cell) {
  padding-top: 14px;
  padding-bottom: 14px;
}
.data-table :deep(.cell) { line-height: 1.5; }
.data-table :deep(.el-table__body tr:hover > td) { background: #f7faff; }
.data-table :deep(.el-tag) {
  border-radius: 999px;
  padding: 0 10px;
  height: 26px;
  line-height: 24px;
  font-size: 13px;
}
.pager { display: flex; justify-content: center; margin-top: 12px; }
</style>
