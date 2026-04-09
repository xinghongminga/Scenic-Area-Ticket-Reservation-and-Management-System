<script setup>
import { computed, reactive, ref } from 'vue';
import { request } from '../../api/http';
import { ElMessage, ElMessageBox } from 'element-plus';

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
    state.list = await request('/api/admin/scenic/list');
    pager.page = 1;
    ElMessage.success('鏁版嵁鍔犺浇鎴愬姛');
  } catch (e) {
    ElMessage.error('鍔犺浇澶辫触');
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
    ElMessage.warning('鏅尯鍚嶇О蹇呭～');
    return;
  }
  try {
    if (isEdit.value) {
      await request('/api/admin/scenic/update', { method: 'PUT', body: JSON.stringify(state.form) });
      ElMessage.success('鏇存柊鎴愬姛');
    } else {
      await request('/api/admin/scenic/create', { method: 'POST', body: JSON.stringify(state.form) });
      ElMessage.success('鍒涘缓鎴愬姛');
    }
    visible.value = false;
    load();
  } catch (e) {
    ElMessage.error('淇濆瓨澶辫触');
  }
}

async function deleteScenic(id) {
  ElMessageBox.confirm('纭鍒犻櫎?', '璀﹀憡', { type: 'warning' })
    .then(async () => {
      try {
        await request(`/api/admin/scenic/delete/${id}`, { method: 'DELETE' });
        ElMessage.success('鍒犻櫎鎴愬姛');
        load();
      } catch (e) {
        ElMessage.error('鍒犻櫎澶辫触');
      }
    })
    .catch(() => {});
}

async function toggleStatus(id) {
  try {
    await request(`/api/admin/scenic/status/${id}`, { method: 'PUT' });
    ElMessage.success('鐘舵€佹洿鏂版垚鍔');
    load();
  } catch (e) {
    ElMessage.error('鐘舵€佹洿鏂板け璐');
  }
}

load();
</script>

<template>
  <div class="card">
    <div class="header">
      <h3>鏅尯绠＄悊</h3>
      <el-button @click="openCreate" type="primary">+ 鏂板鏅尯</el-button>
    </div>

    <el-table :data="pagedList" stripe border>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="鏅尯鍚嶇О" min-width="120" />
      <el-table-column prop="address" label="鍦板潃" min-width="150" />
      <el-table-column prop="openTimeDesc" label="开放时间" min-width="120" />
      <el-table-column prop="contactPhone" label="鑱旂郴鐢佃瘽" width="120" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '鍚敤' : '绂佺敤' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="鎿嶄綔" width="180" fixed="right">
        <template #default="{ row }">
          <el-button @click="openEdit(row)" link type="primary" size="small">缂栬緫</el-button>
          <el-button @click="toggleStatus(row.id)" link type="warning" size="small">{{ row.status === 1 ? '绂佺敤' : '鍚敤' }}</el-button>
          <el-button @click="deleteScenic(row.id)" link type="danger" size="small">鍒犻櫎</el-button>
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

    <el-dialog v-model="visible" :title="isEdit ? '缂栬緫鏅尯' : '鏂板鏅尯'" width="500px">
      <el-form label-width="100px">
        <el-form-item label="鏅尯鍚嶇О">
          <el-input v-model="state.form.name" placeholder="璇疯緭鍏ユ櫙鍖哄悕绉" />
        </el-form-item>
        <el-form-item label="鍦板潃">
          <el-input v-model="state.form.address" placeholder="璇疯緭鍏ユ櫙鍖哄湴鍧€" />
        </el-form-item>
        <el-form-item label="寮€鏀炬椂闂" >
          <el-input v-model="state.form.openTimeDesc" placeholder="渚嬶細08:00-18:00" />
        </el-form-item>
        <el-form-item label="鑱旂郴鐢佃瘽">
          <el-input v-model="state.form.contactPhone" placeholder="璇疯緭鍏ヨ仈绯荤數璇" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">鍙栨秷</el-button>
        <el-button @click="save" type="primary">淇濆瓨</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.card {
  background: #fff;
  border-radius: 14px;
  padding: 16px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
h3 {
  margin: 0;
  font-size: 18px;
}
.pager { display: flex; justify-content: center; margin-top: 12px; }
</style>


