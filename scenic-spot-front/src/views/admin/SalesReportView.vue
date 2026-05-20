<script setup>
import { onMounted, reactive } from 'vue';
import VChart from 'vue-echarts';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { PieChart } from 'echarts/charts';
import { TooltipComponent, TitleComponent, LegendComponent } from 'echarts/components';
import { download, request } from '../../api/http';
import { ElMessage } from 'element-plus';

use([CanvasRenderer, PieChart, TooltipComponent, TitleComponent, LegendComponent]);

const now = new Date();
const monthStart = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0);
const form = reactive({ scenicId: 1, range: [monthStart, now] });
const state = reactive({ loading: false, data: null, chart: {} });

function pad(num) {
  return String(num).padStart(2, '0');
}

function formatDateTime(value) {
  const date = value instanceof Date ? value : new Date(value);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function disabledFutureDate(date) {
  return date.getTime() > Date.now();
}

function getQueryRange() {
  if (!form.range || form.range.length !== 2 || !form.range[0] || !form.range[1]) {
    throw new Error('请选择开始和结束时间');
  }
  const startDate = new Date(form.range[0]);
  const endDate = new Date(form.range[1]);
  if (startDate > endDate) {
    throw new Error('开始时间不能晚于结束时间');
  }
  if (endDate > Date.now()) {
    throw new Error('结束时间不能超过当前时间');
  }
  return {
    start: formatDateTime(startDate),
    end: formatDateTime(endDate)
  };
}

async function load() {
  state.loading = true;
  try {
    const range = getQueryRange();
    state.data = await request(`/api/analyst/report/sales?scenicId=${form.scenicId}&start=${encodeURIComponent(range.start)}&end=${encodeURIComponent(range.end)}`);
    if (state.data?.byTicket?.length) {
      const labels = state.data.byTicket.map(d => d.ticketName || '未命名门票');
      const values = state.data.byTicket.map(d => d.qty || 0);
      state.chart = {
        color: ['#409eff', '#f59e0b', '#10b981', '#ef4444', '#8b5cf6', '#14b8a6'],
        title: { text: '销量分布', left: 'center', textStyle: { fontSize: 14 } },
        tooltip: { trigger: 'item', formatter: '{b}: {c}张 ({d}%)' },
        legend: { bottom: 0 },
        series: [{
          name: '销量',
          type: 'pie',
          radius: ['35%', '60%'],
          center: ['50%', '55%'],
          avoidLabelOverlap: true,
          label: { formatter: '{b}\n{d}%' },
          data: labels.map((l, i) => ({ name: l, value: values[i] }))
        }]
      };
    } else {
      state.chart = {};
    }
  } catch (e) {
    ElMessage.error(e.message || '加载销量报表失败');
  } finally {
    state.loading = false;
  }
}

async function exportCsv() {
  try {
    const range = getQueryRange();
    const blob = await download(`/api/analyst/report/sales/export?scenicId=${form.scenicId}&start=${encodeURIComponent(range.start)}&end=${encodeURIComponent(range.end)}`);
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'sales_report.csv';
    a.click();
    URL.revokeObjectURL(a.href);
    ElMessage.success('导出成功');
  } catch (e) {
    ElMessage.error(e.message || '导出失败');
  }
}

onMounted(load);
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="head">
        <div>
          <h3>销量报表</h3>
          <p>按景区和时间范围查看门票销量分布。</p>
        </div>
      </div>
    </template>

    <div class="toolbar">
      <el-date-picker
        v-model="form.range"
        type="datetimerange"
        range-separator="至"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        format="YYYY-MM-DD HH:mm:ss"
        :disabled-date="disabledFutureDate"
      />
      <el-button type="primary" :loading="state.loading" @click="load">查询</el-button>
      <el-button type="success" plain @click="exportCsv">导出CSV</el-button>
    </div>

    <div v-if="state.data">
      <div v-if="state.chart?.series" class="chart-container">
        <VChart class="chart" :option="state.chart" autoresize />
      </div>
      <el-empty v-else description="当前时段暂无销量数据" />

      <el-table
        v-if="state.data?.byTicket?.length"
        :data="state.data.byTicket"
        border stripe class="data-table" size="small"
      >
        <el-table-column prop="ticketName" label="门票名称" min-width="160" />
        <el-table-column prop="qty" label="销量(张)" width="120" align="right" />
        <el-table-column prop="amountCent" label="金额(分)" width="120" align="right" />
      </el-table>
    </div>
  </el-card>
</template>

<style scoped>
.el-card { border-radius: 16px; }
.head { display: flex; align-items: flex-start; justify-content: space-between; }
.head h3 { margin: 0; font-size: 22px; font-weight: 700; color: #0f172a; }
.head p { margin: 8px 0 0; color: #64748b; font-size: 15px; }

.toolbar { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 12px; align-items: center; }

.chart-container { height: 400px; margin-bottom: 12px; }
.chart { width: 100%; height: 100%; }

.data-table {
  border-radius: 14px;
  overflow: hidden;
  font-size: 15px;
  margin-top: 8px;
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

@media (max-width: 768px) {
  .toolbar { flex-direction: column; align-items: flex-start; }
  .chart-container { height: 300px; }
}
</style>
