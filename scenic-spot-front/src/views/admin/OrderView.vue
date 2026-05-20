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
  statusOptions: ['UNPAID', 'PAID', 'USED', 'REFUNDING', 'REFUNDED', 'RESCHEDULING', 'RESCHEDULED', 'EXPIRED', 'DELETED']
});

const pagedList = computed(() => {
  const start = (state.page - 1) * state.pageSize;
  return state.list.slice(start, start + state.pageSize);
});

const ORDER_STATUS_TEXT = {
  UNPAID: '待支付',
  PAID: '已支付',
  USED: '已核销',
  REFUNDING: '退款中',
  REFUNDED: '已退款',
  RESCHEDULING: '改签中',
  RESCHEDULED: '已改签',
  EXPIRED: '已超时',
  DELETED: '已删除'
};

function statusText(status) {
  return ORDER_STATUS_TEXT[status] || status;
}

function formatCent(value) {
  if (value == null) return '-';
  return (value / 100).toFixed(2);
}

async function load() {
  state.loading = true;
  try {
    const params = new URLSearchParams();
    if (state.keyword.trim()) {
      params.set('userPhone', state.keyword.trim());
    }
    if (state.status) {
      params.set('status', state.status);
    }
    const query = params.toString();
    state.list = await request(`/api/admin/orders${query ? `?${query}` : ''}`);
    state.page = 1;
  } catch (e) {
    ElMessage.error(e.message || '加载订单失败');
  } finally {
    state.loading = false;
  }
}

async function removeOrder(orderNo) {
  try {
    await ElMessageBox.confirm(`确认删除订单 ${orderNo} 吗？`, '删除订单', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await request(`/api/orders/${orderNo}`, { method: 'DELETE' });
    ElMessage.success('订单已删除');
    await load();
  } catch (e) {
    if (e === 'cancel' || e === 'close') return;
    ElMessage.error(e.message || '删除订单失败');
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
          <h3>订单管理</h3>
          <p>查看全量订单、金额与状态。</p>
        </div>
        <el-button :loading="state.loading" @click="load">刷新</el-button>
      </div>
    </template>

    <div class="search-row">
      <el-input v-model="state.keyword" placeholder="手机号模糊查询" clearable @keyup.enter="load" />
      <el-select v-model="state.status" placeholder="订单状态" clearable>
        <el-option v-for="s in state.statusOptions" :key="s" :label="statusText(s)" :value="s" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <el-table :data="pagedList" border stripe v-loading="state.loading" class="data-table" style="margin-top: 10px;">
      <el-table-column prop="orderNo" label="订单号" min-width="210" />
      <el-table-column prop="userFullName" label="姓名" width="110" />
      <el-table-column prop="userIdCardNo" label="身份证号" min-width="180" />
      <el-table-column prop="userPhone" label="手机号" width="140" />
      <el-table-column prop="scenicId" label="景区ID" width="90" />
      <el-table-column prop="visitDate" label="出行日期" width="120" />
      <el-table-column prop="timeslotId" label="时段ID" width="90" />
      <el-table-column label="金额(元)" width="110">
        <template #default="scope">{{ formatCent(scope.row.totalAmountCent) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'PAID' ? 'success' : scope.row.status === 'UNPAID' ? 'warning' : 'info'">
            {{ statusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="scope">
          <el-button type="danger" plain size="small" @click="removeOrder(scope.row.orderNo)">删除</el-button>
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
.el-card {
  border-radius: 16px;
}
.head { display: flex; align-items: flex-start; justify-content: space-between; }
.head h3 { margin: 0; font-size: 22px; font-weight: 700; color: #0f172a; }
.head p { margin: 8px 0 0; color: #64748b; font-size: 15px; }
.search-row { display: grid; grid-template-columns: 1fr 180px auto auto; gap: 8px; }
.data-table {
  margin-top: 10px;
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
.data-table :deep(.cell) {
  line-height: 1.5;
}
.data-table :deep(.el-table__body tr:hover > td) {
  background: #f7faff;
}
.data-table :deep(.el-tag) {
  border-radius: 999px;
  padding: 0 10px;
  height: 26px;
  line-height: 24px;
  font-size: 13px;
}
.pager { display: flex; justify-content: center; margin-top: 12px; }

@media (max-width: 900px) {
  .search-row { grid-template-columns: 1fr; }
}
</style>

