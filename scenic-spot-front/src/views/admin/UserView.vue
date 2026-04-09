<script setup>
import { computed, reactive, ref } from 'vue';
import { request } from '../../api/http';
import { ElMessage, ElMessageBox } from 'element-plus';

const visible = ref(false);
const isEdit = ref(false);
const state = reactive({
  list: [],
  form: { id: 0, nickname: '', phone: '', role: 'STAFF', status: 1, password: '' },
  passwordVisible: false
});
const pager = reactive({ page: 1, pageSize: 10 });
const pagedList = computed(() => {
  const start = (pager.page - 1) * pager.pageSize;
  return state.list.slice(start, start + pager.pageSize);
});

async function load() {
  try {
    state.list = await request('/api/admin/users/list');
    pager.page = 1;
    ElMessage.success('鐢ㄦ埛鍒楄〃鍔犺浇鎴愬姛');
  } catch (e) {
    ElMessage.error('鍔犺浇澶辫触');
  }
}

function openCreate() {
  isEdit.value = false;
  state.form = { id: 0, nickname: '', phone: '', role: 'STAFF', status: 1, password: '' };
  state.passwordVisible = false;
  visible.value = true;
}

function openEdit(row) {
  isEdit.value = true;
  state.form = { id: row.id, nickname: row.nickname, phone: row.phone, role: row.role, status: row.status, password: '' };
  state.passwordVisible = false;
  visible.value = true;
}

async function save() {
  if (!state.form.nickname || !state.form.phone) {
    ElMessage.warning('濮撳悕鍜岀數璇濆繀濉');
    return;
  }
  if (!isEdit.value && !state.form.password) {
    ElMessage.warning('鏂板鐢ㄦ埛瀵嗙爜蹇呭～');
    return;
  }
  try {
    if (isEdit.value) {
      await request('/api/admin/users/update', { method: 'PUT', body: JSON.stringify(state.form) });
      ElMessage.success('鏇存柊鎴愬姛');
    } else {
      await request('/api/admin/users/create-staff', { method: 'POST', body: JSON.stringify(state.form) });
      ElMessage.success('鍒涘缓鎴愬姛');
    }
    visible.value = false;
    load();
  } catch (e) {
    ElMessage.error('淇濆瓨澶辫触:' + e);
  }
}

async function deleteUser(id) {
  ElMessageBox.confirm('纭鍒犻櫎姝ょ敤鎴?', '璀﹀憡', { type: 'warning' })
    .then(async () => {
      try {
        await request(`/api/admin/users/delete/${id}`, { method: 'DELETE' });
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
    await request(`/api/admin/users/status/${id}`, { method: 'PUT' });
    ElMessage.success('鐘舵€佹洿鏂版垚鍔');
    load();
  } catch (e) {
    ElMessage.error('鐘舵€佹洿鏂板け璐');
  }
}

async function resetPassword(id) {
  ElMessageBox.confirm('纭閲嶇疆姝ょ敤鎴峰瘑鐮佷负 123456?', '鎻愮ず', { type: 'info' })
    .then(async () => {
      try {
        await request(`/api/admin/users/reset-password/${id}`, { method: 'PUT' });
        ElMessage.success('瀵嗙爜宸查噸缃负 123456');
        load();
      } catch (e) {
        ElMessage.error('閲嶇疆澶辫触');
      }
    })
    .catch(() => {});
}

load();
</script>

<template>
  <div class="card">
    <div class="header">
      <h3>鐢ㄦ埛绠＄悊</h3>
      <el-button @click="openCreate" type="primary">+ 鍒涘缓鍛樺伐</el-button>
    </div>

    <el-table :data="pagedList" stripe border>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="nickname" label="濮撳悕" width="100" />
      <el-table-column prop="phone" label="鐢佃瘽" width="120" />
      <el-table-column prop="role" label="瑙掕壊" width="80">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'">{{ row.role }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '鍚敤' : '绂佺敤' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="鎿嶄綔" width="240" fixed="right">
        <template #default="{ row }">
          <el-button @click="openEdit(row)" link type="primary" size="small">缂栬緫</el-button>
          <el-button @click="resetPassword(row.id)" link type="warning" size="small">閲嶇疆瀵嗙爜</el-button>
          <el-button @click="toggleStatus(row.id)" link type="warning" size="small">{{ row.status === 1 ? '绂佺敤' : '鍚敤' }}</el-button>
          <el-button @click="deleteUser(row.id)" link type="danger" size="small">鍒犻櫎</el-button>
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

    <el-dialog v-model="visible" :title="isEdit ? '缂栬緫鐢ㄦ埛' : '鍒涘缓鍛樺伐'" width="500px">
      <el-form label-width="100px">
        <el-form-item label="濮撳悕">
          <el-input v-model="state.form.nickname" placeholder="璇疯緭鍏ュ鍚" />
        </el-form-item>
        <el-form-item label="鐢佃瘽">
          <el-input v-model="state.form.phone" placeholder="璇疯緭鍏ョ數璇" />
        </el-form-item>
        <el-form-item label="瑙掕壊">
          <el-select v-model="state.form.role">
            <el-option label="鍛樺伐" value="STAFF" />
            <el-option label="绠＄悊鍛" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!isEdit" label="瀵嗙爜">
          <el-input v-model="state.form.password" type="password" placeholder="璇疯緭鍏ュ垵濮嬪瘑鐮" show-password />
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


