<script setup>
import { computed, reactive } from 'vue';
import { request } from '../../api/http';
import { ElMessage } from 'element-plus';

const ROOT_SCENIC_ID = 1;
const state = reactive({ list: [], showForm: false, editId: null, page: 1, pageSize: 10 });
const form = reactive({ name: '', address: '', openTimeDesc: '', contactPhone: '', status: 'ACTIVE' });
const pagedList = computed(() => {
  const start = (state.page - 1) * state.pageSize;
  return state.list.slice(start, start + state.pageSize);
});

async function loadList() {
  try {
    const list = await request('/api/admin/scenic') || [];
    state.list = list.filter(item => Number(item.id) !== ROOT_SCENIC_ID);
    state.page = 1;
  } catch (e) {
    ElMessage.error('加载景区项目列表失败');
  }
}

function openForm(project) {
  if (project) {
    state.editId = project.id;
    form.name = project.name || '';
    form.address = project.address || '';
    form.openTimeDesc = project.openTimeDesc || '';
    form.contactPhone = project.contactPhone || '';
    form.status = Number(project.status) === 1 ? 'ACTIVE' : 'DISABLED';
  } else {
    state.editId = null;
    form.name = '';
    form.address = '';
    form.openTimeDesc = '';
    form.contactPhone = '';
    form.status = 'ACTIVE';
  }
  state.showForm = true;
}

async function saveProject() {
  try {
    const url = state.editId ? `/api/admin/scenic/${state.editId}` : '/api/admin/scenic';
    const method = state.editId ? 'PUT' : 'POST';
    const data = {
      name: form.name,
      address: form.address,
      openTimeDesc: form.openTimeDesc,
      contactPhone: form.contactPhone
    };
    const result = await request(url, { method, body: data });
    const targetId = state.editId || result?.id;
    if (targetId) {
      await request(`/api/admin/scenic/${targetId}/status`, {
        method: 'PUT',
        body: { status: form.status === 'ACTIVE' ? 1 : 0 }
      });
    }
    ElMessage.success('保存成功');
    state.showForm = false;
    loadList();
  } catch (e) {
    ElMessage.error('保存失败');
  }
}

async function changeStatus(project) {
  try {
    const next = Number(project.status) === 1 ? 0 : 1;
    await request(`/api/admin/scenic/${project.id}/status`, {
      method: 'PUT',
      body: { status: next }
    });
    ElMessage.success(next === 1 ? '已启用' : '已禁用');
    loadList();
  } catch (e) {
    ElMessage.error('状态更新失败');
  }
}

loadList();
</script>

<template>
  <div class="card">
    <h3>景区项目管理</h3>
    <p class="tip">当前系统可管理一个景区，以下配置的是景区内可销售的项目区域（如游乐园、动物园、水族馆）。</p>
    <el-button @click="openForm()" type="primary">新增项目</el-button>

    <el-table :data="pagedList" stripe>
      <el-table-column type="index" />
      <el-table-column prop="name" label="项目名称" />
      <el-table-column prop="address" label="区域位置" />
      <el-table-column prop="openTimeDesc" label="开放说明" width="150" />
      <el-table-column prop="contactPhone" label="联系电话" width="130" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button @click="openForm(row)" text type="primary" size="small">编辑</el-button>
          <el-button @click="changeStatus(row)" text type="warning" size="small">{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
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

    <!-- 编辑表单 -->
    <el-dialog v-model="state.showForm" :title="state.editId ? '编辑项目' : '新增项目'" width="500px">
      <el-form label-width="120px">
        <el-form-item label="项目名称">
          <el-input v-model="form.name" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="区域位置">
          <el-input v-model="form.address" placeholder="请输入区域位置" />
        </el-form-item>
        <el-form-item label="开放说明">
          <el-input v-model="form.openTimeDesc" placeholder="如：09:00-18:00" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" placeholder="请输入电话号码" />
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
        <el-button @click="saveProject" type="primary">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.card { background: #fff; border-radius: 14px; padding: 16px; }
.tip { margin: 8px 0 12px; color: #64748b; font-size: 13px; }
.pager { display: flex; justify-content: center; margin-top: 12px; }
</style>


