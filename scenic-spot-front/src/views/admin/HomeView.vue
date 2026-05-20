<script setup>
import { onMounted, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import { request } from '../../api/http';

const state = reactive({
  loading: false,
  stats: null,
  latestOrders: [],
  pendingAftersales: []
});

async function load() {
  state.loading = true;
  try {
    const data = await request('/api/home/console');
    state.stats = data.stats || null;
    state.latestOrders = data.latestOrders || [];
    state.pendingAftersales = data.pendingAftersales || [];
  } catch (e) {
    ElMessage.error(e.message || '加载首页数据失败');
  } finally {
    state.loading = false;
  }
}

function orderStatusText(s) {
  const map = {
    UNPAID: '待支付',
    PAID: '已支付',
    USED: '已使用',
    REFUNDING: '退款中',
    REFUNDED: '已退款',
    RESCHEDULING: '改签中',
    RESCHEDULED: '已改签',
    DELETED: '已删除',
    EXPIRED: '已超时',
    CANCELLED: '已取消',
    CLOSED: '已关闭'
  };
  return map[s] || s;
}

function orderStatusType(s) {
  if (s === 'PAID' || s === 'USED') return 'success';
  if (s === 'REFUNDING' || s === 'UNPAID' || s === 'RESCHEDULING') return 'warning';
  return 'info';
}

function aftersaleTypeText(t) {
  const map = { REFUND: '退款', RESCHEDULE: '改签', CANCEL: '取消' };
  return map[t] || t || '-';
}

function aftersaleStatusText(s) {
  const map = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝' };
  return map[s] || s;
}

function formatCent(v) {
  if (v == null) return '-';
  return (v / 100).toFixed(2);
}

function formatTime(v) {
  if (!v) return '-';
  return v.replace('T', ' ').substring(0, 19);
}

onMounted(load);
</script>

<template>
  <div class="home">
    <div class="welcome">
      <h2>控制台首页</h2>
      <p>景区实时运营概览</p>
    </div>

    <el-row v-if="state.stats" :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="景区数量" :value="state.stats.scenicCount">
            <template #prefix><span class="stat-icon">&#x1F3DE;</span></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="在售门票" :value="state.stats.activeTicketCount">
            <template #prefix><span class="stat-icon">&#x1F3AB;</span></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="今日订单" :value="state.stats.todayOrderCount">
            <template #prefix><span class="stat-icon">&#x1F4CB;</span></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="待审售后" :value="state.stats.pendingAftersaleCount">
            <template #prefix><span class="stat-icon">&#x1F514;</span></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="用户总数" :value="state.stats.totalUserCount">
            <template #prefix><span class="stat-icon">&#x1F465;</span></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="当前在园" :value="state.stats.totalInPark">
            <template #prefix><span class="stat-icon">&#x1F6B6;</span></template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="panel-row" v-loading="state.loading">
      <el-col :span="12">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-head">
              <h3>最新订单</h3>
              <el-tag type="info" size="small">{{ state.latestOrders.length }} 条</el-tag>
            </div>
          </template>
          <el-empty v-if="!state.loading && !state.latestOrders.length" description="暂无订单" />
          <el-table v-else :data="state.latestOrders" size="small" class="compact-table">
            <el-table-column prop="orderNo" label="订单号" width="170" />
            <el-table-column prop="ticketName" label="门票" min-width="100" show-overflow-tooltip />
            <el-table-column prop="userNickname" label="游客" width="80" />
            <el-table-column label="金额" width="90">
              <template #default="{ row }">{{ formatCent(row.totalAmountCent) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="orderStatusType(row.status)" size="small">{{ orderStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="下单时间" width="160">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-head">
              <h3>待处理售后</h3>
              <el-tag type="warning" size="small">{{ state.pendingAftersales.length }} 条</el-tag>
            </div>
          </template>
          <el-empty v-if="!state.loading && !state.pendingAftersales.length" description="暂无待处理售后" />
          <el-table v-else :data="state.pendingAftersales" size="small" class="compact-table">
            <el-table-column prop="reqNo" label="申请号" width="170" />
            <el-table-column label="类型" width="70">
              <template #default="{ row }">{{ aftersaleTypeText(row.reqType) }}</template>
            </el-table-column>
            <el-table-column prop="reason" label="原因" min-width="120" show-overflow-tooltip />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag type="warning" size="small">{{ aftersaleStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="申请时间" width="160">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.home { display: grid; gap: 16px; }

.welcome { margin-bottom: 4px; }
.welcome h2 { margin: 0; font-size: 22px; font-weight: 700; color: #0f172a; }
.welcome p { margin: 6px 0 0; color: #64748b; font-size: 15px; }

.stat-row { margin-bottom: 8px; }
.stat-card { text-align: center; border-radius: 12px; }
.stat-card :deep(.el-statistic__head) { font-size: 14px; color: #64748b; }
.stat-card :deep(.el-statistic__number) { font-size: 26px; font-weight: 700; color: #1e293b; }
.stat-icon { font-size: 20px; margin-right: 4px; }

.panel-row { flex: 1; }
.panel-card { border-radius: 12px; height: 100%; }
.panel-head { display: flex; align-items: center; gap: 10px; }
.panel-head h3 { margin: 0; font-size: 17px; font-weight: 600; }

.compact-table {
  font-size: 14px;
  border-radius: 10px;
  overflow: hidden;
}
.compact-table :deep(th.el-table__cell) { font-size: 13px; font-weight: 600; color: #334155; background: #f8fafc; }
.compact-table :deep(td.el-table__cell) { padding-top: 10px; padding-bottom: 10px; }
.compact-table :deep(.el-tag--small) { font-size: 12px; }
</style>
