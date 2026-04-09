<script setup>
import { computed, onMounted, reactive } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { request } from '../../api/http';

const state = reactive({
  list: [],
  page: 1,
  pageSize: 10,
  loading: false,
  keyword: '',
  status: '',
  statusOptions: ['SUBMITTED', 'DONE', 'REJECTED']
});

const pagedList = computed(() => {
  const start = (state.page - 1) * state.pageSize;
  return state.list.slice(start, start + state.pageSize);
});

const statusMap = {
  SUBMITTED: '处理中',
  DONE: '已完成',
  REJECTED: '已拒绝'
};

const typeMap = {
  REFUND: '退款',
  RESCHEDULE: '改签'
};

function statusText(status) {
  return statusMap[status] || status;
}

function typeText(type) {
  return typeMap[type] || type;
}

async function load() {
  state.loading = true;
  try {
    const params = new URLSearchParams();
    if (state.keyword.trim()) params.set('userPhone', state.keyword.trim());
    if (state.status) params.set('status', state.status);
    const query = params.toString();
    state.list = await request(`/api/auditor/aftersale${query ? `?${query}` : ''}`);
    state.page = 1;
  } catch (e) {
    ElMessage.error(e.message || '加载售后失败');
  } finally {
    state.loading = false;
  }
}

async function approve(reqNo) {
  try {
    await request(`/api/auditor/aftersale/${reqNo}/approve`, {
      method: 'POST',
      body: { auditComment: '通过' }
    });
    ElMessage.success('审核通过');
    await load();
  } catch (e) {
    ElMessage.error(e.message || '操作失败');
  }
}

async function reject(reqNo) {
  try {
    await request(`/api/auditor/aftersale/${reqNo}/reject`, {
      method: 'POST',
      body: { auditComment: '拒绝' }
    });
    ElMessage.success('已拒绝申请');
    await load();
  } catch (e) {
    ElMessage.error(e.message || '操作失败');
  }
}

async function editReq(item) {
  try {
    const { value } = await ElMessageBox.prompt('请输入新的售后原因', '编辑售后单', {
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      inputValue: item.reason || '',
      inputPlaceholder: '售后原因'
    });
    await request(`/api/auditor/aftersale/${item.reqNo}`, {
      method: 'PUT',
      body: {
        reason: value,
        targetVisitDate: item.targetVisitDate || null,
        targetTimeslotId: item.targetTimeslotId || null
      }
    });
    ElMessage.success('售后单已更新');
    await load();
  } catch (e) {
    if (e === 'cancel' || e === 'close') return;
    ElMessage.error(e.message || '更新失败');
  }
}

async function removeReq(reqNo) {
  try {
    await ElMessageBox.confirm(`确认删除售后单 ${reqNo} 吗？`, '删除售后单', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消'
    });
    await request(`/api/auditor/aftersale/${reqNo}`, { method: 'DELETE' });
    ElMessage.success('售后单已删除');
    await load();
  } catch (e) {
    if (e === 'cancel' || e === 'close') return;
    ElMessage.error(e.message || '删除失败');
  }
}

function resetSearch() {
  state.keyword = '';
  state.status = '';
  load();
}

onMounted(load);
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="head">
        <div>
          <h3>售后审核</h3>
          <p>审核退票、改签等售后请求。</p>
        </div>
        <el-button :loading="state.loading" @click="load">刷新</el-button>
      </div>
    </template>

    <div class="search-row">
      <el-input v-model="state.keyword" placeholder="手机号模糊查询" clearable @keyup.enter="load" />
      <el-select v-model="state.status" placeholder="售后状态" clearable>
        <el-option v-for="s in state.statusOptions" :key="s" :label="statusText(s)" :value="s" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <el-table :data="pagedList" border stripe v-loading="state.loading">
      <el-table-column prop="reqNo" label="申请单" min-width="190" />
      <el-table-column prop="orderNo" label="订单号" min-width="190" />
      <el-table-column prop="userNickname" label="用户" width="120" />
      <el-table-column prop="userPhone" label="手机号" width="140" />
      <el-table-column label="类型" width="100">
        <template #default="scope">{{ typeText(scope.row.reqType) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'DONE' ? 'success' : scope.row.status === 'REJECTED' ? 'danger' : 'warning'">
            {{ statusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="scope">
          <div v-if="scope.row.status === 'SUBMITTED'" class="actions">
            <el-button type="success" size="small" @click="approve(scope.row.reqNo)">通过</el-button>
            <el-button type="danger" plain size="small" @click="reject(scope.row.reqNo)">拒绝</el-button>
            <el-button size="small" @click="editReq(scope.row)">编辑</el-button>
            <el-button type="danger" plain size="small" @click="removeReq(scope.row.reqNo)">删除</el-button>
          </div>
          <div v-else class="done-wrap">
            <el-tag type="success">已完成</el-tag>
          </div>
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
  </el-card>
</template>

<style scoped>
.head { display: flex; align-items: flex-start; justify-content: space-between; }
.head h3 { margin: 0; }
.head p { margin: 8px 0 0; color: #64748b; font-size: 13px; }
.search-row { display: grid; grid-template-columns: 1fr 180px auto auto; gap: 8px; margin-bottom: 12px; }
.actions { display: flex; gap: 8px; }
.done-wrap { display: flex; justify-content: center; }
.pager { display: flex; justify-content: center; margin-top: 12px; }

@media (max-width: 900px) {
  .search-row { grid-template-columns: 1fr; }
  .actions { flex-wrap: wrap; }
}
</style>

