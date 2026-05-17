<script setup>
import { computed, reactive, ref } from 'vue';
import { request } from '../../api/http';
import { ElMessage } from 'element-plus';

const visible = ref(false);
const isEdit = ref(false);
const state = reactive({ list: [], form: { id: 0, name: '', address: '', openTimeDesc: '', contactPhone: '', status: 1 } });
const pager = reactive({ page: 1, pageSize: 10 });
const pagedList = computed(() => {
  const start = (pager.page - 1) * pager.pageSize;
  return state.list.slice(start, start + pager.pageSize);
});

async function load() {
  try {
    state.list = await request('/api/admin/scenic');
    pager.page = 1;
  } catch (e) {
    ElMessage.error(e.message || '加载失败');
  }
}

function openCreate() {
  isEdit.value = false;
  state.form = { id: 0, name: '', address: '', openTimeDesc: '', contactPhone: '', status: 1 };
  visible.value = true;
}

function openEdit(row) {
  isEdit.value = true;
  state.form = { ...row };
  visible.value = true;
}

async function save() {
  if (!state.form.name) {
    ElMessage.warning('景区名称必填');
    return;
  }
  try {
    if (isEdit.value) {
      await request(`/api/admin/scenic/${state.form.id}`, { method: 'PUT', body: state.form });
      ElMessage.success('更新成功');
    } else {
      await request('/api/admin/scenic', { method: 'POST', body: state.form });
      ElMessage.success('创建成功');
    }
    visible.value = false;
    load();
  } catch (e) {
    ElMessage.error(e.message || '保存失败');
  }
}

async function toggleStatus(id) {
  try {
    await request(`/api/admin/scenic/${id}/status`, { method: 'PUT', body: { status: 0 } });
    ElMessage.success('状态更新成功');
    load();
  } catch (e) {
    ElMessage.error(e.message || '状态更新失败');
  }
}
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="head">
        <div>
          <h3>景区管理</h3>
          <p>管理系统内的景区基础信息。</p>
        </div>
        <el-button @click="openCreate" type="primary">+ 新增景区</el-button>
      </div>
    </template>

    <el-table :data="pagedList" border stripe class="data-table">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="景区名称" min-width="120" />
      <el-table-column prop="address" label="地址" min-width="150" />
      <el-table-column prop="openTimeDesc" label="开放时间" min-width="120" />
      <el-table-column prop="contactPhone" label="联系电话" width="130" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button @click="openEdit(row)" link type="primary" size="small">编辑</el-button>
          <el-button @click="toggleStatus(row.id)" link type="warning" size="small">{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
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

    <el-dialog v-model="visible" :title="isEdit ? '编辑景区' : '新增景区'" width="500px">
      <el-form label-width="100px">
        <el-form-item label="景区名称">
          <el-input v-model="state.form.name" placeholder="请输入景区名称" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="state.form.address" placeholder="请输入景区地址" />
        </el-form-item>
        <el-form-item label="开放时间">
          <el-input v-model="state.form.openTimeDesc" placeholder="例：08:00-18:00" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="state.form.contactPhone" placeholder="请输入联系电话" />
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
